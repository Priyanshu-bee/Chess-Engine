# Engine Architecture & Roadmap

This document outlines the current package layout, timing protocols, and the future scope of improvements for the chess engine.

---

## 🏛️ Current Architecture & Packages

The engine is structured into three decoupled packages, separating chess rules, search execution, and strategic intelligence:

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
└── search/
    ├── SearchSession.java     (Thread driver & monitor coordinator)
    ├── SearchProgress.java    (DTO for scores, depth, stability, and bestMove)
    ├── SearchConstraints.java (DTO for UCI time limits)
    └── SearchAbortedException.java (Lite unwinding exception)
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

## 🚀 Scope of Future Improvements

### 1. Sophisticated Time Management Strategies
Currently, `StandardTimeManager` uses a simple division algorithm. We can extend this with chess-specific evaluations:
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
