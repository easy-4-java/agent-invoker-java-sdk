package io.github.hiwepy.agent.invoker.hermes;

<<<<<<< HEAD
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.agent.invoker.CallbackOutcome;
import io.github.hiwepy.hermes.model.RunStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Hermes 回调解析器。将 Hermes RunStatus 输出解析为业务语义的 {@link CallbackOutcome}。
 *
 * <p>使用三策略 JSON 解析（HermesJsonParser）：从输出文本中提取第一个 JSON 对象。</p>
 */
@Slf4j
public class HermesCallbackParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 从 Hermes RunStatus 解析 CallbackOutcome。
     *
     * @param status Hermes Run 状态
     * @param taskId 业务任务 ID
     * @return 解析后的 CallbackOutcome
     */
    public CallbackOutcome parseFromRunStatus(RunStatus status, String taskId) {
        if (status == null) {
            return CallbackOutcome.builder()
                    .taskId(taskId)
                    .status(CallbackOutcome.CallbackStatus.FAILED)
                    .errorMessage("RunStatus is null")
=======
import io.github.hiwepy.agent.invoker.CallbackOutcome;
import io.github.hiwepy.hermes.model.RunStatus;
import io.github.hiwepy.hermes.util.HermesJsonParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class HermesCallbackParser {

    private final HermesJsonParser jsonParser = new HermesJsonParser();

    public CallbackOutcome parseFromRunStatus(RunStatus status, String taskId) {
        if (status == null || status.getOutput() == null) {
            return CallbackOutcome.builder()
                    .taskId(taskId)
                    .status(CallbackOutcome.CallbackStatus.FAILED)
                    .errorMessage("Empty response from Hermes")
>>>>>>> c15e88a (feat: add hermes-agent adapter)
                    .build();
        }

        String output = status.getOutput();
<<<<<<< HEAD
        if (output == null || output.isEmpty()) {
            return CallbackOutcome.builder()
                    .taskId(taskId)
                    .status(CallbackOutcome.CallbackStatus.FAILED)
                    .errorMessage("RunStatus output is empty")
                    .build();
        }

        return parseFromText(output, taskId);
    }

    /**
     * 从文本输出解析 CallbackOutcome。
     *
     * <p>尝试三策略 JSON 解析：先整体解析，再逐行查找 JSON 对象，最后查找花括号边界。</p>
     *
     * @param text   输出文本
     * @param taskId 业务任务 ID
     * @return 解析后的 CallbackOutcome
     */
    public CallbackOutcome parseFromText(String text, String taskId) {
        if (text == null || text.isEmpty()) {
            return CallbackOutcome.builder()
                    .taskId(taskId)
                    .status(CallbackOutcome.CallbackStatus.FAILED)
                    .errorMessage("Output text is empty")
                    .build();
        }

        // Strategy 1: Try to parse the entire text as JSON
        Map<String, Object> parsed = tryParseJson(text);
        if (parsed != null) {
            return buildOutcome(parsed, taskId);
        }

        // Strategy 2: Scan line by line for a JSON object
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("{")) {
                parsed = tryParseJson(trimmed);
                if (parsed != null) {
                    return buildOutcome(parsed, taskId);
                }
            }
        }

        // Strategy 3: Find content between first { and last }
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            String candidate = text.substring(firstBrace, lastBrace + 1);
            parsed = tryParseJson(candidate);
            if (parsed != null) {
                return buildOutcome(parsed, taskId);
            }
        }

        // No JSON found — use raw text as content
        return CallbackOutcome.builder()
                .taskId(taskId)
                .status(CallbackOutcome.CallbackStatus.SUCCESS)
                .content(text.trim())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tryParseJson(String text) {
        try {
            Object obj = MAPPER.readValue(text, Object.class);
            if (obj instanceof Map) {
                return (Map<String, Object>) obj;
            }
        } catch (JsonProcessingException ignored) {
            // not valid JSON
        }
        return null;
    }

    private CallbackOutcome buildOutcome(Map<String, Object> map, String taskId) {
        CallbackOutcome.Builder builder = CallbackOutcome.builder()
                .taskId(taskId)
                .status(CallbackOutcome.CallbackStatus.SUCCESS);

        builder.title(stringValue(map, "title"));
        builder.subtitle(stringValue(map, "subtitle"));
        builder.content(stringValue(map, "content"));
        builder.coverUrl(stringValue(map, "cover_url"));
        builder.imageUrls(stringListValue(map, "image_urls"));
        builder.tags(stringListValue(map, "tags"));
        builder.topics(stringListValue(map, "topics"));
        builder.sourceChannel(stringValue(map, "source_channel"));
        builder.outputType(stringValue(map, "output_type"));
        builder.imagePrompt(stringValue(map, "image_prompt"));
        builder.apiKey(stringValue(map, "api_key"));
        builder.extra(mapValue(map, "extra"));

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringListValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof List) {
            List<?> rawList = (List<?>) v;
            List<String> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Map) return (Map<String, Object>) v;
        return null;
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
=======
        Map<String, Object> parsed = jsonParser.parseFromText(output);

        if (parsed != null) {
            CallbackOutcome.Builder builder = CallbackOutcome.builder()
                    .taskId(taskId)
                    .status(CallbackOutcome.CallbackStatus.SUCCESS);

            if (parsed.containsKey("title")) builder.title(parsed.get("title").toString());
            if (parsed.containsKey("subtitle")) builder.subtitle(parsed.get("subtitle").toString());
            if (parsed.containsKey("content")) builder.content(parsed.get("content").toString());
            if (parsed.containsKey("cover_url")) builder.coverUrl(parsed.get("cover_url").toString());
            if (parsed.containsKey("coverUrl")) builder.coverUrl(parsed.get("coverUrl").toString());
            if (parsed.containsKey("output_type")) builder.outputType(parsed.get("output_type").toString());
            if (parsed.containsKey("outputType")) builder.outputType(parsed.get("outputType").toString());
            if (parsed.containsKey("image_prompt")) builder.imagePrompt(parsed.get("image_prompt").toString());
            if (parsed.containsKey("imagePrompt")) builder.imagePrompt(parsed.get("imagePrompt").toString());
            if (parsed.containsKey("error_message")) builder.errorMessage(parsed.get("error_message").toString());

            return builder.build();
        }

        // No JSON found, use raw output as content
        return CallbackOutcome.builder()
                .taskId(taskId)
                .status(CallbackOutcome.CallbackStatus.SUCCESS)
                .content(output)
                .build();
    }
>>>>>>> c15e88a (feat: add hermes-agent adapter)
}
