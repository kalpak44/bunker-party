# Bunker Party

## What it is
- A light, fast **browser-based party game**, inspired by *Bunker*.
- Each player gets a quirky survival character made of **hidden cards**:
  profession, health, age, gender, hobby/skill, phobia, and an item.
- Every round introduces a **chaotic bunker event** that sets the tone.
- Players **reveal one card per round**, argue their usefulness, and vote.
- From round 3 onward, players may be **eliminated by voting**.
- The game ends when **one or two players remain** - or when all cards are revealed.

The goal: **convince others you deserve a place in the bunker.**


## How to play
1. Open the app in your browser.
2. Enter your name and choose a language  
   *(English, Русский, Български)*.
3. Create a new game to get a **4-digit room code**, or join an existing room.
4. In the lobby, everyone votes to start (minimum **3 players** required).
5. Each round:
    - A bunker event appears.
    - Every active player reveals **one hidden card**.
    - Players confirm the end of the round.
6. From **round 3**, players vote to eliminate others  
   (the number eliminated depends on player count).
7. Eliminated players can **observe**, but not participate.
8. The game ends when:
    - Only **one or two players** remain, or
    - **All cards are revealed**.

## Game rules (quick)
- One card reveal **per player per round**.
- Cards cannot be revealed twice.
- Voting is anonymous and simultaneous.
- Ties trigger a **revote** among tied players.
- The game dynamically balances eliminations so it always ends with survivors.
- Player characters are generated so that **card values are unique** within a room whenever possible.


## Tech overview
- **Backend:** Spark Java + WebSockets
- **Frontend:** Vanilla HTML + Tailwind CSS
- **Real-time:** WebSocket game state sync
- **No database:** All rooms live in memory
- **Zero auth:** Just enter a name and play

## Requirements
- Java 17 or newer
- Maven 3.6+ (for building from source)
- Docker (optional, for containerized deployment)

## Play locally (without Docker)

### Option 1: Run directly from IDE (IntelliJ IDEA / Eclipse)
1. Install Java 17 or newer
2. Open the project in your IDE
3. Run the `Main.java` class (com.bunkerparty.Main)
4. Open http://localhost:8000 in your browser

### Option 2: Build and run JAR
1. Install Java 17 or newer and Maven 3.6+
2. In the project folder:
   ```bash
   mvn clean package
   java -jar target/bunker-party-1.0.0.jar
   ```
3. Open http://localhost:8000 in your browser

## Run with Docker

### Using Jib (recommended - no Dockerfile needed)
Build and push to registry:
```bash
mvn compile jib:build
```

Build to Docker daemon:
```bash
mvn compile jib:dockerBuild
docker run --rm -p 8000:8000 kalpak44/bunker-party:latest
```

Build to tar file:
```bash
mvn compile jib:buildTar
docker load --input target/jib-image.tar
docker run --rm -p 8000:8000 kalpak44/bunker-party:latest
```

### Using traditional Dockerfile
Build and run locally:
```bash
docker build -t bunker-party:local .
docker run --rm -p 8000:8000 bunker-party:local
```

Or use the published image:
```bash
docker run --rm -p 8000:8000 kalpak44/bunker-party:latest
```

Open the game at: http://localhost:8000

## Development
The application automatically detects if it's running from IDE or JAR:
- **IDE**: Serves static files from `./static` directory
- **JAR/Container**: Serves static files from classpath (embedded in JAR)

Run directly from IDE or use:
```bash
mvn clean compile exec:java -Dexec.mainClass="com.bunkerparty.Main"
```

## Notes
- Room codes are **4 digits** and case-insensitive.
- Rooms are destroyed automatically when empty.
- This is a party game - **argue passionately, eliminate fairly, and be kind**.

---

Have fun - and good luck surviving the bunker ☢️
