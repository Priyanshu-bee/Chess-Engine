# Engine Architecture & Roadmap

This document outlines the package layout, timing protocols, completed features, and the future scope of improvements for the chess engine.

---

## 🏛️ Current Architecture & Packages

The engine is structured into five decoupled packages, separating rules, strategy, timing, search execution, and execution hooks:

```
src/
├── core/
│   ├── Board.java             (Chess state, bitboards, incremental Zobrist hashing, 3-fold repetition checks)
│   ├── BoardListener.java     (Functional interface for move hooks)
│   └── Move.java, Color.java, Square.java, PieceType.java
├── brain/
│   ├── Brain.java             (Strategic intelligence interface with think() method)
│   ├── NegamaxBrain.java      (100% clock-blind search implementation)
│   └── MctsBrain.java         (Simulation-based MCTS skeleton)
├── time/
│   ├── TimeManager.java       (Abstract clock & progress decision interface)
│   └── StandardTimeManager.java (Concrete time manager implementing soft/hard/grace limits)
├── search/
│   ├── SearchSession.java     (Thread driver & monitor coordinator)
│   ├── Logger.java            (Unified diagnostic logger writing to stdout and engine.log)
│   ├── SearchProgress.java    (DTO for scores, depth, stability, and bestMove)
│   ├── SearchConstraints.java (DTO for UCI time limits)
│   └── SearchAbortedException.java (Lite unwinding exception)
└── execution/
    └── MyBot.java             (UCI loop parser & application entry point)
```

### Search Lifecycle Flow
1. **Instantiation**: `MyBot` instantiates a persistent `Brain` instance (keeping Transposition Tables and trees alive across moves).
2. **Move Request**: `MyBot` receives `go` command, creates a `SearchSession` and starts it.
3. **Budgeting**: `SearchSession` instantiates a `TimeManager` which calculates soft and hard limits.
4. **Initialization**: `SearchSession` clones the board state, attaches a throttled `BoardListener` to the clone, and initializes the brain:
   `brain.init(searchBoard, allocatedTimeMs, targetDepth, targetNodes, isInfinite);`
5. **Execution**: The search runs in a background thread. The main thread returns immediately.
6. **Monitoring**: A monitor thread polls every 10ms, calling:
   `timeManager.shouldStop(elapsedMs, brain.getProgress())`
7. **Aborting**: If the time manager decides to stop, it sets `stopSignal = true`. The next move executed on the board clone triggers `SearchAbortedException` via the throttled Board Listener, unwinding the recursion stack instantly and returning the best move.

---

## 🏆 Completed Architecture & Features

### 1. Incremental Zobrist Hashing & Draw Detection (`core/Board.java`)
* **Deterministic Keys**: Statically pre-calculated pseudorandom 64-bit keys using a fixed seed for piece placements ($12 \times 64$), castling rights ($16$), en-passant target files ($9$), and side to move.
* **XOR Updates**: Added the `zobristHash` field. It is initialized from scratch during FEN loading and updated incrementally using fast bitwise XOR operations (`^`) inside `makeMove()`.
* **Zero-Cost Unmake**: `BoardState` stores the hash of that state. Inside `unmakeMove()`, we simply pop the previous hash from `stateHistory` at zero computational cost, completely eliminating hash drift.
* **3-Fold Repetition Check**: Scans backwards up to `halfmoveClock` items in `stateHistory` to match the current hash. Modified `isDraw()` to include this check.
* **Cloning**: Updated `copy()` to copy the current hash and push the hashes correctly in the cloned state stack.

### 2. Persistent Brains & Re-rooting
* **MyBot.java**: Spawns a single persistent `Brain` instance to keep caches, tables, and search trees alive across turns.
* **Init Hook**: `Brain.init(board, allocatedTimeMs, targetDepth, targetNodes, isInfinite)` signature exposes targets directly, allowing MCTS to perform tree re-rooting on board update instead of clearing the tree.

### 3. UCI Handshake & Pondering Support
* **Handshake Option**: Announces `option name Ponder type check default false` in the UCI handshake. This unlocks the pondering option in GUI settings, instructing Cute Chess to send `go ponder` commands.
* **Time Cancellation**: Interrupted search threads unwind instantly when `stop` or `ponderhit` commands change timing budgets mid-search.

### 4. Dynamic Time Management (`time/StandardTimeManager.java`)
* **Soft & Hard Limits**: Sets a soft limit (ideal time) and hard limit (maximum extension cap).
* **Grace Periods**: Extends the search budget if:
  * The position score is unstable/volatile.
  * The current search depth layer is near completion (>85%).
* **Immediate Cutoffs**: Stops immediately if the position is stable and progress is low to conserve clock time.

### 5. Unified Logging & Diagnostics (`search/Logger.java`)
* **Dual Output**: Prints formatted `info` and `bestmove` tokens to standard output (for Cute Chess) while appending a complete search session trace to `engine.log` in the repository root.

---

## 🚀 Scope of Future Improvements

### 1. Sophisticated Time Management Strategies
We can extend `StandardTimeManager` with chess-specific evaluations:
* **Position-Aware Multipliers**: Before search, query the brain for a complexity multiplier:
  * Highly tactical position (both kings exposed, queens active) $\rightarrow$ Increase budget ($1.5\text{x}$ to $2.0\text{x}$).
  * Simple endgame or recapture $\rightarrow$ Decrease budget ($0.1\text{x}$ to $0.2\text{x}$).
* **Dynamic Mid-Search Adjustments**:
  * **Panic Extensions**: If the search score drops drastically (suggesting a blunder or threat), dynamically push the soft limit outward to find a defensive line.
  * **Confidence Cutoffs**: If the best move remains stable for 5+ depths and the score is high, stop the search early to conserve clock time.

### 2. Full MCTS Brain Implementation
Develop the skeleton in `MctsBrain.java` into a competitive Monte Carlo Tree Search:
* **Node Structure**: Implement a tree of `MctsNode` objects tracking visit counts ($N$) and value scores ($Q$).
* **UCT Selection**: Use the Upper Confidence Bound applied to Trees formula to balance exploration and exploitation:
  \[UCT = \frac{Q_i}{N_i} + c \times \sqrt{\frac{\ln N_{parent}}{N_i}}\]
* **Expansion & Rollouts**: Add lightweight random or heuristic-guided playouts to evaluate terminal outcomes.
* **Backpropagation**: Feed simulation results back up the node path.

### 3. Tree Reuse & Permanent Brain Pondering
Instead of relying on the GUI to tell us when to ponder, we can control pondering ourselves:
* **Permanent Brain**: The moment we output `bestmove`, the engine immediately starts MCTS rollouts on the opponent's options.
* **MCTS Tree Re-rooting**: When the opponent plays a move:
  * Compare the new board state against the children of the MCTS root.
  * If it matches, call `setRoot(matchingChildNode)`.
  * This preserves thousands of simulations we ran during the opponent's turn, playing our reply instantly.

---

## 🧪 Verification & Testing Status

To ensure engine safety, all implementation updates are checked against a dual verification process:

### 1. Fully Verified Features (In-Game Tested)
* **Zobrist Hashing & Repetition Detection**: Verified. State histories, clones, and unmake functions correctly calculate hashes without drift across 150+ moves.
* **Dynamic Time Management**: Verified. The time manager correctly extends budgets under evaluation volatility/layer finishes, and stops immediately when stable.
* **UCI Checkmate Formatting**: Verified. Engine outputs `score mate <N>` properly, and Cute Chess renders the remaining move count correctly.

### 2. Partially Verified Features (Pondering)
* **Ponder Handshake**: Verified. The handshake successfully announces the `Ponder` option type.
* **Ponder Execution & Cancel**: Verified via automated test suite. The engine correctly processes `go ponder`, transitions on `ponderhit`, and terminates on `stop`.
* **In-Game Match Verification**: **Pending**. Pondering transitions have not yet been verified inside an interactive real game.
