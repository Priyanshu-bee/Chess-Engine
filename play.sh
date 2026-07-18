#!/bin/bash
# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
cd "$SCRIPT_DIR"

# Automate dependency installation on macOS using Homebrew
if [[ "$OSTYPE" == "darwin"* ]]; then
    missing_deps=()
    if ! /usr/libexec/java_home &> /dev/null && [ ! -x "/opt/homebrew/opt/openjdk/bin/javac" ]; then
        missing_deps+=("Java-JDK")
    fi
    if ! command -v cmake &> /dev/null; then
        missing_deps+=("CMake")
    fi
    if ! qmake -v &> /dev/null && ! brew list qt &> /dev/null && ! brew list qt@6 &> /dev/null; then
        missing_deps+=("Qt")
    fi

    if [ ${#missing_deps[@]} -gt 0 ]; then
        if command -v brew &> /dev/null; then
            echo "The following dependencies are missing: ${missing_deps[*]}"
            read -p "Would you like to install them via Homebrew? (y/n): " confirm
            if [[ "$confirm" =~ ^[yY]$ ]]; then
                for dep in "${missing_deps[@]}"; do
                    if [ "$dep" == "Java-JDK" ]; then
                        echo "Installing Java JDK..."
                        brew install openjdk
                    elif [ "$dep" == "CMake" ]; then
                        echo "Installing CMake..."
                        brew install cmake
                    elif [ "$dep" == "Qt" ]; then
                        echo "Installing Qt..."
                        brew install qt
                    fi
                done
            else
                echo "Skipping dependency installation. Compilation may fail if requirements are missing."
            fi
        else
            echo "Warning: The following dependencies are missing: ${missing_deps[*]}. Homebrew is not installed, so they cannot be auto-installed. Please install them manually."
        fi
    fi
fi


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

# Run the setup script to patch GUI files and configure the engine profile
if command -v python3 &> /dev/null; then
    python3 tools/setup.py
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
# Find javac command dynamically
if [ -x "/opt/homebrew/opt/openjdk/bin/javac" ]; then
    JAVAC_CMD="/opt/homebrew/opt/openjdk/bin/javac"
elif /usr/libexec/java_home &> /dev/null; then
    JAVAC_CMD="$(/usr/libexec/java_home)/bin/javac"
else
    JAVAC_CMD="javac"
fi

$JAVAC_CMD -d bin src/core/*.java src/brain/*.java src/execution/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed! Cannot launch Cute Chess."
    exit 1
fi

echo "Compilation successful. Class files stored in 'bin/'."

exec ./tools/cutechess-src/build/cutechess "$@"
