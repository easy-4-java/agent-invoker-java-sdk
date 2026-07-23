package io.github.easy4j.agent.invoker.opencode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.agent.invoker.CallbackOutcome;
import io.github.easy4j.agent.invoker.RawCallbackPayload;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenCode 回调解析器。
 * <p>
 * 与 OpenClawCallbackParser 对称，但适配 OpenCode 的特殊场景：
 * OpenCode 没有 webhook/callback 机制，回调数据来自 AI 响应文本中嵌入的 JSON。
 * </p>
 * <p>
 * 解析策略：
 * <ol>
 *     <li>尝试从 {@code ```json ... ```} 代码块中提取 JSON</li>
 *     <li>尝试直接解析整个文本为 JSON</li>
 *     <li>尝试提取裸 JSON 对象（含 task_id/title 字段的）</li>
 * </ol>
 * </p>
 */
@Slf4j
public class OpenCodeCallbackParser {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```json\\s*\\n?(\\{.*?})\\s*\\n?```", Pattern.DOTALL);

    private static final Pattern BARE_JSON_PATTERN = Pattern.compile(
            "(\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*})", Pattern.DOTALL);

    /**
     * 从 RawCallbackPayload 解析（HTTP webhook 回调场景）。
     */
    public CallbackOutcome parse(RawCallbackPayload payload) {
        String rawBody = payload.getRawBody();
        if (rawBody == null || rawBody.isEmpty()) {
            return CallbackOutcome.builder()
                    .status(CallbackOutcome.CallbackStatus.FAILED)
                    .errorMessage("Empty callback body")
                    .build();
        }
        return parseFromText(rawBody);
    }

    /**
     * 从 AI 响应文本中解析回调 JSON。
     *
     * @param text AI 响应文本
     * @return 解析结果
     */
    public CallbackOutcome parseFromText(String text) {
        if (text == null || text.isEmpty()) {
            return CallbackOutcome.builder()
                    .status(CallbackOutcome.CallbackStatus.FAILED)
                    .errorMessage("Empty text")
                    .build();
        }

        Map<String, Object> parsed = extractJson(text);
        if (parsed == null) {
            return CallbackOutcome.builder()
                    .status(CallbackOutcome.CallbackStatus.FAILED)
                    .errorMessage("No parseable callback JSON found in response")
                    .build();
        }

        return mapToOutcome(parsed);
    }

    /**
     * 从文本中提取 JSON Map。
     */
    public Map<String, Object> extractJson(String text) {
        // 1. 尝试从 ```json ... ``` 代码块中提取
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            String json = matcher.group(1);
            try {
                return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.debug("Failed to parse JSON from code block: {}", json, e);
            }
        }

        // 2. 尝试直接解析整个文本为 JSON
        try {
            return MAPPER.readValue(text.trim(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
        }

        // 3. 尝试提取裸 JSON 对象
        Matcher bareMatcher = BARE_JSON_PATTERN.matcher(text);
        while (bareMatcher.find()) {
            String json = bareMatcher.group(1);
            try {
                Map<String, Object> parsed = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
                if (parsed.containsKey("task_id") || parsed.containsKey("taskId") || parsed.containsKey("title")) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private CallbackOutcome mapToOutcome(Map<String, Object> map) {
        String taskId = stringValue(map, "task_id", "taskId");
        String apiKey = stringValue(map, "api_key", "apiKey");

        return CallbackOutcome.builder()
                .taskId(taskId)
                .apiKey(apiKey)
                .status(CallbackOutcome.CallbackStatus.SUCCESS)
                .title(stringValue(map, "title"))
                .subtitle(stringValue(map, "subtitle"))
                .content(stringValue(map, "content"))
                .coverUrl(stringValue(map, "cover_url", "coverUrl"))
                .imageUrls(stringListValue(map, "image_urls", "imageUrls"))
                .tags(stringListValue(map, "tags"))
                .topics(stringListValue(map, "topics"))
                .sourceChannel(stringValue(map, "source_channel", "sourceChannel"))
                .outputType(stringValue(map, "output_type", "outputType"))
                .imagePrompt(stringValue(map, "image_prompt", "imagePrompt"))
                .extra(mapValue(map, "extra"))
                .build();
    }

    private static String stringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null) {
                return v.toString();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringListValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v instanceof List) {
                return (List<String>) v;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Map) {
            return (Map<String, Object>) v;
        }
        return null;
    }
}
