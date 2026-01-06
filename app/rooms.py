import random

rooms: dict[str, dict] = {}

REVEAL_KEYS = ["profession", "health", "age", "gender", "hobby", "phobia", "item"]

PHASE_LOBBY = "lobby"
PHASE_REVEAL = "reveal"
PHASE_CONFIRM = "confirm"
PHASE_VOTE = "vote"
PHASE_GAME_OVER = "game_over"


def _gen_room_code() -> str:
    #  4 digits
    return str(random.randint(1000, 9999))


def create_room(default_lang: str) -> str:
    # ensure uniqueness
    for _ in range(50):
        code = _gen_room_code()
        if code not in rooms:
            rooms[code] = {
                "room_id": code,
                "default_lang": default_lang,

                "players": {},  # pid -> player dict
                "pid_by_name": {},  # name.casefold() -> pid (for reconnection)
                "phase": PHASE_LOBBY,
                "round": 0,
                "event_idx": None,

                "start_votes": set(),

                "round_reveals": {},     # pid -> key
                "round_confirms": set(), # pid -> confirmed
                "round_votes": {},       # pid -> target_pid
                "eliminated": set(),     # set of pids eliminated
                "elim_plan": None,       # list[int] of eliminations per rounds 3..7

                "logs": [],              # structured log entries: {type, timestamp, data}
            }
            return code
    # fallback (extremely unlikely)
    code = _gen_room_code()
    rooms[code] = {
        "room_id": code,
        "default_lang": default_lang,
        "players": {},
        "pid_by_name": {},
        "phase": PHASE_LOBBY,
        "round": 0,
        "event_idx": None,
        "start_votes": set(),
        "round_reveals": {},
        "round_confirms": set(),
        "round_votes": {},
        "eliminated": set(),
        "elim_plan": None,
        "logs": [],
    }
    return code
