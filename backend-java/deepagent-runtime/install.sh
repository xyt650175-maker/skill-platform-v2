#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BACKEND_DIR=$(dirname "$SCRIPT_DIR")
python3 -m venv "$BACKEND_DIR/.deepagent-venv"
"$BACKEND_DIR/.deepagent-venv/bin/python" -m pip install --upgrade pip
"$BACKEND_DIR/.deepagent-venv/bin/python" -m pip install -r "$SCRIPT_DIR/requirements.txt"
"$BACKEND_DIR/.deepagent-venv/bin/python" -c "import deepagents, langchain_openai; print('DeepAgent runtime ready')"
