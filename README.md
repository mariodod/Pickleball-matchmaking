# 🎾 Pickleball Matchmaking & Practice Rotation

A small, mobile-friendly web app for running doubles pickleball practice sessions: a King of the
Court ladder, automatic partner rotation, and a FIFO bench for waiting players.

Built with **Java 21**, **Spring Boot 4.1.1**, **Spring Data JPA** and an embedded file-backed
**H2 database**. The frontend is a single static HTML page with vanilla JavaScript — no build step,
no CDN, no Node.js.

---

## ✨ Features

- **🏆 King of the Court ladder** — winners move up, losers move down (players waiting on the bench
  take the court seats first).
- **🔀 Partner splitting** — you never keep the same partner for two rounds in a row.
- **⏱️ FIFO bench** — waiting players enter the courts in the order they arrived; players who drop
  out of a rotation go back to the bench.
- **📱 Mobile-first cards** — big tap targets next to the courts, one tap records the winner.
- **🔢 Round counter** — persisted in the database, so the round survives a server restart.
- **💾 Persistence** — courts, results and the round counter live in `./data/pickleball.mv.db`.
- **🔄 Session reset** — clears every court and starts again at Round 1.

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** or later installed (e.g. OpenJDK 21, Temurin 21, Microsoft Build of OpenJDK 21).
  The build compiles against the Java 21 toolchain.

### Run it

```bash
git clone https://github.com/mariodod/Pickleball-matchmaking.git
cd Pickleball-matchmaking
./gradlew bootRun        # Windows: .\gradlew.bat bootRun
```

Open [http://localhost:8080/](http://localhost:8080/) — the root URL redirects to the court screen.

### Run the tests

```bash
./gradlew test
```

The test profile uses in-memory databases, so tests never touch `./data/pickleball.mv.db`.

---

## 📖 How to Use the App

```
   [ 1. Add courts & players ]
              │
              ▼
   [ 2. Play, tap the winning team (🏆) ]
              │
              ▼
   [ 3. Tap "🔄 Rotate to Next Matches" ]
              │
   ┌──────────┴───────────┐
   ▼                      ▼
 Winners move UP       Losers move DOWN
 Partners split        Partners split
 Bench enters          Resting players sit out
              │
              ▼
   [ 4. Round counter advances ]
```

### 1. Set up the courts

1. Scroll to **➕ Add a New Court**.
2. Enter **Team 1** (Player 1, Player 2) and **Team 2** (Player 1, Player 2).
3. *(Optional)* Add players waiting on the bench with **+ Add Another Reserve**.
4. Press **Add Court to Ladder**, and repeat for every court. The first court you add is the top
   court of the ladder.

### 2. Record results

- Every court card shows **Team 1** vs **Team 2**.
- Tap the winning team: it turns green and shows a **🏆 WINNER** badge.
- Tap the same team again to clear the result, or tap the other team to switch the winner.

### 3. Rotate to the next round

- Every court needs a result first, otherwise the rotation is rejected with
  *"Every court needs a winner before rotating"*.
- Tapping **🔄 Rotate to Next Matches** promotes winners, demotes losers, splits partners, pulls the
  bench onto the courts and increments the round counter.
- Results are cleared so the new round starts fresh.

### 4. Manage the session

- **Remove a court**: tap **✕ Delete** in the court header.
- **Add a court**: any time during the session.
- **Reset**: **Reset Session** in the header wipes all courts and resets the round counter to 1
  (with a confirmation prompt).

---

## 🧠 Rotation Rules

Every rotation builds a queue per court and then fills it:

1. The court starts with its **bench players** (FIFO, oldest first).
2. **Four players are appended** to that queue in the order `A.p1, B.p1, A.p2, B.p2`, where `A` and
   `B` are the two teams that feed the court.
3. The **first four names** of the queue play: names 1–2 are Team 1, names 3–4 are Team 2.
4. **Everyone left in the queue sits out** and becomes the new bench.

With an empty bench this yields **Team 1 = (A.p1, B.p1)** and **Team 2 = (A.p2, B.p2)**: the two
player-1 slots play together, partners are split, and the player-2 slots play together.

Who feeds which court:

| Court | Team A | Team B |
| :--- | :--- | :--- |
| **Top court** | Its own winners | The winners of the court below |
| **Middle court** | The winners of the court below (moving up) | The losers of the court above (moving down) |
| **Bottom court** | Its own losers | The losers of the court above |
| **Single court** | Its winners | Its losers |

So, with an empty bench, winners always move up and losers always move down, and the top and
bottom courts keep one of their own groups.

---

## 📡 REST API

All endpoints live under `/courts`. Errors are returned as
[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details, e.g.
`{"status":400,"title":"Bad Request","detail":"Every court needs a winner before rotating"}`.

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/courts` | All courts, in ladder order |
| `POST` | `/courts/add` | Add a court (see body below) |
| `DELETE` | `/courts/{id}` | Delete one court by id |
| `PUT` | `/courts/{id}/winner` | Set the winner: `{"winner":"team1"\|"team2"\|""}` (empty clears it) |
| `POST` | `/courts/calculate` | Run the rotation and increment the round |
| `GET` | `/courts/round` | Current round, e.g. `{"round":1}` |
| `POST` | `/courts/reset` | Delete all courts and reset the round to 1 |

`PUT /courts/{id}/winner` takes `{"winner":"team1"}` or `{"winner":"team2"}` (case-insensitive,
surrounding spaces are ignored) and `{"winner":""}` to clear a recorded result. A request without a
`winner` field is rejected with `400`.

Body of `POST /courts/add`:

```json
{
  "team1": { "player1": "Alice", "player2": "Bob" },
  "team2": { "player1": "Charlie", "player2": "David" },
  "reserves": ["Eve", "Frank"]
}
```

`reserves` is optional. Player names must be non-empty; the request is rejected with `400`
otherwise.

A court is returned as:

```json
{
  "id": 1,
  "team1": { "player1": "Alice", "player2": "Bob" },
  "team2": { "player1": "Charlie", "player2": "David" },
  "reserves": ["Eve"],
  "team1Win": false,
  "team2Win": false
}
```

---

## 🛠️ Technical Stack

- **Backend**: Java 21, Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`).
- **Persistence**: Hibernate ORM on a file-backed H2 database (`./data/pickleball.mv.db`).
- **Frontend**: one static HTML page (`src/main/resources/static/courts.html`) with inline CSS and
  vanilla JavaScript.
- **Tests**: JUnit 5 with `@SpringBootTest`; the service is covered directly and the HTTP API is
  covered against a real server on a random port.
- **Build**: Gradle 9.8.1 wrapper.

### Project layout

```
src/main/java/com/pickleball/pickleball
├── controller/   HTTP endpoints (courts, static redirect, error handling)
├── model/        JPA entities (Court, SessionState) and the Pair record
├── repository/   Spring Data repositories
└── service/      Ladder and rotation logic
```

### H2 console

The H2 web console is enabled by default at
[http://localhost:8080/h2-console](http://localhost:8080/h2-console) (reachable from localhost
only; it is disabled in the test profile). Connect with JDBC URL
`jdbc:h2:file:./data/pickleball;AUTO_SERVER=TRUE`, user `sa`, empty password.

---

## 📄 License

Released under the [MIT License](LICENSE).
