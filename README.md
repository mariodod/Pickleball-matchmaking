# 🎾 Pickleball Matchmaking & Practice Rotation

A lightweight, mobile-friendly web application for organizing doubles pickleball practice sessions, King of the Court ladders, and automated player rotations with waiting reserve queues.

Built with **Java 21**, **Spring Boot 3.4.2**, **Spring Data JPA**, and an embedded file-backed **H2 Database**.

---

## ✨ Features

- **🏆 Automated King of the Court Ladder**:
  - **Winners move up** toward Court 1.
  - **Losers slide down** toward the bottom court.
  - **Partners split and mix** against incoming players so people don't play with the same partner all session.
- **⏱️ Automated Reserve Bench (FIFO Queue)**:
  - Supports waiting players on any court.
  - Waiting players automatically cycle into the next match in First-In-First-Out order, while resting players rotate to the reserve queue.
- **📱 Mobile-First Court Cards**:
  - Designed specifically for phones and tablets next to the courts.
  - Large, thumb-friendly tap targets to record winners with a single tap.
- **🔢 Persistent Round Tracking**:
  - Automatically advances and displays the current round (*Round 1*, *Round 2*, etc.).
- **💾 Local Database Persistence**:
  - All courts, matches, and session rounds are saved to a lightweight local database file (`./data/pickleball.mv.db`), persisting seamlessly across server restarts.
- **🔄 Session Reset**:
  - One-click session reset with safety confirmation to start fresh practices.

---

## 🚀 Quick Start

### Prerequisites
- **Java 21** or later installed (e.g. OpenJDK 21, Microsoft OpenJDK 21, Temurin 21).

### Running the Application

1. **Clone the repository**:
   ```bash
   git clone https://github.com/mariodod/Pickleball-matchmaking.git
   cd Pickleball-matchmaking
   ```

2. **Run the server**:
   - On Windows (PowerShell):
     ```powershell
     ./gradlew bootRun
     ```
   - On macOS / Linux:
     ```bash
     ./gradlew bootRun
     ```

3. **Open the App**:
   Navigate to [**http://localhost:8080/**](http://localhost:8080/) in any web browser.

4. **Run Automated Tests**:
   ```bash
   ./gradlew test
   ```

---

## 📖 How to Use the App (Step-by-Step Guide)

```
       [ 1. Add Courts & Players ]
                    │
                    ▼
       [ 2. Play Matches & Tap Winners (🏆) ]
                    │
                    ▼
       [ 3. Click "Rotate to Next Matches" ]
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
  Winners Move UP          Losers Move DOWN
  Partners Split           Partners Split
  Reserves Enter           Resting Players Sit Out
                    │
                    ▼
       [ 4. Advance to Next Round (Round 2+) ]
```

### 1. Setting Up Courts
1. Scroll down to the **➕ Add a New Court** form.
2. Enter the names for:
   - **Team 1**: Player 1 and Player 2.
   - **Team 2**: Player 1 and Player 2.
3. *(Optional)* If you have waiting players on this court, click **+ Add Another Reserve** and type their names.
4. Click **Add Court to Ladder**.
5. Repeat for as many courts as you have at your practice venue (Court 1, Court 2, Court 3...).

### 2. Recording Match Results
- Each court card displays **Team 1** vs **Team 2**.
- Once a game is complete, **tap the card of the winning team**.
- The winning team immediately highlights in vibrant green and displays a `🏆 WINNER` badge.
- *(Mistake? Tap the selected team again to deselect, or tap the other team to switch).*

### 3. Rotating to the Next Round
- Ensure every active court has a declared winner.
- Tap the **`🔄 Rotate to Next Matches`** button at the top.
- The app automatically:
  1. Increments the **Round Counter** (e.g., Round 1 ➔ Round 2).
  2. Promotes winners to higher courts and sends losers to lower courts.
  3. Splits former partners so they play alongside new incoming players.
  4. Brings any waiting reserve players onto the court and rotates resting players to the bench.
  5. Clears match results for the new round.

### 4. Managing Courts During Practice
- **Remove a Court**: Tap the **✕ Delete** button in the header of any court card to remove it.
- **Add a Court**: Add a new court at any time during practice.

### 5. Resetting a Practice Session
- When practice ends or you want to start a brand-new session with a new group of players:
- Tap **Reset Session** in the top navigation bar.
- Confirm the browser prompt to wipe all courts and reset the round counter back to **Round 1**.

---

## 🧠 Rotation Algorithm Rules

| Court Level | What Happens to Winners | What Happens to Losers |
| :--- | :--- | :--- |
| **Top Court (Court 1)** | **Stay on Court 1**, split partners, and play with winners advancing from Court 2. | **Move down** to Court 2, split partners, and play with losers from higher courts. |
| **Middle Courts (Court $i$)** | **Move up** to Court $i-1$, split partners, and play with losers sliding down. | **Move down** to Court $i+1$, split partners, and play with winners moving up. |
| **Bottom Court (Last Court)** | **Move up** to the court above, split partners. | **Stay on the bottom court**, split partners, and play with losers sliding down from above. |
| **Single Court Session** | Split partners: Winner 1 plays with Loser 1 against Winner 2 and Loser 2. | If reserves are waiting, they enter first (FIFO) and excess players sit out. |

---

## 🛠️ Technical Stack & Architecture

- **Backend**: Java 21, Spring Boot 3.4.2 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`)
- **Persistence**: File-backed **H2 Database** (`./data/pickleball.mv.db`) with Hibernate ORM.
- **Testing**: JUnit 5, `@SpringBootTest` with isolated in-memory H2 configuration (`jdbc:h2:mem:testdb`).
- **Frontend**: Pure semantic HTML5, responsive CSS Grid / Flexbox, and vanilla JavaScript (no Node.js, Webpack, or external CDN dependencies needed).
- **Developer Console**: Built-in H2 Web Console available during local development at:
  - **URL**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - **JDBC URL**: `jdbc:h2:file:./data/pickleball`
  - **Username**: `sa`
  - **Password**: *(leave blank)*

---

## 📡 REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/courts` | Retrieve all active courts in order |
| `POST` | `/courts/add` | Add a new court with `team1`, `team2`, and `reserves` |
| `DELETE` | `/courts/{id}` | Delete a specific court by its ID |
| `POST` | `/courts/delete` | Delete the last court in the ladder |
| `PUT` | `/courts/{id}/winner` | Update court winner (`{ "winner": "team1" }` or `"team2"` or `""`) |
| `POST` | `/courts/calculate` | Execute rotation algorithm and increment the round |
| `GET` | `/courts/round` | Retrieve current round counter (`{ "round": 1 }`) |
| `POST` | `/courts/reset` | Clear all courts and reset session to Round 1 |

---

## 📄 License
This project is open source and available under the [MIT License](LICENSE).
