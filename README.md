# Bunker Party

A light, fast **browser-based party game**, inspired by *Bunker*.

## What it is
- Each player gets a quirky survival character made of **hidden cards**: profession, health, age, gender, hobby/skill, phobia, and an item.
- Every round introduces a **bunker event** that sets the tone.
- Players **reveal one card per round**, argue their usefulness for survival in the bunker.
- The game continues until **all cards are revealed**.

The goal: **convince others you deserve a place in the bunker.**


## How to play
1. Open the app in your browser.
2. Enter your name and choose a language *(English, Русский, Български)*.
3. Create a new game to get a **4-digit room code**, or join an existing room via a link or code.
4. In the lobby, everyone votes to start (minimum **3 players** required).
5. Each round:
    - A bunker event appears.
    - Every player reveals **one hidden card**.
    - Players discuss their characters and confirm the end of the round.
6. The game ends when **all cards are revealed**.

## Tech overview
- **Backend:** Spark Java + WebSockets
- **Frontend:** Vanilla JS + Tailwind CSS
- **Real-time:** WebSocket game state sync
- **No database:** All rooms live in memory
- **Zero auth:** Just enter a name and play

## Requirements
- Java 17 or newer
- Maven 3.6+

## Run locally

### Option 1: Run directly from IDE
1. Open the project in your IDE (IntelliJ IDEA recommended).
2. Run the `Main.java` class (`com.bunkerparty.Main`).
3. Open http://localhost:8000 in your browser.

### Option 2: Build and run JAR
1. In the project folder:
   ```bash
   mvn clean package
   java -jar target/bunker-party-1.0.0.jar
   ```
2. Open http://localhost:8000 in your browser.

## Run with Docker (Jib)
The project uses **Jib** to build Docker images without a `Dockerfile`.

### Build to Docker daemon
```bash
mvn compile jib:dockerBuild
docker run --rm -p 8000:8000 bunker-party:latest
```

### Build to tar file
```bash
mvn compile jib:buildTar
docker load --input target/jib-image.tar
docker run --rm -p 8000:8000 bunker-party:latest
```

Open the game at: http://localhost:8000

## Notes
- Room codes are **4 digits**.
- Rooms are destroyed automatically when empty.
- This is a party game - **argue passionately and have fun**.

---

Have fun - and good luck surviving the bunker ☢️
