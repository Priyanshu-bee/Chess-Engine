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

# Patch Cute Chess dialog defaults if not already done
if [ -f "tools/cutechess-src/projects/gui/src/newgamedlg.cpp" ]; then
    if ! grep -q "ui->m_blackPlayerCpuRadio->setChecked" "tools/cutechess-src/projects/gui/src/newgamedlg.cpp"; then
        echo "Patching Cute Chess GUI defaults to Human vs Engine..."
        if command -v python3 &> /dev/null; then
            python3 -c "
import os
cpp_file = 'tools/cutechess-src/projects/gui/src/newgamedlg.cpp'
with open(cpp_file, 'r') as f:
    content = f.read()
target = 'ui->m_buttonBox->button(QDialogButtonBox::Ok)->setEnabled(ok);\n\t});\n}'
replacement = 'ui->m_buttonBox->button(QDialogButtonBox::Ok)->setEnabled(ok);\n\t});\n\n\tui->m_whitePlayerHumanRadio->setChecked(true);\n\tui->m_blackPlayerCpuRadio->setChecked(true);\n}'
if target in content:
    with open(cpp_file, 'w') as f:
        f.write(content.replace(target, replacement))
"
            # Force rebuild by deleting the binary
            rm -f tools/cutechess-src/build/cutechess
        fi
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
