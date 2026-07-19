#!/usr/bin/env python3
import os
import json

def configure_engine():
    """Auto-configures the BitChess engine profile inside Cute Chess settings."""
    print("Auto-configuring BitChess engine in Cute Chess...")
    config_dir = os.path.expanduser('~/.config/cutechess.com')
    os.makedirs(config_dir, exist_ok=True)
    engines_file = os.path.join(config_dir, 'engines.json')

    new_engine = {
        'command': os.path.join(os.getcwd(), 'bin/MyBot'),
        'name': 'BitChess',
        'protocol': 'uci',
        'stderrFile': '',
        'timeoutScaleFactor': 1,
        'workingDirectory': os.getcwd(),
        'ponder': True,
        'whitepov': True
    }

    engines = []
    if os.path.exists(engines_file):
        try:
            with open(engines_file, 'r') as f:
                content = f.read().strip()
                if content:
                    engines = json.loads(content)
        except Exception as e:
            print(f"Warning: Failed to parse engines.json: {e}")

    exists = False
    for engine in engines:
        if engine.get('name') == 'BitChess':
            engine['command'] = new_engine['command']
            engine['workingDirectory'] = new_engine['workingDirectory']
            engine['ponder'] = True
            engine['whitepov'] = True
            exists = True
            break

    if not exists:
        engines.append(new_engine)

    try:
        with open(engines_file, 'w') as f:
            json.dump(engines, f, indent=4)
    except Exception as e:
        print(f"Error: Failed to write to engines.json: {e}")

def patch_cutechess_gui():
    """Patches Cute Chess GUI defaults and formatting."""
    cpp_file_1 = 'tools/cutechess-src/projects/gui/src/newgamedlg.cpp'
    cpp_file_2 = 'tools/cutechess-src/projects/gui/src/cutechessapp.cpp'
    cpp_file_3 = 'tools/cutechess-src/projects/lib/src/moveevaluation.cpp'

    if not os.path.exists(cpp_file_1) or not os.path.exists(cpp_file_2):
        return

    needs_patch_1 = False
    needs_patch_2 = False
    needs_patch_3 = False

    # 1. Inspect newgamedlg.cpp
    with open(cpp_file_1, 'r') as f:
        content_1 = f.read()
    if 'ui->m_blackPlayerCpuRadio->setChecked' not in content_1:
        needs_patch_1 = True

    # 2. Inspect cutechessapp.cpp
    with open(cpp_file_2, 'r') as f:
        content_2 = f.read()
    if 'BitChess' not in content_2:
        needs_patch_2 = True

    # 3. Inspect moveevaluation.cpp
    content_3 = ""
    if os.path.exists(cpp_file_3):
        with open(cpp_file_3, 'r') as f:
            content_3 = f.read()
        if 'str += "M" + QString::number((absScore + 1) / 2);' not in content_3:
            needs_patch_3 = True

    if not needs_patch_1 and not needs_patch_2 and not needs_patch_3:
        return

    print("Patching Cute Chess GUI defaults (New Game & Startup Game & Score Format)...")

    # Perform Patch 1
    if needs_patch_1:
        target = 'ui->m_buttonBox->button(QDialogButtonBox::Ok)->setEnabled(ok);\n\t});\n}'
        replacement = 'ui->m_buttonBox->button(QDialogButtonBox::Ok)->setEnabled(ok);\n\t});\n\n\tui->m_whitePlayerHumanRadio->setChecked(true);\n\tui->m_blackPlayerCpuRadio->setChecked(true);\n}'
        if target in content_1:
            with open(cpp_file_1, 'w') as f:
                f.write(content_1.replace(target, replacement))

    # Perform Patch 2
    if needs_patch_2:
        target_inc = '#include <humanbuilder.h>'
        replacement_inc = '#include <humanbuilder.h>\n#include <enginebuilder.h>'
        if target_inc in content_2 and '#include <enginebuilder.h>' not in content_2:
            content_2 = content_2.replace(target_inc, replacement_inc)

        replacement_func = """void CuteChessApplication::newDefaultGame()
{
	// default game is a human versus human game using standard variant and
	// infinite time control
	ChessGame* game = new ChessGame(Chess::BoardFactory::create("standard"),
		new PgnGame());

	QSettings s;
	s.beginGroup("games");
	s.setValue("pondering", true);

	// Load White time control (default to 40 moves in 5 minutes)
	TimeControl whiteTc;
	whiteTc.setMovesPerTc(40);
	whiteTc.setTimePerTc(300000);
	whiteTc.readSettings(&s);
	game->setTimeControl(whiteTc, Chess::Side::White);

	// Load Black time control (default to 40 moves in 5 minutes)
	TimeControl blackTc;
	blackTc.setMovesPerTc(40);
	blackTc.setTimePerTc(300000);
	s.beginGroup("second_time_control");
	blackTc.readSettings(&s);
	s.endGroup(); // "second_time_control"
	game->setTimeControl(blackTc, Chess::Side::Black);

	s.endGroup(); // "games"

	game->pause();

	connect(game, SIGNAL(started(ChessGame*)),
		this, SLOT(newGameWindow(ChessGame*)));

	PlayerBuilder* whiteBuilder = new HumanBuilder(userName());
	PlayerBuilder* blackBuilder = nullptr;

	EngineManager* manager = engineManager();
	if (manager && manager->engineCount() > 0)
	{
		EngineConfiguration config;
		bool found = false;
		for (int i = 0; i < manager->engineCount(); ++i)
		{
			if (manager->engineAt(i).name() == "BitChess")
			{
				config = manager->engineAt(i);
				found = true;
				break;
			}
		}
		if (!found)
			config = manager->engineAt(0);

		config.setPondering(true);
		blackBuilder = new EngineBuilder(config);
	}
	else
	{
		blackBuilder = new HumanBuilder(userName());
	}

	gameManager()->newGame(game, whiteBuilder, blackBuilder);
}"""
        old_func_start = 'void CuteChessApplication::newDefaultGame()'
        old_func_end = 'new HumanBuilder(userName()));\n}'
        start_idx = content_2.find(old_func_start)
        if start_idx != -1:
            end_idx = content_2.find(old_func_end, start_idx)
            if end_idx != -1:
                end_idx += len(old_func_end)
                content_2 = content_2[:start_idx] + replacement_func + content_2[end_idx:]

        with open(cpp_file_2, 'w') as f:
            f.write(content_2)

    # Perform Patch 3
    if needs_patch_3 and content_3:
        target_eval = 'str += "M" + QString::number(absScore);'
        replacement_eval = 'str += "M" + QString::number((absScore + 1) / 2);'
        if target_eval in content_3:
            with open(cpp_file_3, 'w') as f:
                f.write(content_3.replace(target_eval, replacement_eval))

    # Force rebuild by deleting the binary
    binary_path = 'tools/cutechess-src/build/cutechess'
    if os.path.exists(binary_path):
        os.remove(binary_path)

if __name__ == '__main__':
    # Patch GUI files (compilation triggers)
    patch_cutechess_gui()
    # Configure engines.json profile
    configure_engine()
