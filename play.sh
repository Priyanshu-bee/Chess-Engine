#!/bin/bash
# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
cd "$SCRIPT_DIR"

# Automate dependency installation on macOS using Homebrew
if [[ "$OSTYPE" == "darwin"* ]]; then
    if command -v brew &> /dev/null; then
        if ! command -v java &> /dev/null; then
            echo "Java JDK not found. Installing via Homebrew..."
            brew install openjdk
        fi
        if ! command -v cmake &> /dev/null; then
            echo "CMake not found. Installing via Homebrew..."
            brew install cmake
        fi
        if ! brew list qt &> /dev/null && ! brew list qt@6 &> /dev/null; then
            echo "Qt libraries not found. Installing via Homebrew..."
            brew install qt
        fi
    else
        echo "Warning: Homebrew is not installed. Please manually install Java, CMake, and Qt."
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

# Auto-configure Cute Chess engine configuration profile dynamically
if command -v python3 &> /dev/null; then
    echo "Auto-configuring BitChess engine in Cute Chess..."
    python3 -c "
import json, os
config_dir = os.path.expanduser('~/.config/cutechess.com')
os.makedirs(config_dir, exist_ok=True)
engines_file = os.path.join(config_dir, 'engines.json')
new_engine = {
    'command': os.path.join(os.getcwd(), 'bin/MyBot'),
    'name': 'BitChess',
    'protocol': 'uci',
    'stderrFile': '',
    'timeoutScaleFactor': 1,
    'workingDirectory': os.getcwd()
}
engines = []
if os.path.exists(engines_file):
    try:
        with open(engines_file, 'r') as f:
            content = f.read().strip()
            if content: engines = json.loads(content)
    except Exception as e: pass
exists = False
for engine in engines:
    if engine.get('name') == 'BitChess':
        engine['command'] = new_engine['command']
        engine['workingDirectory'] = new_engine['workingDirectory']
        exists = True
        break
if not exists:
    engines.append(new_engine)
with open(engines_file, 'w') as f:
    json.dump(engines, f, indent=4)
"
fi

exec ./tools/cutechess-src/build/cutechess "$@"
