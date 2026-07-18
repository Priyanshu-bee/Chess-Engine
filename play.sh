#!/bin/bash
# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
cd "$SCRIPT_DIR"

# Create bin directory if it doesn't exist
mkdir -p bin

# Check if Cute Chess submodule is initialized and compile if missing
if [ ! -f "tools/cutechess-src/CMakeLists.txt" ]; then
    echo "Submodule tools/cutechess-src is not initialized. Initializing..."
    git submodule update --init --recursive
    if [ $? -ne 0 ]; then
        echo "Failed to initialize Git submodules!"
        exit 1
    fi
fi

if [ ! -f "tools/cutechess-src/build/cutechess" ]; then
    echo "Cute Chess GUI executable not found. Compiling Cute Chess from source..."
    cmake -S tools/cutechess-src -B tools/cutechess-src/build
    cmake --build tools/cutechess-src/build -j$(sysctl -n hw.ncpu)
    if [ $? -ne 0 ]; then
        echo "Failed to build Cute Chess GUI!"
        exit 1
    fi
fi

# Recompile all Java source files into the bin directory
echo "Recompiling BitChess engine..."
/opt/homebrew/opt/openjdk/bin/javac -d bin src/core/*.java src/brain/*.java src/execution/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed! Cannot launch Cute Chess."
    exit 1
fi

echo "Compilation successful. Class files stored in 'bin/'."

exec ./tools/cutechess-src/build/cutechess "$@"
