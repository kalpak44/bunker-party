from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
import uuid
import random
import re

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


def make_character_for_room(room: dict, lang: str) -> dict[str, str]:
    """
    Create a character for a player ensuring no duplicate card values per key within the room.
    Preference is given to the player's selected language list; if it's exhausted for a key,
    fall back to any available value (including duplicates as a last resort).
    """
    # Prefer the player's language; if not present (shouldn't happen), use room default
    if lang not in GAME_DATA:
        lang = room.get("default_lang", "en")

    cards_by_lang = GAME_DATA[lang]["cards"]

    # Collect used values in the room per key (string comparison)
    used_per_key: dict[str, set] = {k: set() for k in REVEAL_KEYS}
    for p in room.get("players", {}).values():
        ch = p.get("character", {})
        for k in REVEAL_KEYS:
            v = ch.get(k)
            if v is not None:
                used_per_key[k].add(v)

    character: dict[str, str] = {}
    for k in REVEAL_KEYS:
        # Start with preferred language pool
        pool = list(cards_by_lang.get(k, []))
        # Union across all languages for this key
        union_pool = set(pool)
        for gd in GAME_DATA.values():
            union_pool.update(gd["cards"].get(k, []))

        # Filter out already used values for this key
        available = [v for v in pool if v not in used_per_key[k]]
        union_available = [v for v in union_pool if v not in used_per_key[k]]

        if available:
            character[k] = random.choice(available)
        elif union_available:
            character[k] = random.choice(union_available)
        else:
            # No unique options left at all for this key in any language — reject creating character
            raise ValueError(f"No unique cards remaining for key '{k}' in room {room.get('room_id')}")

    return character


async def safe_send(ws: WebSocket, msg: dict):
    try:
        await ws.send_json(msg)
    except Exception:
        pass


async def broadcast(room: dict, msg: dict):
    for p in list(room["players"].values()):
        await safe_send(p["ws"], msg)


async def broadcast_players(room: dict):
    await broadcast(room, {
        "type": "players",
        "players": [p["name"] for p in room["players"].values()],
    })


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


async def broadcast_state(room: dict):
    act_pids = active_pids(room)
    # Progress counts should consider only active (not eliminated) players
    reveals_done = len([pid for pid in room.get("round_reveals", {}).keys() if pid in act_pids])
    confirms_done = len([pid for pid in room.get("round_confirms", set()) if pid in act_pids])
    votes_done = len([pid for pid in room.get("round_votes", {}).keys() if pid in act_pids])
    # players_total is active participants count in current phase
    players_total = len(act_pids)

    await broadcast(room, {
        "type": "state",
        "phase": room["phase"],
        "round": room["round"],
        "event_idx": room["event_idx"],
        "players_total": players_total,
        "capacity": compute_unique_capacity(),
        "start_votes": len(room["start_votes"]),
        "reveals_done": reveals_done,
        "confirms_done": confirms_done,
        "votes_done": votes_done,
        "vote_quota": current_round_quota(room),
        "eliminated_names": [name_by_pid(room, pid) for pid in room.get("eliminated", set())],
        "revealed": {p["name"]: p["revealed"] for p in room["players"].values()},
    })


async def send_event_localized(room: dict):
    # same idx, localized per player
    for p in list(room["players"].values()):
        lang = p["lang"]
        event = GAME_DATA[lang]["bunkers"][room["event_idx"]]
        await safe_send(p["ws"], {"type": "bunker_event", "round": room["round"], "event": event})


async def start_next_round(room: dict):
    # Game might have already ended via eliminations
    act = active_pids(room)
    if len(act) <= 1:
        await declare_winner_if_any(room)
        return

    if all_used_all_cards(room):
        room["phase"] = PHASE_GAME_OVER
        # localized game over to each player
        for p in list(room["players"].values()):
            ui = GAME_DATA[p["lang"]]["ui"]
            await safe_send(p["ws"], {"type": "game_over", "message": f'{ui["game_over"]}. {ui["all_cards_used"]}'})
        await broadcast_state(room)
        return

    room["phase"] = PHASE_REVEAL
    room["round"] += 1
    room["round_reveals"] = {}
    room["round_confirms"] = set()
    room["round_votes"] = {}

    n = len(GAME_DATA[room["default_lang"]]["bunkers"])
    room["event_idx"] = random.randrange(n)

    await send_event_localized(room)
    await broadcast_state(room)


def compute_elimination_plan(total_players: int) -> dict[int, int]:
    # Distribute eliminations (total_players - 1) across rounds 3..7 as evenly as possible
    total_elims = max(0, total_players - 1)
    rounds = [3, 4, 5, 6, 7]
    base = total_elims // len(rounds)
    rem = total_elims % len(rounds)
    plan = {}
    for i, r in enumerate(rounds):
        plan[r] = base + (1 if i < rem else 0)
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
        for p in list(room["players"].values()):
            ui = GAME_DATA[p["lang"]]["ui"]
            msg_tpl = ui.get("congrats_winner", "Congratulations, {name}! You made it into the bunker.")
            try:
                message = msg_tpl.format(name=winner_name)
            except Exception:
                message = msg_tpl
            await safe_send(p["ws"], {"type": "game_over", "message": message})
        await broadcast_state(room)
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
    pid = uuid.uuid4().hex

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

        # no late join after start
        if room["phase"] != PHASE_LOBBY:
            await safe_send(ws, {"type": "error", "code": "game_started", "message": ui["game_started"]})
            await ws.close(code=1008)
            return

        # unique name (case-insensitive)
        existing = {p["name"].casefold() for p in room["players"].values()}
        if name.casefold() in existing:
            await safe_send(ws, {"type": "error", "code": "name_taken", "message": ui["name_taken"]})
            await ws.close(code=1008)
            return

        # capacity guard: refuse join if players exceed unique cards availability
        capacity = compute_unique_capacity()
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
            character = make_character_for_room(room, lang)
        except ValueError:
            await safe_send(ws, {"type": "error", "code": "no_unique_cards", "message": ui.get("error", "Error")})
            await ws.close(code=1013)
            return

        room["players"][pid] = {
            "name": name,
            "lang": lang,
            "ws": ws,
            "character": character,
            "revealed": {},
            "used_keys": set(),
        }

        await safe_send(ws, {
            "type": "init",
            "room_id": room_id,
            "player_name": name,
            "ui": ui,
            "labels": GAME_DATA[lang]["labels"],
            "character": character,
        })

        await broadcast_players(room)
        await broadcast_state(room)

        while True:
            msg = await ws.receive_json()
            t = msg.get("type")

            # VOTE START
            if t == "vote_start" and room["phase"] == PHASE_LOBBY:
                room["start_votes"].add(pid)
                await broadcast_state(room)

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
                room["round_reveals"][pid] = key

                await broadcast(room, {
                    "type": "player_reveal",
                    "player": p["name"],
                    "key": key,
                    "value": p["character"][key],
                })

                # if all active players revealed, go confirm
                if len([x for x in room["round_reveals"].keys() if x in active_pids(room)]) == len(active_pids(room)) and len(active_pids(room)) > 0:
                    room["phase"] = PHASE_CONFIRM
                    await broadcast(room, {"type": "phase", "phase": PHASE_CONFIRM})

                await broadcast_state(room)

            # CONFIRM ROUND END
            elif t == "confirm_round_end" and room["phase"] == PHASE_CONFIRM:
                # eliminated cannot act
                if pid in room.get("eliminated", set()):
                    continue
                room["round_confirms"].add(pid)
                await broadcast_state(room)

                # If all active confirmed, either start vote (from round 3) or next round
                if len([x for x in room["round_confirms"] if x in active_pids(room)]) == len(active_pids(room)) and len(active_pids(room)) > 0:
                    if room["round"] >= 3 and current_round_quota(room) > 0 and len(active_pids(room)) > 1:
                        room["phase"] = PHASE_VOTE
                        room["round_votes"] = {}
                        await broadcast(room, {"type": "phase", "phase": PHASE_VOTE})
                        await broadcast_state(room)
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
                for apid in active_pids(room):
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
                await broadcast_state(room)

                # If all active voted, tally
                if len([x for x in room["round_votes"].keys() if x in active_pids(room)]) == len(active_pids(room)):
                    # tally votes for targets among active
                    tally: dict[str, int] = {}
                    for voter, tgt in room["round_votes"].items():
                        if voter in active_pids(room) and tgt in active_pids(room):
                            tally[tgt] = tally.get(tgt, 0) + 1

                    # rank candidates by votes desc, then by name asc for determinism
                    ranked = sorted(active_pids(room), key=lambda ap: (-tally.get(ap, 0), room["players"][ap]["name"].casefold()))
                    quota = min(current_round_quota(room), max(0, len(active_pids(room)) - 1))
                    to_eliminate = set(ranked[:quota]) if quota > 0 else set()

                    # mark eliminated and notify
                    for ep in to_eliminate:
                        room.setdefault("eliminated", set()).add(ep)
                        pname = room["players"][ep]["name"]
                        # notify all (log purpose)
                        await broadcast(room, {"type": "eliminated_info", "player": pname})
                        # notify eliminated personally
                        try:
                            ui = GAME_DATA[room["players"][ep]["lang"]]["ui"]
                            txt = ui.get("you_are_out", "You are out. You can observe but not participate.")
                            await safe_send(room["players"][ep]["ws"], {"type": "eliminated", "message": txt})
                        except Exception:
                            pass

                    # After elimination, check for winner or move to next round
                    if await declare_winner_if_any(room):
                        return
                    await start_next_round(room)

    except WebSocketDisconnect:
        pass
    finally:
        if room_id in rooms:
            room = rooms[room_id]
            room["players"].pop(pid, None)
            room["start_votes"].discard(pid)
            room["round_reveals"].pop(pid, None)
            room["round_confirms"].discard(pid)
            room.get("round_votes", {}).pop(pid, None)
            room.get("eliminated", set()).discard(pid)

            if not room["players"]:
                rooms.pop(room_id, None)
            else:
                # keep state consistent after leave
                if room["phase"] == PHASE_LOBBY:
                    room["start_votes"] = {x for x in room["start_votes"] if x in room["players"]}
                if room["phase"] == PHASE_REVEAL:
                    if len([x for x in room["round_reveals"].keys() if x in active_pids(room)]) == len(active_pids(room)):
                        room["phase"] = PHASE_CONFIRM
                if room["phase"] == PHASE_CONFIRM:
                    if len([x for x in room["round_confirms"] if x in active_pids(room)]) == len(active_pids(room)):
                        if room["round"] >= 3 and current_round_quota(room) > 0 and len(active_pids(room)) > 1:
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

                await broadcast_players(room)
                await broadcast_state(room)
