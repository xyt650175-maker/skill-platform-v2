package com.company.llmaif.chat.service;

import com.company.llmaif.chat.service.vo.ChatRequestDTO;
import com.company.llmaif.common.AgentException;
import com.company.llmaif.config.LlmaifProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 启动真实 Deep Agents Python 运行时，并把运行事件转成平台 SSE。
 * Java 层只负责鉴权、配置传递和事件转发；Skill 的文件操作由 DeepAgent 完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepAgentRuntimeService {

    private final LlmaifProperties properties;
    private final ObjectMapper objectMapper;

    public SseEmitter streamSkillCreator(ChatRequestDTO dto) {
        LlmaifProperties.DeepAgent config = properties.getDeepAgent();
        LlmaifProperties.Llm llm = properties.getLlm();
        if (!config.isEnabled()) {
            throw new AgentException("DeepAgent 运行时未启用");
        }
        if (llm.isApiKeyRequired() && isBlank(llm.getApiKey())) {
            throw new AgentException("未配置模型访问令牌，DeepAgent 无法启动");
        }

        SseEmitter emitter = new SseEmitter(TimeUnit.SECONDS.toMillis(config.getTimeoutSeconds() + 15L));
        CompletableFuture.runAsync(() -> run(dto, emitter, config, llm));
        return emitter;
    }

    private void run(ChatRequestDTO dto, SseEmitter emitter, LlmaifProperties.DeepAgent config,
                     LlmaifProperties.Llm llm) {
        Process process = null;
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
        try {
            Path python = resolveExecutable(config.getPythonExecutable());
            Path script = resolvePath(config.getRuntimeScript());
            if (!Files.isExecutable(python)) {
                throw new AgentException("DeepAgent Python 环境不存在，请执行 deepagent-runtime/install.sh");
            }
            if (!Files.isRegularFile(script)) {
                throw new AgentException("DeepAgent 运行脚本不存在：" + script);
            }

            ProcessBuilder builder = new ProcessBuilder(python.toString(), script.toString());
            builder.directory(new File(System.getProperty("user.dir")));
            builder.environment().put("DEEPAGENT_API_KEY", defaultString(llm.getApiKey()));
            builder.environment().put("DEEPAGENT_BASE_URL", defaultString(llm.getBaseUrl()));
            builder.environment().put("DEEPAGENT_MODEL", defaultString(llm.getDefaultModel()));
            builder.environment().put("DEEPAGENT_MODEL_TIMEOUT", String.valueOf(llm.getTimeoutSeconds()));
            builder.environment().put("DEEPAGENT_RUNTIME_TIMEOUT",
                    String.valueOf(Math.max(30, config.getTimeoutSeconds() - 15)));
            process = builder.start();
            Process running = process;
            watchdog.schedule(() -> {
                if (running.isAlive()) {
                    log.warn("DeepAgent Skill Creator 超时，终止运行时进程");
                    running.destroyForcibly();
                }
            }, config.getTimeoutSeconds(), TimeUnit.SECONDS);

            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readLimited(running));
            try (OutputStream stdin = process.getOutputStream()) {
                objectMapper.writeValue(stdin, dto);
            }

            boolean sawError = false;
            boolean sawDone = false;
            try (BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = stdout.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    JsonNode event = objectMapper.readTree(line);
                    String type = event.path("type").asText("");
                    if ("error".equals(type)) sawError = true;
                    if ("done".equals(type)) sawDone = true;
                    emitter.send(SseEmitter.event().data(line));
                }
            }

            int exitCode = process.waitFor();
            String errorOutput = stderr.get(2, TimeUnit.SECONDS);
            if (exitCode != 0 && !sawError) {
                log.warn("DeepAgent 运行失败，exitCode={}, stderr={}", exitCode, sanitize(errorOutput));
                emit(emitter, "error", "DeepAgent Skill Creator 运行失败，请检查运行时日志");
            }
            if (!sawDone) emit(emitter, "done", "");
        } catch (Exception e) {
            log.warn("DeepAgent Skill Creator 调用失败: {}", sanitize(e.getMessage()));
            emit(emitter, "error", e instanceof AgentException
                    ? e.getMessage()
                    : "DeepAgent Skill Creator 调用失败，请检查运行时配置");
            emit(emitter, "done", "");
        } finally {
            watchdog.shutdownNow();
            if (process != null && process.isAlive()) process.destroyForcibly();
            emitter.complete();
        }
    }

    private Path resolveExecutable(String configured) {
        Path direct = Paths.get(configured);
        if (direct.isAbsolute() || configured.contains(File.separator)) {
            return direct.toAbsolutePath().normalize();
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String item : path.split(File.pathSeparator)) {
                Path candidate = Paths.get(item, configured);
                if (Files.isExecutable(candidate)) return candidate.toAbsolutePath().normalize();
            }
        }
        return direct.toAbsolutePath().normalize();
    }

    private Path resolvePath(String configured) {
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    private String readLimited(Process process) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && output.length() < 8000) {
                output.append(line).append('\n');
            }
        } catch (IOException ignored) {
            // 主流程会根据退出码给出稳定错误信息。
        }
        return output.toString();
    }

    private void emit(SseEmitter emitter, String type, String content) {
        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", type);
            event.put("content", content);
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
        } catch (Exception ignored) {
            // 浏览器断开后终止推送即可。
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String sanitize(String value) {
        if (value == null) return "";
        return value.replaceAll("sk-[A-Za-z0-9._-]+", "[redacted]");
    }
}
