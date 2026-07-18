# BitChess 👑
An efficient 64-bit Java chess engine compliant with the **UCI (Universal Chess Interface)** protocol.

* **Engine Name:** `BitChess`
* **Author:** `ArtificialMagic`

---

## 📋 Prerequisites

To run this project, you need the following dependencies installed on your system:
* **Java Development Kit (JDK 8 or higher)** to run and compile the Java engine.
* **CMake & Qt (5 or 6)** if you need to build the Cute Chess GUI from the git submodule.

On macOS, the `./play.sh` script will automatically check for these dependencies and offer to install them via **Homebrew** if they are missing. You can also install them manually:
```bash
brew install openjdk cmake qt
```

---

## 🚀 How to Launch and Play

To build the code and start playing against the engine immediately:

1. Open your terminal in this directory.
2. Run the master launcher script:
   ```bash
   ./play.sh
   ```

### What `play.sh` does automatically:
* **Checks Prerequisites:** Scans for Java, CMake, and Qt, and prompts to install them via Homebrew if any are missing.
* **Manages GUI Submodule:** Automatically clones the Cute Chess submodule and builds the graphical interface if it hasn't been built yet.
* **Configures Engine Profile:** Programmatically registers the `BitChess` profile inside Cute Chess's local settings (`~/.config/cutechess.com/engines.json`), dynamically matching your current repository paths.
* **Compiles Java Code:** Recompiles all engine packages from `src/` to `bin/`.
* **Starts the GUI:** Opens Cute Chess. You can immediately click **Game -> New** and select **BitChess** as your opponent!

---

## 🧠 Engine Highlights & Features

* **Bitboard Representation:** Uses 64-bit integers (`long`) to represent positions and perform rapid bitwise move calculations.
* **Alpha-Beta Search:** Leverages Minimax with Alpha-Beta pruning to discard unfavorable lines of play.
* **Quiescence Search:** Minimizes the horizon effect by searching all tactical capture sequences to a quiet state before scoring.
* **Position Evaluation:** Integrates material values with standard Piece-Square Tables (PST) to assess positional quality (e.g., center control, pawn advancement).
