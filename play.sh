#!/bin/bash
# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
cd "$SCRIPT_DIR"

# Create bin directory if it doesn't exist
mkdir -p bin

# Recompile all Java source files into the bin directory
echo "Recompiling BitChess engine..."
/opt/homebrew/opt/openjdk/bin/javac -d bin src/core/*.java src/brain/*.java src/execution/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed! Cannot launch Cute Chess."
    exit 1
fi

echo "Compilation successful. Class files stored in 'bin/'."

exec ./tools/cutechess-src/build/cutechess "$@"
