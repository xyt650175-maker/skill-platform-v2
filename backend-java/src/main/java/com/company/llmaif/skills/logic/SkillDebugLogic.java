package com.company.llmaif.skills.logic;

import com.company.llmaif.common.AgentException;
import com.company.llmaif.skills.service.ISkillCodeService;
import com.company.llmaif.skills.service.vo.RunSkillDebugDTO;
import com.company.llmaif.skills.service.vo.SkillCodeVO;
import com.company.llmaif.skills.service.vo.SkillDebugVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 本机受限调试运行器。
 *
 * 它以临时目录、隔离模式 Python、最小环境变量和超时来降低误操作风险，
 * 但没有容器 / 网络隔离能力，因此不能作为不可信代码的安全沙箱。
 */
@Component
public class SkillDebugLogic {

    private static final long TIMEOUT_SECONDS = 3L;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;
    private static final String EXECUTION_MODE = "restricted-local (not container sandbox)";

    private final ISkillCodeService skillCodeService;
    private final ObjectMapper objectMapper;

    public SkillDebugLogic(ISkillCodeService skillCodeService, ObjectMapper objectMapper) {
        this.skillCodeService = skillCodeService;
        this.objectMapper = objectMapper;
    }

    public SkillDebugVO run(Long skillId, RunSkillDebugDTO dto) {
        long started = System.nanoTime();
        Path workDir = null;
        try {
            validateInput(dto.getInputJson());
            SkillCodeVO source = skillCodeService.getCode(skillId);
            if (source.getFiles() == null || source.getFiles().isEmpty()) {
                return failed(started, "Skill 尚无可运行的草稿文件", "未找到任何草稿文件。请先保存 SKILL.md 与 scripts/main.py。", null);
            }
            if (!source.getFiles().containsKey("scripts/main.py")) {
                return failed(started, "缺少 scripts/main.py", "调试入口必须是 scripts/main.py，并导出 handle(input_data) 函数。", null);
            }
            workDir = Files.createTempDirectory("llmaif-skill-debug-");
            writeSkillFiles(workDir, source.getFiles());
            Files.write(workDir.resolve("__runner__.py"), runnerScript().getBytes(StandardCharsets.UTF_8));

            ProcessBuilder builder = new ProcessBuilder("python3", "-I", "-S", "__runner__.py");
            builder.directory(workDir.toFile());
            builder.environment().clear();
            builder.environment().put("PATH", "/usr/bin:/bin");
            builder.environment().put("PYTHONIOENCODING", "utf-8");
            builder.redirectErrorStream(false);

            Process process = builder.start();
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(dto.getInputJson().getBytes(StandardCharsets.UTF_8));
            }
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
            }

            SkillDebugVO result = new SkillDebugVO();
            result.setExecutionMode(EXECUTION_MODE);
            result.setDurationMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            result.setDependencyCalls(0);
            result.setPromptTokens(0);
            result.setCompletionTokens(0);
            result.setTotalTokens(0);
            result.setStdout(readLimited(process.getInputStream()));
            result.setStderr(readLimited(process.getErrorStream()));

            if (!finished) {
                result.setStatus("FAILED");
                result.setErrorMessage("运行超时（限制 " + TIMEOUT_SECONDS + " 秒）");
                result.setOutput(result.getErrorMessage());
                return result;
            }
            result.setExitCode(process.exitValue());
            if (process.exitValue() != 0) {
                result.setStatus("FAILED");
                result.setErrorMessage("Skill 执行失败（退出码 " + process.exitValue() + "）");
                result.setOutput(result.getErrorMessage());
                return result;
            }
            result.setStatus("PASS");
            result.setOutput(result.getStdout());
            return result;
        } catch (AgentException e) {
            return failed(started, e.getMessage(), "调试准备失败：" + e.getMessage(), null);
        } catch (IOException e) {
            return failed(started, "启动本地调试运行器失败", e.getClass().getSimpleName() + ": " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failed(started, "本地调试运行被中断", e.getClass().getSimpleName() + ": " + e.getMessage(), null);
        } finally {
            deleteQuietly(workDir);
        }
    }

    private SkillDebugVO failed(long started, String errorMessage, String stderr, Integer exitCode) {
        SkillDebugVO result = new SkillDebugVO();
        result.setStatus("FAILED");
        result.setExecutionMode(EXECUTION_MODE);
        result.setDurationMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        result.setDependencyCalls(0);
        result.setPromptTokens(0);
        result.setCompletionTokens(0);
        result.setTotalTokens(0);
        result.setExitCode(exitCode);
        result.setErrorMessage(errorMessage);
        result.setOutput(errorMessage);
        result.setStdout("");
        result.setStderr(stderr == null ? "" : stderr);
        return result;
    }

    private void validateInput(String inputJson) {
        try {
            JsonNode node = objectMapper.readTree(inputJson);
            if (node == null || !node.isObject()) {
                throw new AgentException("调试输入必须是 JSON 对象");
            }
        } catch (IOException e) {
            throw new AgentException("调试输入不是合法 JSON：" + e.getMessage());
        }
    }

    private void writeSkillFiles(Path workDir, Map<String, String> files) throws IOException {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path relative = Paths.get(entry.getKey()).normalize();
            if (relative.isAbsolute() || relative.startsWith("..") || entry.getKey().contains(":")) {
                throw new AgentException("Skill 文件路径不合法：" + entry.getKey());
            }
            Path target = workDir.resolve(relative).normalize();
            if (!target.startsWith(workDir)) {
                throw new AgentException("Skill 文件路径不合法：" + entry.getKey());
            }
            Files.createDirectories(target.getParent());
            Files.write(target, entry.getValue().getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readLimited(java.io.InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int remaining = MAX_OUTPUT_BYTES;
        int read;
        while (remaining > 0 && (read = input.read(buffer, 0, Math.min(buffer.length, remaining))) != -1) {
            output.write(buffer, 0, read);
            remaining -= read;
        }
        String suffix = remaining == 0 ? "\n[输出已截断]" : "";
        return new String(output.toByteArray(), StandardCharsets.UTF_8) + suffix;
    }

    private void deleteQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try {
            List<Path> paths = new ArrayList<Path>();
            Files.walk(directory).forEach(paths::add);
            paths.sort(Comparator.reverseOrder());
            for (Path path : paths) Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 临时目录会由系统清理；不让清理失败掩盖执行结果。
        }
    }

    private String runnerScript() {
        return "import json\n"
                + "import runpy\n"
                + "import sys\n"
                + "# 仅将当前临时 Skill 目录加入模块查找路径，支持 scripts/ 下的职责拆分模块。\n"
                + "sys.path.insert(0, '.')\n"
                + "namespace = runpy.run_path('scripts/main.py')\n"
                + "handler = namespace.get('handle')\n"
                + "if not callable(handler):\n"
                + "    raise RuntimeError('scripts/main.py 必须导出 handle(input_data) 函数')\n"
                + "# 读取完整标准输入，支持用户在调试面板输入格式化的多行 JSON。\n"
                + "input_data = json.loads(sys.stdin.read() or '{}')\n"
                + "result = handler(input_data)\n"
                + "print(json.dumps({'result': result}, ensure_ascii=False, default=str))\n";
    }
}
