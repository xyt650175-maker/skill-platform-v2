---
name: skill-creator
description: Create or update an Agent Skill when the user asks to build, generate, scaffold, or revise a Skill and its supporting resources.
---

# Skill Creator

Create a focused, usable Skill in the provided workspace. Preserve the user's intent and make the smallest complete change.

## Required workflow

1. Inspect the existing files before editing an existing Skill.
2. Decide which files are actually required.
3. Create or update files with the filesystem tools; do not merely describe proposed contents.
4. Re-read the resulting `SKILL.md` and any executable entrypoint before finishing.

## Structure

`SKILL.md` is required. Its YAML frontmatter must contain the platform fields `name`, `name_zh`, `description`, `version`, `tags`, `runEnv`, and `digestValue`. Use `pending` for `digestValue`; the platform replaces it when saving.

Use optional resources only when they materially help:

- `scripts/` for deterministic or repeatedly reused execution logic. When the Skill needs platform debugging, provide `scripts/main.py` with `handle(input_data)`. The platform always passes `input_data` as a Python `dict`; access fields with `input_data.get(...)` or dictionary indexing and return a JSON-serializable `dict`.
- `references/` for detailed schemas, rules, or guidance loaded only when needed.
- `assets/` for resources copied into an output.
- `requirements.txt` only when Python code has third-party dependencies.

Do not create `README.md`, standalone YAML files, empty directories, placeholders, changelogs, installation guides, or unrelated examples. Keep `SKILL.md` concise and under 500 lines. Use a lowercase letters/numbers/hyphens name shorter than 64 characters.
