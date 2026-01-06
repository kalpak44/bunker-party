from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
import uuid
import random
import re
import time

from app.rooms import (
    rooms,
    create_room,
    REVEAL_KEYS,
    PHASE_LOBBY,
    PHASE_REVEAL,
    PHASE_CONFIRM,
    PHASE_VOTE,
    PHASE_GAME_OVER,
)
from app.game_data import GAME_DATA

ROOM_RE = re.compile(r"^\d{4}$")
MAX_PLAYERS = 6

app = FastAPI()
app.mount("/static", StaticFiles(directory="static"), name="static")


@app.get("/")
def index():
    return FileResponse("static/index.html")


@app.post("/create/{lang}")
def create(lang: str):
    if lang not in GAME_DATA:
        lang = "en"
    return {"room_id": create_room(lang)}


def compute_unique_capacity() -> int:
    """
    Compute the maximum number of players that can join a room while
    preserving uniqueness per card key across the room.

    It's the minimum, over all reveal keys, of the count of unique card
    values available for that key across all languages.
    """
    min_unique = None
    for k in REVEAL_KEYS:
        union_vals = set()
        for gd in GAME_DATA.values():
            union_vals.update(gd["cards"].get(k, []))
        cnt = len(union_vals)
        if min_unique is None or cnt < min_unique:
            min_unique = cnt
    return min_unique or 0


def make_character_for_room(room: dict, lang: str) -> tuple[dict[str, str], dict[str, int]]:
    """
    Create a character for a player ensuring no duplicate card values per key within the room.
    Preference is given to the player's selected language list; if it's exhausted for a key,
    fall back to any available value (including duplicates as a last resort).

    Returns: (character dict with localized values, character_indices dict with card indices)
    """
    # Prefer the player's language; if not present (shouldn't happen), use room default
    if lang not in GAME_DATA:
        lang = room.get("default_lang", "en")

    cards_by_lang = GAME_DATA[lang]["cards"]

    # Collect used indices in the room per key
    used_indices_per_key: dict[str, set] = {k: set() for k in REVEAL_KEYS}
    for p in room.get("players", {}).values():
        ch_indices = p.get("character_indices", {})
        for k in REVEAL_KEYS:
            idx = ch_indices.get(k)
            if idx is not None:
                used_indices_per_key[k].add(idx)

    character: dict[str, str] = {}
    character_indices: dict[str, int] = {}

    for k in REVEAL_KEYS:
        # Get card list for this key in player's language
        pool = list(cards_by_lang.get(k, []))

        # Find available indices (not used yet)
        available_indices = [i for i in range(len(pool)) if i not in used_indices_per_key[k]]

        if available_indices:
            idx = random.choice(available_indices)
            character[k] = pool[idx]
            character_indices[k] = idx
        else:
            # No unique options left
            raise ValueError(f"No unique cards remaining for key '{k}' in room {room.get('room_id')}")

    return character, character_indices


async def safe_send(ws: WebSocket, msg: dict):
    try:
        await ws.send_json(msg)
    except Exception:
        pass


def add_log(room: dict, log_type: str, data: dict):
    """Add a structured log entry to room logs"""
    room["logs"].append({
        "type": log_type,
        "timestamp": time.time(),
        "data": data
    })


def get_localized_logs(room: dict, lang: str, limit: int = 50) -> list[str]:
    """Convert structured logs to localized text messages"""
    ui = GAME_DATA[lang]["ui"]
    labels = GAME_DATA[lang]["labels"]
    cards = GAME_DATA[lang]["cards"]

    messages = []
    for entry in room["logs"][-limit:]:
        log_type = entry["type"]
        data = entry["data"]

        if log_type == "player_joined":
            messages.append(f"➕ {data['name']}")
        elif log_type == "room_created":
            messages.append(f"{ui.get('room_code', 'Room code')}: {data['room_id']}")
        elif log_type == "round_started":
            bunker_event = GAME_DATA[lang]["bunkers"][data["event_idx"]]
            messages.append(f"🏠 {ui['round']} {data['round']}: {bunker_event}")
        elif log_type == "player_revealed":
            key = data["key"]
            card_idx = data["card_idx"]
            localized_key = labels.get(key, key)
            localized_value = cards[key][card_idx] if key in cards and card_idx < len(cards[key]) else "?"
            messages.append(f"🔓 {data['player_name']} — {localized_key}: {localized_value}")
        elif log_type == "player_reconnected":
            msg_tpl = ui.get("player_reconnected", "{name} reconnected")
            messages.append(f"🔌 {msg_tpl.replace('{name}', data['name'])}")
        elif log_type == "player_disconnected":
            msg_tpl = ui.get("player_disconnected", "{name} disconnected")
            messages.append(f"⚠️ {msg_tpl.replace('{name}', data['name'])}")
        elif log_type == "player_eliminated":
            messages.append(f"❌ {data['player_name']}")
        elif log_type == "game_over":
            winner_count = data.get("winner_count", 0)
            if winner_count == 1:
                winner = data.get("winner", "")
                msg_tpl = ui.get("congrats_winner", "Congratulations, {name}! You made it into the bunker.")
                try:
                    message = msg_tpl.format(name=winner)
                except Exception:
                    message = msg_tpl
                messages.append(f"🏁 {message}")
            elif winner_count == 2:
                winners = data.get("winners", [])
                if len(winners) == 2:
                    msg_tpl = ui.get("congrats_winners_two", "Congratulations, {name1} and {name2}! You both made it into the bunker.")
                    try:
                        message = msg_tpl.format(name1=winners[0], name2=winners[1])
                    except Exception:
                        message = msg_tpl
                    messages.append(f"🏁 {message}")
            else:
                # All cards used
                messages.append(f"🏁 {ui['game_over']}. {ui['all_cards_used']}")
        elif log_type == "vote_start":
            msg = ui.get("you_voted_start", "Voted to start")
            messages.append(f"✅ {data['player_name']}: {msg}")
        elif log_type == "confirm_round":
            msg = ui.get("you_confirmed", "Confirmed round end")
            messages.append(f"✅ {data['player_name']}: {msg}")

    return messages


async def broadcast(room: dict, msg: dict):
    for p in list(room["players"].values()):
        await safe_send(p["ws"], msg)


async def broadcast_refresh(room: dict):
    """Notify all clients to refresh their data from backend"""
    await broadcast(room, {"type": "refresh"})


def all_used_all_cards(room: dict) -> bool:
    if not room["players"]:
        return False
    return all(len(p["used_keys"]) >= len(REVEAL_KEYS) for p in room["players"].values())


def active_pids(room: dict) -> list[str]:
    eliminated = room.get("eliminated", set())
    return [pid for pid in room["players"].keys() if pid not in eliminated]


def name_by_pid(room: dict, pid: str) -> str:
    p = room["players"].get(pid)
    return p["name"] if p else ""




async def send_event_localized(room: dict):
    # Log round start
    add_log(room, "round_started", {"round": room["round"], "event_idx": room["event_idx"]})
    # Notify clients to refresh (they'll fetch localized bunker event)
    await broadcast_refresh(room)


async def start_next_round(room: dict):
    # Game might have already ended via eliminations
    act = active_pids(room)
    if len(act) <= 2:
        await declare_winner_if_any(room)
        return

    if all_used_all_cards(room):
        room["phase"] = PHASE_GAME_OVER
        # Log game over
        add_log(room, "game_over", {"message": "All cards used"})
        await broadcast_refresh(room)
        return

    room["phase"] = PHASE_REVEAL
    room["round"] += 1
    room["round_reveals"] = {}
    room["round_confirms"] = set()
    room["round_votes"] = {}
    # clear any revote context from previous round
    room.pop("revote_targets", None)
    room.pop("revote_quota", None)

    n = len(GAME_DATA[room["default_lang"]]["bunkers"])
    room["event_idx"] = random.randrange(n)

    await send_event_localized(room)


def compute_elimination_plan(total_players: int) -> dict[int, int]:
    """
    Plan eliminations so that two players remain at the end.
    Distribute (total_players - 2) eliminations across rounds 3..7 as evenly as possible,
    prioritizing later rounds (shift voting towards the end for small lobbies).
    """
    total_elims = max(0, total_players - 2)
    rounds = [3, 4, 5, 6, 7]
    base = total_elims // len(rounds)
    rem = total_elims % len(rounds)
    plan: dict[int, int] = {}
    # Fill base amount first
    for r in rounds:
        plan[r] = base
    # Distribute remainder to later rounds first (shift voting to the end)
    for r in reversed(rounds):
        if rem <= 0:
            break
        plan[r] += 1
        rem -= 1
    return plan


def current_round_quota(room: dict) -> int:
    plan = room.get("elim_plan") or {}
    return int(plan.get(room.get("round", 0), 0))


async def declare_winner_if_any(room: dict):
    act = active_pids(room)
    if len(act) == 1:
        winner_pid = act[0]
        winner_name = name_by_pid(room, winner_pid)
        room["phase"] = PHASE_GAME_OVER
        add_log(room, "game_over", {"winner": winner_name, "winner_count": 1})
        await broadcast_refresh(room)
        return True
    if len(act) == 2:
        n1 = name_by_pid(room, act[0])
        n2 = name_by_pid(room, act[1])
        room["phase"] = PHASE_GAME_OVER
        add_log(room, "game_over", {"winners": [n1, n2], "winner_count": 2})
        await broadcast_refresh(room)
        return True
    return False


@app.websocket("/ws/{room_id}")
async def ws_room(ws: WebSocket, room_id: str):
    room_id = (room_id or "").strip()

    await ws.accept()

    # format validation early
    if not ROOM_RE.match(room_id):
        # read join payload to know language for localized error if possible
        try:
            join_peek = await ws.receive_json()
            lang = (join_peek.get("lang") or "en").strip()
        except Exception:
            lang = "en"
        if lang not in GAME_DATA:
            lang = "en"
        await safe_send(ws, {"type": "error", "code": "invalid_room_format", "message": GAME_DATA[lang]["ui"]["invalid_room_format"]})
        await ws.close(code=1008)
        return

    room_id = room_id.upper()

    # room existence
    if room_id not in rooms:
        # read join payload to know language for localized error if possible
        try:
            join_peek = await ws.receive_json()
            lang = (join_peek.get("lang") or "en").strip()
        except Exception:
            lang = "en"
        if lang not in GAME_DATA:
            lang = "en"
        await safe_send(ws, {"type": "error", "code": "invalid_room", "message": GAME_DATA[lang]["ui"]["invalid_room"]})
        await ws.close(code=1008)
        return

    room = rooms[room_id]
    pid = None
    is_reconnect = False

    try:
        join = await ws.receive_json()
        name = (join.get("name") or "").strip()
        lang = (join.get("lang") or "").strip()

        if lang not in GAME_DATA:
            lang = room["default_lang"]
        ui = GAME_DATA[lang]["ui"]

        if not name:
            await safe_send(ws, {"type": "error", "code": "name_required", "message": ui["name_required"]})
            await ws.close(code=1008)
            return

        # Check for reconnection - allow in any phase
        name_cf = name.casefold()
        existing_pid = room.get("pid_by_name", {}).get(name_cf)

        if existing_pid and existing_pid in room["players"]:
            # Player with same name exists - this is a reconnection
            pid = existing_pid
            is_reconnect = True
            # Update language preference if changed
            room["players"][pid]["lang"] = lang
        else:
            # New player trying to join
            if room["phase"] != PHASE_LOBBY:
                # Game already started, no matching player - reject
                await safe_send(ws, {"type": "error", "code": "game_started", "message": ui["game_started"]})
                await ws.close(code=1008)
                return

            # In lobby, check for name uniqueness
            existing = {p["name"].casefold() for p in room["players"].values()}
            if name.casefold() in existing:
                await safe_send(ws, {"type": "error", "code": "name_taken", "message": ui["name_taken"]})
                await ws.close(code=1008)
                return

        # Handle reconnection vs new player
        if is_reconnect:
            # Update websocket for reconnecting player
            room["players"][pid]["ws"] = ws
            room["players"][pid]["online"] = True
            room["players"][pid]["last_seen"] = time.time()
            character = room["players"][pid]["character"]

            # Log reconnection
            add_log(room, "player_reconnected", {"name": name})
        else:
            # New player joining
            # capacity guard: refuse join if players exceed unique cards availability or max player limit
            capacity = min(compute_unique_capacity(), MAX_PLAYERS)
            if len(room["players"]) >= capacity:
                message_tpl = ui.get("too_many_players") or ui.get("error", "Error")
                try:
                    message = message_tpl.format(n=capacity)
                except Exception:
                    message = message_tpl
                await safe_send(ws, {
                    "type": "error",
                    "code": "too_many_players",
                    "message": message,
                    "max_players": capacity,
                })
                await ws.close(code=1013)
                return

            try:
                character, character_indices = make_character_for_room(room, lang)
            except ValueError:
                await safe_send(ws, {"type": "error", "code": "no_unique_cards", "message": ui.get("error", "Error")})
                await ws.close(code=1013)
                return

            # Create new player
            pid = uuid.uuid4().hex
            room["players"][pid] = {
                "name": name,
                "lang": lang,
                "ws": ws,
                "character": character,
                "character_indices": character_indices,
                "revealed": {},
                "revealed_indices": {},
                "used_keys": set(),
                "online": True,
                "last_seen": time.time(),
            }
            # Track name -> pid mapping for reconnection
            room["pid_by_name"][name.casefold()] = pid
            # Log player join
            add_log(room, "player_joined", {"name": name})

        await safe_send(ws, {
            "type": "init",
            "room_id": room_id,
            "player_name": name,
            "ui": ui,
            "labels": GAME_DATA[lang]["labels"],
            "character": character,
        })

        # Notify all clients to refresh
        await broadcast_refresh(room)

        while True:
            msg = await ws.receive_json()
            t = msg.get("type")

            # STATE QUERY HANDLERS
            if t == "get_ui":
                # Return localized UI strings
                p = room["players"].get(pid)
                if p:
                    lang_data = GAME_DATA[p["lang"]]
                    ui_with_bunkers = {**lang_data["ui"], "bunkers": lang_data["bunkers"]}
                    await safe_send(ws, {
                        "type": "ui_data",
                        "ui": ui_with_bunkers,
                        "labels": lang_data["labels"],
                        "requestId": msg.get("requestId")
                    })

            elif t == "get_character":
                # Return player's character cards (localized)
                p = room["players"].get(pid)
                if p:
                    await safe_send(ws, {
                        "type": "character_data",
                        "character": p["character"],
                        "requestId": msg.get("requestId")
                    })

            elif t == "get_logs":
                # Return localized logs
                p = room["players"].get(pid)
                if p:
                    logs = get_localized_logs(room, p["lang"])
                    await safe_send(ws, {
                        "type": "logs_data",
                        "logs": logs,
                        "requestId": msg.get("requestId")
                    })

            elif t == "get_players":
                # Return player list with online status
                player_list = []
                for player in room["players"].values():
                    player_list.append({
                        "name": player["name"],
                        "online": player.get("online", True)
                    })
                await safe_send(ws, {
                    "type": "players_data",
                    "players": player_list,
                    "requestId": msg.get("requestId")
                })

            elif t == "get_revealed":
                # Return all revealed cards (localized for requesting player)
                p = room["players"].get(pid)
                if p:
                    recipient_lang = p["lang"]
                    localized_labels = GAME_DATA[recipient_lang]["labels"]
                    localized_cards = GAME_DATA[recipient_lang]["cards"]
                    revealed_localized = {}
                    for pl in room["players"].values():
                        player_name = pl["name"]
                        revealed_indices = pl.get("revealed_indices", {})
                        revealed_localized[player_name] = {}
                        for key, card_idx in revealed_indices.items():
                            localized_key = localized_labels.get(key, key)
                            if key in localized_cards and card_idx < len(localized_cards[key]):
                                localized_value = localized_cards[key][card_idx]
                            else:
                                localized_value = pl.get("revealed", {}).get(key, "")
                            revealed_localized[player_name][localized_key] = localized_value
                    await safe_send(ws, {
                        "type": "revealed_data",
                        "revealed": revealed_localized,
                        "requestId": msg.get("requestId")
                    })

            elif t == "get_game_state":
                # Return full game state (localized)
                p = room["players"].get(pid)
                if p:
                    act_pids = active_pids(room)
                    reveals_done = len([pid for pid in room.get("round_reveals", {}).keys() if pid in act_pids])
                    confirms_done = len([pid for pid in room.get("round_confirms", set()) if pid in act_pids])
                    votes_done = len([pid for pid in room.get("round_votes", {}).keys() if pid in act_pids])
                    players_total = len(act_pids)

                    player_action_done = False
                    if room["phase"] == PHASE_LOBBY:
                        player_action_done = pid in room["start_votes"]
                    elif room["phase"] == PHASE_REVEAL:
                        player_action_done = pid in room.get("round_reveals", {})
                    elif room["phase"] == PHASE_CONFIRM:
                        player_action_done = pid in room.get("round_confirms", set())
                    elif room["phase"] == PHASE_VOTE:
                        player_action_done = pid in room.get("round_votes", {})

                    await safe_send(ws, {
                        "type": "game_state_data",
                        "phase": room["phase"],
                        "round": room["round"],
                        "event_idx": room["event_idx"],
                        "players_total": players_total,
                        "capacity": min(compute_unique_capacity(), MAX_PLAYERS),
                        "start_votes": len(room["start_votes"]),
                        "reveals_done": reveals_done,
                        "confirms_done": confirms_done,
                        "votes_done": votes_done,
                        "vote_quota": current_round_quota(room),
                        "revote_targets": [name_by_pid(room, pid) for pid in (room.get("revote_targets") or [])],
                        "revote_quota": int(room.get("revote_quota") or 0),
                        "eliminated_names": [name_by_pid(room, pid) for pid in room.get("eliminated", set())],
                        "player_action_done": player_action_done,
                        "requestId": msg.get("requestId")
                    })

            # VOTE START
            elif t == "vote_start" and room["phase"] == PHASE_LOBBY:
                room["start_votes"].add(pid)
                p = room["players"].get(pid)
                if p:
                    add_log(room, "vote_start", {"player_name": p["name"]})
                await broadcast_refresh(room)

                # Require unanimous votes AND a minimum of 3 players to start
                if (
                    len(room["start_votes"]) == len(room["players"]) and
                    len(room["players"]) >= 3
                ):
                    room["round"] = 0
                    # Initialize elimination plan based on initial number of players
                    room["elim_plan"] = compute_elimination_plan(len(room["players"]))
                    await start_next_round(room)

            # REVEAL
            elif t == "reveal_card" and room["phase"] == PHASE_REVEAL:
                key = (msg.get("key") or "").strip()
                if key not in REVEAL_KEYS:
                    continue

                p = room["players"].get(pid)
                if not p:
                    continue

                # eliminated cannot act
                if pid in room.get("eliminated", set()):
                    continue

                # prevent repeating the same card across game
                if key in p["used_keys"]:
                    continue

                # one per round
                if pid in room["round_reveals"]:
                    continue

                p["used_keys"].add(key)
                p["revealed"][key] = p["character"][key]
                p["revealed_indices"][key] = p["character_indices"][key]
                room["round_reveals"][pid] = key

                # Log reveal
                card_idx = p["character_indices"][key]
                add_log(room, "player_revealed", {
                    "player_name": p["name"],
                    "key": key,
                    "card_idx": card_idx
                })

                # if all active players revealed, go confirm
                if len([x for x in room["round_reveals"].keys() if x in active_pids(room)]) == len(active_pids(room)) and len(active_pids(room)) > 0:
                    room["phase"] = PHASE_CONFIRM

                await broadcast_refresh(room)

            # CONFIRM ROUND END
            elif t == "confirm_round_end" and room["phase"] == PHASE_CONFIRM:
                # eliminated cannot act
                if pid in room.get("eliminated", set()):
                    continue
                room["round_confirms"].add(pid)
                p = room["players"].get(pid)
                if p:
                    add_log(room, "confirm_round", {"player_name": p["name"]})
                await broadcast_refresh(room)

                # If all active confirmed, either start vote (from round 3) or next round
                if len([x for x in room["round_confirms"] if x in active_pids(room)]) == len(active_pids(room)) and len(active_pids(room)) > 0:
                    if room["round"] >= 3 and current_round_quota(room) > 0 and len(active_pids(room)) > 2:
                        room["phase"] = PHASE_VOTE
                        room["round_votes"] = {}
                        await broadcast_refresh(room)
                    else:
                        await start_next_round(room)

            # VOTE TO ELIMINATE
            elif t == "vote_eliminate" and room.get("phase") == PHASE_VOTE:
                # eliminated cannot act
                if pid in room.get("eliminated", set()):
                    continue
                target_name = (msg.get("target") or "").strip()
                # map name to pid among active players, case-insensitive
                target_pid = None
                target_name_cf = target_name.casefold()
                # if in revote, only consider revote targets
                revote_targets: set | None = room.get("revote_targets")
                candidates = list(active_pids(room))
                if revote_targets:
                    candidates = [ap for ap in candidates if ap in revote_targets]
                for apid in candidates:
                    if room["players"][apid]["name"].casefold() == target_name_cf:
                        target_pid = apid
                        break
                if not target_pid:
                    continue
                # cannot vote if already voted
                if pid in room.get("round_votes", {}):
                    continue
                # cannot vote for self
                if target_pid == pid:
                    continue
                room["round_votes"][pid] = target_pid
                await broadcast_refresh(room)

                # If all active voted, tally
                if len([x for x in room["round_votes"].keys() if x in active_pids(room)]) == len(active_pids(room)):
                    # tally votes for targets among active
                    tally: dict[str, int] = {}
                    for voter, tgt in room["round_votes"].items():
                        if voter in active_pids(room) and tgt in active_pids(room):
                            tally[tgt] = tally.get(tgt, 0) + 1

                    # Determine elimination quota taking into account active players
                    quota = min(current_round_quota(room), max(0, len(active_pids(room)) - 2))

                    # If no one should be eliminated, just proceed to next round
                    if quota <= 0:
                        await start_next_round(room)
                        return

                    # Revote context (if exists, limit candidates and use stored quota)
                    revote_targets = room.get("revote_targets")
                    revote_quota = room.get("revote_quota")
                    if revote_targets:
                        candidates = [ap for ap in active_pids(room) if ap in revote_targets]
                        quota = int(revote_quota or quota)
                    else:
                        candidates = list(active_pids(room))

                    # If nobody received any votes, avoid unfair deterministic elimination
                    max_votes = 0
                    if candidates:
                        max_votes = max((tally.get(ap, 0) for ap in candidates), default=0)
                    if max_votes == 0:
                        # No signal from votes — skip eliminations this round
                        await start_next_round(room)
                        return

                    # Compute top group by highest votes among considered candidates
                    top_group = [ap for ap in candidates if tally.get(ap, 0) == max_votes]

                    if len(top_group) > quota:
                        # Trigger a revote restricted to the tied top candidates
                        room["revote_targets"] = set(top_group)
                        room["revote_quota"] = quota
                        room["round_votes"] = {}
                        await broadcast_refresh(room)
                        continue

                    # No need for revote; eliminate by ranking deterministically within remaining slots
                    ranked = sorted(candidates, key=lambda ap: (-tally.get(ap, 0), room["players"][ap]["name"].casefold()))
                    to_eliminate = set(ranked[:quota])

                    # mark eliminated and notify
                    for ep in to_eliminate:
                        room.setdefault("eliminated", set()).add(ep)
                        pname = room["players"][ep]["name"]
                        add_log(room, "player_eliminated", {"player_name": pname})

                    # clear revote context once resolved
                    room.pop("revote_targets", None)
                    room.pop("revote_quota", None)

                    # After elimination, check for winner or move to next round
                    if await declare_winner_if_any(room):
                        return
                    await start_next_round(room)


    except WebSocketDisconnect:
        pass
    finally:
        if room_id in rooms and pid:
            room = rooms[room_id]

            # Mark player as offline instead of removing
            if pid in room["players"]:
                room["players"][pid]["online"] = False
                room["players"][pid]["last_seen"] = time.time()
                room["players"][pid]["ws"] = None

                # Log disconnection
                player_name = room["players"][pid]["name"]
                add_log(room, "player_disconnected", {"name": player_name})

            # Clean up room if all players are offline for more than 5 minutes
            all_offline = all(not p.get("online", True) for p in room["players"].values())
            if all_offline:
                oldest_seen = min((p.get("last_seen", time.time()) for p in room["players"].values()), default=time.time())
                if time.time() - oldest_seen > 300:  # 5 minutes
                    rooms.pop(room_id, None)

            if room_id in rooms:
                # keep state consistent after leave
                if room["phase"] == PHASE_LOBBY:
                    room["start_votes"] = {x for x in room["start_votes"] if x in room["players"]}
                if room["phase"] == PHASE_REVEAL:
                    if len([x for x in room["round_reveals"].keys() if x in active_pids(room)]) == len(active_pids(room)):
                        room["phase"] = PHASE_CONFIRM
                if room["phase"] == PHASE_CONFIRM:
                    if len([x for x in room["round_confirms"] if x in active_pids(room)]) == len(active_pids(room)):
                        if room["round"] >= 3 and current_round_quota(room) > 0 and len(active_pids(room)) > 2:
                            room["phase"] = PHASE_VOTE
                        else:
                            await start_next_round(room)
                            return
                if room["phase"] == PHASE_VOTE:
                    if len([x for x in room.get("round_votes", {}).keys() if x in active_pids(room)]) == len(active_pids(room)):
                        # trigger tally path as if last vote happened
                        # simple fallback: start next round (cannot easily re-run tally on disconnect); keep state consistent
                        await start_next_round(room)
                        return

                await broadcast_refresh(room)
