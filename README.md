# BitChess 👑
An efficient 64-bit Java chess engine compliant with the **UCI (Universal Chess Interface)** protocol.

* **Engine Name:** `BitChess`
* **Author:** `ArtificialMagic`

---

## 📋 Prerequisites

To run this project, you need the following dependencies installed:

1. **Java Development Kit (JDK 8 or higher):** Required to compile and run the Java engine.
2. **CMake & Qt (5 or 6):** Required only if you need to build the Cute Chess GUI from the submodule.

On macOS, you can install all prerequisites using **Homebrew**:
```bash
brew install openjdk cmake qt
```

---

## 🚀 How to Launch and Play

To compile any code changes and play against the engine immediately in the GUI:

1. Open your terminal in this directory.
2. Run the play script:
   ```bash
   ./play.sh
   ```
This script will automatically:
* Recompile the latest Java files and store the compiled binaries cleanly in the `bin/` directory.
* Launch the local compiled **Cute Chess** GUI.

---

## ⚙️ Cute Chess GUI Configuration

If you need to configure or verify the engine settings inside the Cute Chess GUI:

1. Close any open matches to unlock settings (**Game** -> **Close**).
2. Go to **Tools** -> **Settings** -> **Engines**.
3. If `BitChess` is not configured, select **Add...** (or edit the existing one) with these parameters:
   * **Name:** `BitChess`
   * **Protocol:** `UCI`
   * **Command:** `/Users/priyanshu/CSE/Chess-Engine/bin/MyBot`
   * **Working Directory:** `/Users/priyanshu/CSE/Chess-Engine`
4. Click **OK** to save, then select **Game** -> **New** to start playing!

---

## 🧠 Engine Highlights & Features

* **Bitboard Representation:** Uses 64-bit integers (`long`) to represent positions and perform rapid bitwise move calculations.
* **Alpha-Beta Search:** Leverages Minimax with Alpha-Beta pruning to discard unfavorable lines of play.
* **Quiescence Search:** Minimizes the horizon effect by searching all tactical capture sequences to a quiet state before scoring.
* **Position Evaluation:** Integrates material values with standard Piece-Square Tables (PST) to assess positional quality (e.g., center control, pawn advancement).
