# Bunker Party

What it is
- A light, fast party game you play in the browser, inspired by “Bunker.”
- Each player gets a quirky survival character made of hidden cards (profession, health, age, hobby/skill, phobia, item, etc.).
- In each round, a funny/chaotic “bunker event” sets the mood for discussion.
- Players reveal one card per round and try to convince others they’re a valuable teammate.
- The game ends when everyone has revealed all their cards.

How to play
1. Open the app in your browser.
2. Enter your name and pick a language (English, Русский, Български).
3. Create a new game to get a 4‑digit room code — or join an existing room with its code.
4. When everyone is ready, start the game. Reveal one card per round, discuss, then confirm the round end.
5. Keep going through new bunker events until all cards are revealed.

Play locally (without Docker)
1. Install Python 3.12 or newer.
2. In the project folder:
   ```bash
   python -m venv .venv
   source .venv/bin/activate   # Windows: .venv\Scripts\activate
   pip install -r requirements.txt
   uvicorn app.main:app --reload
   ```
3. Open http://localhost:8000 in your browser.

Run with Docker
- Build and run locally:
  ```bash
  docker build -t bunker-party:local .
  docker run --rm -p 8000:8000 bunker-party:local
  ```
- Or use the published image:
  ```bash
  docker run --rm -p 8000:8000 kalpak44/bunker-party:latest
  ```

Open the game at: http://localhost:8000

Have fun and argue kindly!
