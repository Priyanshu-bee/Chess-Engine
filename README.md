# BitChess 👑
An efficient 64-bit Java chess engine compliant with the **UCI (Universal Chess Interface)** protocol.

* **Engine Name:** `BitChess`
* **Author:** `ArtificialMagic`

---

## 🚀 Quick Start: How to Play

Open your terminal in this directory and run the launcher script:
```bash
./play.sh
```
This is all you need to do! The script will compile your files and open the Cute Chess GUI immediately. Select **Game -> New** and pick **BitChess** to start playing.

---

## 🧠 Engine Highlights & Features

* **Bitboard Representation:** Uses 64-bit integers (`long`) to represent positions and perform rapid bitwise move calculations.
* **Alpha-Beta Search:** Leverages Minimax with Alpha-Beta pruning to discard unfavorable lines of play.
* **Quiescence Search:** Minimizes the horizon effect by searching all tactical capture sequences to a quiet state before scoring.
* **Position Evaluation:** Integrates material values with standard Piece-Square Tables (PST) to assess positional quality (e.g., center control, pawn advancement).

---

## 📋 Installation Details & Prerequisites

### What `play.sh` does automatically in the background:
* **Dependency Scan:** Checks if Java JDK, CMake, or Qt are missing from your macOS system, and prompts you to install them automatically using **Homebrew**.
* **GUI Submodule Setup:** Automatically clones the Cute Chess GUI Git submodule and compiles it from C++ source code if it hasn't been built yet.
* **Engine Profile Registration:** Programmatically registers the `BitChess` engine configuration profile inside Cute Chess's local settings (`~/.config/cutechess.com/engines.json`), dynamically matching your local directory paths.
* **Java Compilation:** Recompiles all Java engine packages from the `src/` directory directly into the `/bin` directory.

### Manual Dependency Installation
If you prefer to install the system dependencies manually (on macOS via Homebrew):
```bash
brew install openjdk cmake qt
```
* **Java JDK 8 (or higher)** is required to compile and execute the chess engine.
* **CMake & Qt (5 or 6)** are required only to build the Cute Chess GUI from the git submodule.
