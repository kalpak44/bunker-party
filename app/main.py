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


def make_character(lang: str) -> dict[str, str]:
    cards = GAME_DATA[lang]["cards"]
    return {k: random.choice(cards[k]) for k in REVEAL_KEYS}


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


async def broadcast_state(room: dict):
    await broadcast(room, {
        "type": "state",
        "phase": room["phase"],
        "round": room["round"],
        "event_idx": room["event_idx"],
        "players_total": len(room["players"]),
        "start_votes": len(room["start_votes"]),
        "reveals_done": len(room["round_reveals"]),
        "confirms_done": len(room["round_confirms"]),
        "revealed": {p["name"]: p["revealed"] for p in room["players"].values()},
    })


async def send_event_localized(room: dict):
    # same idx, localized per player
    for p in list(room["players"].values()):
        lang = p["lang"]
        event = GAME_DATA[lang]["bunkers"][room["event_idx"]]
        await safe_send(p["ws"], {"type": "bunker_event", "round": room["round"], "event": event})


async def start_next_round(room: dict):
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

    n = len(GAME_DATA[room["default_lang"]]["bunkers"])
    room["event_idx"] = random.randrange(n)

    await send_event_localized(room)
    await broadcast_state(room)


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

        character = make_character(lang)

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

                if len(room["start_votes"]) == len(room["players"]) and len(room["players"]) > 0:
                    room["round"] = 0
                    await start_next_round(room)

            # REVEAL
            elif t == "reveal_card" and room["phase"] == PHASE_REVEAL:
                key = (msg.get("key") or "").strip()
                if key not in REVEAL_KEYS:
                    continue

                p = room["players"].get(pid)
                if not p:
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

                # if all revealed, go confirm
                if len(room["round_reveals"]) == len(room["players"]) and len(room["players"]) > 0:
                    room["phase"] = PHASE_CONFIRM
                    await broadcast(room, {"type": "phase", "phase": PHASE_CONFIRM})

                await broadcast_state(room)

            # CONFIRM ROUND END
            elif t == "confirm_round_end" and room["phase"] == PHASE_CONFIRM:
                room["round_confirms"].add(pid)
                await broadcast_state(room)

                if len(room["round_confirms"]) == len(room["players"]) and len(room["players"]) > 0:
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

            if not room["players"]:
                rooms.pop(room_id, None)
            else:
                # keep state consistent after leave
                if room["phase"] == PHASE_LOBBY:
                    room["start_votes"] = {x for x in room["start_votes"] if x in room["players"]}
                if room["phase"] == PHASE_REVEAL:
                    if len(room["round_reveals"]) == len(room["players"]):
                        room["phase"] = PHASE_CONFIRM
                if room["phase"] == PHASE_CONFIRM:
                    if len(room["round_confirms"]) == len(room["players"]):
                        await start_next_round(room)
                        return

                await broadcast_players(room)
                await broadcast_state(room)
