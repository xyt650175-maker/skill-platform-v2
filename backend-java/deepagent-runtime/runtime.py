#!/usr/bin/env python3
"""Run the real Deep Agents SDK with the project Skill Creator.

The Java API starts this process for one generation request. The DeepAgent works
inside an isolated temporary filesystem and must create files through its
filesystem tools. Result files are emitted as newline-delimited JSON events.
"""

from __future__ import annotations

import json
import os
import re
import signal
import shutil
import sys
import tempfile
from pathlib import Path
from typing import Any

from deepagents import create_deep_agent
from deepagents.backends import FilesystemBackend
from langchain_openai import ChatOpenAI


ALLOWED_ROOT_FILES = {"SKILL.md", "requirements.txt"}
ALLOWED_DIRECTORIES = {"scripts", "references", "assets"}
RUNTIME_ROOT = Path(__file__).resolve().parent
SKILL_CREATOR_ROOT = RUNTIME_ROOT / "skills" / "skill-creator"

SYSTEM_PROMPT = """You are the Skill generation runtime for an enterprise platform.
You MUST use the available skill-creator skill before doing any work. Read its
SKILL.md, inspect /workspace, then create or update the requested Skill by using
filesystem write/edit tools. All deliverable files must live under /workspace.
Do not only print file contents in chat. Finish only after /workspace/SKILL.md
exists and you have re-read the files you changed. Preserve existing files that
the user did not ask to change.
"""


def timeout_handler(_signum: int, _frame: Any) -> None:
    raise TimeoutError("DeepAgent 运行超时")


def emit(event_type: str, content: str = "", **extra: Any) -> None:
    payload: dict[str, Any] = {"type": event_type, "content": content}
    payload.update(extra)
    sys.stdout.write(json.dumps(payload, ensure_ascii=False) + "\n")
    sys.stdout.flush()


def safe_relative_path(raw_path: str) -> Path | None:
    normalized = raw_path.replace("\\", "/")
    while normalized.startswith("./"):
        normalized = normalized[2:]
    path = Path(normalized)
    if not normalized or normalized.startswith("/") or path.is_absolute() or ".." in path.parts:
        return None
    if normalized in ALLOWED_ROOT_FILES:
        return path
    if path.parts and path.parts[0] in ALLOWED_DIRECTORIES:
        return path
    return None


def populate_workspace(workspace: Path, files: dict[str, Any]) -> None:
    for raw_path, raw_content in files.items():
        relative = safe_relative_path(str(raw_path))
        if relative is None:
            continue
        target = workspace / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(str(raw_content), encoding="utf-8")


def collect_workspace(workspace: Path) -> dict[str, str]:
    files: dict[str, str] = {}
    for path in sorted(workspace.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(workspace)
        if safe_relative_path(relative.as_posix()) is None:
            continue
        if path.stat().st_size > 2 * 1024 * 1024:
            raise ValueError(f"生成文件超过 2MB：{relative.as_posix()}")
        files[relative.as_posix()] = path.read_text(encoding="utf-8")
    return files


def total_usage(result: dict[str, Any]) -> dict[str, int]:
    prompt = completion = total = 0
    for message in result.get("messages", []):
        usage = getattr(message, "usage_metadata", None) or {}
        prompt += int(usage.get("input_tokens", 0) or 0)
        completion += int(usage.get("output_tokens", 0) or 0)
        total += int(usage.get("total_tokens", 0) or 0)
    return {"promptTokens": prompt, "completionTokens": completion, "totalTokens": total}


def tool_calls(result: dict[str, Any]) -> list[str]:
    names: list[str] = []
    for message in result.get("messages", []):
        for call in getattr(message, "tool_calls", None) or []:
            name = str(call.get("name", ""))
            if name:
                names.append(name)
    return names


def request_messages(payload: dict[str, Any]) -> list[dict[str, str]]:
    messages: list[dict[str, str]] = []
    for item in payload.get("messages", []):
        role = str(item.get("role", "user"))
        if role == "system":
            role = "user"
        if role not in {"user", "assistant"}:
            continue
        content = str(item.get("content", "")).strip()
        if content:
            messages.append({"role": role, "content": content})
    if not messages:
        raise ValueError("缺少 Skill 创建需求")
    skill_name = str(payload.get("skillName", "")).strip()
    skill_version = str(payload.get("skillVersion", "")).strip()
    target = f"目标 Skill 名称：{skill_name or '由需求确定'}；平台版本：{skill_version or '0.0.0'}。"
    first_user = next((item for item in messages if item["role"] == "user"), None)
    if first_user is not None:
        first_user["content"] = target + "\n\n" + first_user["content"]
    return messages


def run(payload: dict[str, Any]) -> None:
    api_key = os.environ.get("DEEPAGENT_API_KEY", "").strip()
    base_url = os.environ.get("DEEPAGENT_BASE_URL", "").strip()
    model_name = os.environ.get("DEEPAGENT_MODEL", "").strip()
    if not api_key or not base_url or not model_name:
        raise RuntimeError("DeepAgent 模型配置不完整")
    if not SKILL_CREATOR_ROOT.is_dir():
        raise RuntimeError("Skill Creator 未安装")

    with tempfile.TemporaryDirectory(prefix="skill-platform-deepagent-") as temp_dir:
        root = Path(temp_dir)
        workspace = root / "workspace"
        skills_root = root / "skills" / "skill-creator"
        workspace.mkdir(parents=True)
        shutil.copytree(SKILL_CREATOR_ROOT, skills_root)
        populate_workspace(workspace, payload.get("files") or {})

        emit("thinking", "DeepAgent 运行时已启动，正在加载 Skill Creator。")
        llm = ChatOpenAI(
            model=model_name,
            api_key=api_key,
            base_url=base_url,
            temperature=0.2,
            max_tokens=8192,
            timeout=float(os.environ.get("DEEPAGENT_MODEL_TIMEOUT", "180")),
            max_retries=2,
        )
        agent = create_deep_agent(
            model=llm,
            system_prompt=SYSTEM_PROMPT,
            backend=FilesystemBackend(root_dir=root, virtual_mode=True),
            skills=["/skills/"],
        )
        runtime_timeout = int(os.environ.get("DEEPAGENT_RUNTIME_TIMEOUT", "270"))
        signal.signal(signal.SIGALRM, timeout_handler)
        signal.alarm(runtime_timeout)
        result: dict[str, Any] | None = None
        announced_calls = 0
        try:
            for state in agent.stream(
                {"messages": request_messages(payload)},
                config={"recursion_limit": 30},
                stream_mode="values",
            ):
                result = state
                calls = tool_calls(state)
                if len(calls) > announced_calls:
                    latest = calls[announced_calls:]
                    emit("thinking", f"DeepAgent 正在执行工具：{', '.join(latest)}")
                    announced_calls = len(calls)
        finally:
            signal.alarm(0)
        if result is None:
            raise RuntimeError("DeepAgent 未返回运行结果")
        calls = tool_calls(result)
        if "read_file" not in calls or not ({"write_file", "edit_file"} & set(calls)):
            raise RuntimeError("DeepAgent 未按要求加载 Skill Creator 并执行文件工具")
        files = collect_workspace(workspace)
        if "SKILL.md" not in files:
            raise RuntimeError("DeepAgent 未在工作区生成 SKILL.md")

        emit("thinking", "Skill Creator 已加载，DeepAgent 文件工具执行完成。", toolCalls=calls)
        for path, content in files.items():
            emit("file", content, path=path)
        emit("content", f"DeepAgent 已通过 Skill Creator 生成 {len(files)} 个文件。")
        emit("usage", **total_usage(result))
        emit("done")


def main() -> int:
    try:
        payload = json.load(sys.stdin)
        run(payload)
        return 0
    except Exception as exc:
        message = re.sub(r"sk-[A-Za-z0-9._-]+", "[redacted]", str(exc))
        emit("error", f"DeepAgent Skill Creator 运行失败：{message}")
        emit("done")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
