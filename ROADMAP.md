# BitChess Future Enhancements Roadmap 🗺️

Here is a detailed roadmap of the next logical improvements for **BitChess** to make it significantly stronger, more responsive, and compliant with tournament conditions.

---

## ⏳ 1. UCI Time Management

Currently, the engine ignores the time parameters sent by the GUI in the `go` command and searches to a fixed depth. In tournament play, it must allocate its thinking time dynamically.

### How it works:
When the GUI sends a command like:
```
go wtime 300000 btime 280000 winc 2000 binc 2000 movestogo 40
```
The engine parses:
*   `wtime` / `btime`: Remaining time in milliseconds.
*   `winc` / `binc`: Time increment per move in milliseconds.
*   `movestogo`: Number of moves remaining until the next time control (if absent, we estimate $\approx 40$ moves).

### Proposed Time Allocation Formula:
We calculate the target duration for the current move ($T_{\text{target}}$):
$$T_{\text{target}} = \frac{T_{\text{remaining}}}{\min(\text{movestogo}, 40)} + T_{\text{increment}} - \text{margin}$$
*   *Margin:* We keep a small buffer (e.g., $100\text{ ms}$) to account for network transmission and engine overhead.
*   *Iterative Deepening Check:* During iterative deepening (depth 1, 2, 3...), we check the elapsed time after each depth is fully searched. If the elapsed time exceeds $50\%$ of $T_{\text{target}}$, we stop and return the best move found so far rather than starting another deep search layer that we won't finish in time.

---

## 🤖 2. Pondering (Thinking on Opponent's Time)

Pondering allows the engine to keep searching while the opponent is thinking, dramatically increasing its effective depth on the next move.

### How it works:
1.  **Ponder Move Selection:** When the engine completes a search, it returns the best move (`bestmove`) and the move it *expects* the opponent to play (the `ponder` move from the Principal Variation).
2.  **Ponder Search:** The GUI sends `go ponder`. The engine makes the expected opponent move on its internal board and starts a background search.
3.  **Ponder Hit:** If the opponent plays the expected move, the GUI sends `ponderhit`. The engine immediately stops the ponder search, returns the best move, and switches to its own turn.
4.  **Ponder Miss:** If the opponent plays a different move, the GUI sends `stop`. The engine discards the search, resets the board to the actual position, and awaits a standard `go` command.

---

## 🧠 3. Improving the Brain (Search & Evaluation)

To increase search depth and tactical awareness, we can implement several standard chess engine optimizations:

### A. Move Ordering (Immediate 5x–10x Speedup)
Alpha-Beta pruning is only effective if we search the best moves first.
*   **MVV-LVA (Most Valuable Victim - Least Valuable Aggressor):** Sort captures so that attacking a Queen with a Pawn is searched before attacking a Pawn with a Queen.
*   **Killer Heuristic:** Prioritize quiet moves that caused beta cutoffs in other branches at the same ply.
*   **History Heuristic:** Score moves based on how often they caused cutoffs across the entire tree.

### B. Transposition Tables (TT)
*   **Zobrist Hashing:** Assign a unique 64-bit random integer to each piece-square combination. Dynamically update the hash on every move.
*   **Hash Table:** Store searched positions, their depths, flags (exact, lower bound, upper bound), and evaluation scores. If we encounter the same position via a different move order, we retrieve the score instantly without re-searching.

### C. Advanced Evaluation
*   **Mobility:** Score based on the number of legal moves available to knights, bishops, rooks, and queens (more active pieces are valued higher).
*   **King Safety:** Penalize open pawn shelters in front of the castled king.
*   **Pawn Structure:** Score passed pawns, doubled pawns, and isolated pawns.
