package com.company.llmaif.chat.service.impl;

import com.company.llmaif.chat.service.ChatService;
import com.company.llmaif.chat.service.vo.ChatRequestDTO;
import com.company.llmaif.common.AgentException;
import com.company.llmaif.config.LlmaifProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** 通过 OpenAI 兼容协议代理模型流，前端统一消费平台 SSE 格式。 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final LlmaifProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public SseEmitter stream(ChatRequestDTO dto) {
        LlmaifProperties.Llm llm = properties.getLlm();
        if (llm.isApiKeyRequired() && (llm.getApiKey() == null || llm.getApiKey().trim().isEmpty())) {
            throw new AgentException("未配置模型访问令牌，无法调用模型");
        }

        SseEmitter emitter = new SseEmitter(TimeUnit.SECONDS.toMillis(llm.getTimeoutSeconds()));
        CompletableFuture.runAsync(() -> proxyStream(dto, llm, emitter));
        return emitter;
    }

    private void proxyStream(ChatRequestDTO dto, LlmaifProperties.Llm llm, SseEmitter emitter) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(llm.getTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
        try {
            String body = buildRequest(dto, llm.getDefaultModel());
            Request.Builder requestBuilder = new Request.Builder()
                    .url(llm.getBaseUrl().replaceAll("/+$", "") + "/chat/completions")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body, JSON));
            if (llm.getApiKey() != null && !llm.getApiKey().trim().isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + llm.getApiKey());
            }
            Request request = requestBuilder.build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    emit(emitter, "error", "模型服务暂时不可用（HTTP " + response.code() + "）");
                    emitter.complete();
                    return;
                }
                readStream(response.body(), emitter);
            }
        } catch (Exception e) {
            emit(emitter, "error", llm.isApiKeyRequired()
                    ? "调用模型服务失败，请检查网络和模型配置"
                    : "本地 Ollama 模型服务不可用，请确认服务已启动并已安装指定模型");
        } finally {
            emitter.complete();
            client.dispatcher().executorService().shutdown();
        }
    }

    private String buildRequest(ChatRequestDTO dto, String model) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.7);
        root.put("max_tokens", 4096);
        root.put("stream", true);
        root.putObject("stream_options").put("include_usage", true);
        ArrayNode messages = root.putArray("messages");
        for (ChatRequestDTO.MessageDTO message : dto.getMessages()) {
            messages.addObject().put("role", message.getRole()).put("content", message.getContent());
        }
        return objectMapper.writeValueAsString(root);
    }

    private void readStream(ResponseBody responseBody, SseEmitter emitter) throws IOException {
        try (BufferedReader reader = new BufferedReader(responseBody.charStream())) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) {
                    continue;
                }
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode chunk = objectMapper.readTree(data);
                JsonNode usage = chunk.path("usage");
                if (usage.isObject()) {
                    ObjectNode usageEvent = objectMapper.createObjectNode();
                    usageEvent.put("type", "usage");
                    usageEvent.put("promptTokens", usage.path("prompt_tokens").asInt(0));
                    usageEvent.put("completionTokens", usage.path("completion_tokens").asInt(0));
                    usageEvent.put("totalTokens", usage.path("total_tokens").asInt(0));
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(usageEvent)));
                }
                JsonNode choices = chunk.path("choices");
                if (!choices.isArray() || choices.size() == 0) {
                    continue;
                }
                JsonNode delta = choices.get(0).path("delta");
                String thinking = text(delta, "reasoning_content", "reasoning");
                String content = delta.path("content").asText("");
                if (!thinking.isEmpty()) {
                    emit(emitter, "thinking", thinking);
                }
                if (!content.isEmpty()) {
                    emit(emitter, "content", content);
                }
            }
        }
        emit(emitter, "done", "");
    }

    private String text(JsonNode node, String first, String second) {
        JsonNode value = node.get(first);
        if (value == null || value.isNull()) {
            value = node.get(second);
        }
        return value == null || value.isNull() ? "" : value.asText();
    }

    private void emit(SseEmitter emitter, String type, String content) {
        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", type);
            event.put("content", content);
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
        } catch (Exception ignored) {
            // 客户端断开后无需继续推送。
        }
    }
}
