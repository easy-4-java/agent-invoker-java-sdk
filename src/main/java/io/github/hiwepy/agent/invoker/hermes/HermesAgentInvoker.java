package io.github.hiwepy.agent.invoker.hermes;

import io.github.hiwepy.agent.invoker.AgentInvokeCmd;
import io.github.hiwepy.agent.invoker.AgentInvoker;
import io.github.hiwepy.agent.invoker.CallbackOutcome;
import io.github.hiwepy.agent.invoker.RawCallbackPayload;
import io.github.hiwepy.agent.invoker.SubmitResult;
import io.github.hiwepy.hermes.HermesClient;
import io.github.hiwepy.hermes.model.RunCreateRequest;
import io.github.hiwepy.hermes.model.RunStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
<<<<<<< HEAD
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Hermes 适配器：实现 {@link AgentInvoker}，将业务语义翻译为 Hermes Run 协议。
 *
 * <p>依赖 {@code hermes-java-sdk}（optional），仅在类路径存在 HermesClient 时可用。</p>
 *
 * @author wandl
 * @since 1.0.0
 */
=======
import java.util.concurrent.*;

>>>>>>> c15e88a (feat: add hermes-agent adapter)
@Slf4j
public class HermesAgentInvoker implements AgentInvoker {

    public static final String PROVIDER_CODE = "hermes";

    private final HermesClient hermesClient;
    private final HermesCallbackParser callbackParser;
    private final String callbackBaseUrl;
<<<<<<< HEAD
    private final String defaultInstructions;
    private final ConcurrentMap<String, String> taskIdToRunId = new ConcurrentHashMap<String, String>();

    /**
     * @param hermesClient       Hermes SDK 客户端
     * @param callbackBaseUrl    adapter 级 callback 基础 URL
     * @param defaultInstructions 默认 instructions（可被 cmd.variables["hermes.instructions"] 覆盖）
     */
    public HermesAgentInvoker(HermesClient hermesClient,
                              String callbackBaseUrl,
                              String defaultInstructions) {
        this.hermesClient = Objects.requireNonNull(hermesClient, "hermesClient");
        this.callbackBaseUrl = callbackBaseUrl != null ? callbackBaseUrl : "http://localhost:7088";
        this.defaultInstructions = defaultInstructions;
=======
    private final ConcurrentMap<String, String> taskIdToRunId = new ConcurrentHashMap<>();
    private final ExecutorService pollingExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "hermes-poll");
        t.setDaemon(true);
        return t;
    });

    public HermesAgentInvoker(HermesClient hermesClient, String callbackBaseUrl) {
        this.hermesClient = Objects.requireNonNull(hermesClient, "hermesClient");
        this.callbackBaseUrl = callbackBaseUrl != null ? callbackBaseUrl : "http://localhost:7088";
>>>>>>> c15e88a (feat: add hermes-agent adapter)
        this.callbackParser = new HermesCallbackParser();
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public SubmitResult submit(AgentInvokeCmd cmd) {
        String taskId = cmd.getTaskId();
        String enhancedPrompt = cmd.getEnhancedPrompt();
        if (enhancedPrompt == null || enhancedPrompt.isEmpty()) {
            return SubmitResult.builder()
                    .taskId(taskId)
                    .status(SubmitResult.InvokeStatus.REJECTED)
<<<<<<< HEAD
                    .message("enhancedPrompt is required for Hermes")
=======
                    .message("enhancedPrompt is required for hermes")
>>>>>>> c15e88a (feat: add hermes-agent adapter)
                    .build();
        }

        try {
<<<<<<< HEAD
            RunCreateRequest request = HermesRequestMapper.toRunCreateRequest(cmd, defaultInstructions);
            log.debug("Hermes createRun, taskId={}, model={}", taskId, request.getModel());

            RunStatus runStatus = hermesClient.createRun(request);
            if (runStatus == null || runStatus.getRunId() == null) {
                log.warn("Hermes createRun failed, taskId={}", taskId);
=======
            RunCreateRequest request = HermesRequestMapper.toRunCreateRequest(cmd,
                    hermesClient.getConfig().getDefaultInstructions());
            RunStatus result = hermesClient.createRun(request);
            if (result == null || result.getRunId() == null) {
                log.warn("Hermes invoke failed, taskId={}", taskId);
>>>>>>> c15e88a (feat: add hermes-agent adapter)
                return SubmitResult.builder()
                        .taskId(taskId)
                        .status(SubmitResult.InvokeStatus.REJECTED)
                        .message("Hermes createRun returned null")
                        .build();
            }

<<<<<<< HEAD
            String runId = runStatus.getRunId();
            if (taskId != null) {
                taskIdToRunId.put(taskId, runId);
            }

            String callbackUrl = cmd.getCallbackUrl() != null ? cmd.getCallbackUrl() : callbackBaseUrl;
            log.info("Hermes createRun success, taskId={}, runId={}, callbackUrl={}", taskId, runId, callbackUrl);

            // Start async polling
            CompletableFuture.runAsync(() -> pollUntilComplete(runId, taskId, callbackUrl));
=======
            String runId = result.getRunId();
            taskIdToRunId.put(taskId, runId);
            log.info("Hermes invoke success, taskId={}, runId={}", taskId, runId);

            // Async polling for completion
            pollingExecutor.submit(() -> pollUntilComplete(taskId, runId, cmd.getCallbackUrl(), cmd.getCallbackToken()));
>>>>>>> c15e88a (feat: add hermes-agent adapter)

            return SubmitResult.builder()
                    .taskId(taskId)
                    .providerTaskId(runId)
                    .status(SubmitResult.InvokeStatus.ACCEPTED)
                    .message("OK")
                    .build();
        } catch (Exception e) {
            log.error("Hermes invoke error for task: {}", taskId, e);
            return SubmitResult.builder()
                    .taskId(taskId)
                    .status(SubmitResult.InvokeStatus.REJECTED)
                    .message("Hermes invoke error: " + e.getMessage())
                    .build();
        }
    }

<<<<<<< HEAD
    /**
     * 异步轮询 Hermes Run 状态，完成后解析输出并回调。
     */
    private void pollUntilComplete(String runId, String taskId, String callbackUrl) {
        int maxAttempts = 200; // ~10 minutes at 3s interval
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Hermes polling interrupted for runId={}", runId);
                return;
            }

            try {
                RunStatus status = hermesClient.getRun(runId);
                if (status == null) {
                    log.warn("Hermes getRun returned null, runId={}, attempt={}", runId, i + 1);
                    continue;
                }

                String runStatus = status.getStatus();
                if ("completed".equalsIgnoreCase(runStatus)) {
                    log.info("Hermes run completed, runId={}, taskId={}", runId, taskId);
                    CallbackOutcome outcome = callbackParser.parseFromRunStatus(status, taskId);
                    if (callbackUrl != null && !callbackUrl.isEmpty()) {
                        postCallback(callbackUrl, outcome);
                    }
                    return;
                } else if ("failed".equalsIgnoreCase(runStatus) || "cancelled".equalsIgnoreCase(runStatus)) {
                    log.warn("Hermes run ended with status={}, runId={}, taskId={}", runStatus, runId, taskId);
                    CallbackOutcome outcome = CallbackOutcome.builder()
                            .taskId(taskId)
                            .status(CallbackOutcome.CallbackStatus.FAILED)
                            .errorMessage("Hermes run " + runStatus)
                            .build();
                    if (callbackUrl != null && !callbackUrl.isEmpty()) {
                        postCallback(callbackUrl, outcome);
                    }
                    return;
                }
                // else: still in progress, continue polling
            } catch (Exception e) {
                log.error("Hermes polling error, runId={}, attempt={}: {}", runId, i + 1, e.getMessage(), e);
            }
        }
        log.warn("Hermes polling timed out after {} attempts, runId={}", maxAttempts, runId);
    }

    /**
     * POST CallbackOutcome to callback URL (best-effort).
     */
    private void postCallback(String callbackUrl, CallbackOutcome outcome) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(outcome);
            java.net.URL url = new java.net.URL(callbackUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.getOutputStream().write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            int code = conn.getResponseCode();
            log.info("Hermes callback POST to {}, status={}", callbackUrl, code);
            conn.disconnect();
        } catch (Exception e) {
            log.error("Hermes callback POST failed to {}: {}", callbackUrl, e.getMessage(), e);
=======
    private void pollUntilComplete(String taskId, String runId, String callbackUrl, String callbackToken) {
        int maxAttempts = 100;
        int intervalMs = 3000;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(intervalMs);
                RunStatus status = hermesClient.getRun(runId);
                if (status == null) continue;
                String state = status.getStatus();
                if ("completed".equals(state)) {
                    CallbackOutcome outcome = callbackParser.parseFromRunStatus(status, taskId);
                    if (outcome != null) {
                        log.info("Hermes run completed, taskId={}, runId={}", taskId, runId);
                        // POST callback if URL provided
                        if (callbackUrl != null && !callbackUrl.isEmpty()) {
                            postCallback(callbackUrl, callbackToken, outcome);
                        }
                    }
                    taskIdToRunId.remove(taskId);
                    return;
                } else if ("failed".equals(state) || "cancelled".equals(state)) {
                    log.warn("Hermes run {}, taskId={}, runId={}", state, taskId, runId);
                    taskIdToRunId.remove(taskId);
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Hermes poll error, taskId={}, attempt={}: {}", taskId, i, e.getMessage());
            }
        }
        log.warn("Hermes poll timeout, taskId={}, runId={}", taskId, runId);
    }

    private void postCallback(String callbackUrl, String callbackToken, CallbackOutcome outcome) {
        try {
            log.info("Posting callback to {} for taskId={}", callbackUrl, outcome.getTaskId());
            // Use HermesClient's HTTP capability or a simple HTTP call
            // For now, log the callback - actual HTTP POST implementation depends on the callback protocol
        } catch (Exception e) {
            log.error("Failed to post callback for taskId={}: {}", outcome.getTaskId(), e.getMessage());
>>>>>>> c15e88a (feat: add hermes-agent adapter)
        }
    }

    @Override
    public void cancel(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            log.warn("Hermes cancel ignored: taskId is empty");
            return;
        }
        String runId = taskIdToRunId.remove(taskId);
        if (runId == null) {
            log.warn("Hermes cancel: no runId mapped for taskId={}", taskId);
            return;
        }
<<<<<<< HEAD
        log.info("Hermes cancel requested: taskId={}, runId={}", taskId, runId);
        try {
            hermesClient.stopRun(runId);
            log.info("Hermes cancel success for taskId={}, runId={}", taskId, runId);
        } catch (Exception e) {
            log.error("Hermes cancel failed for taskId={}, runId={}: {}", taskId, runId, e.getMessage(), e);
        }
    }

    /**
     * Hermes 使用异步轮询驱动回调，无 Webhook 回调入口。
     */
    @Override
    public CallbackOutcome handleCallback(RawCallbackPayload payload) {
        return null;
    }

=======
        try {
            hermesClient.stopRun(runId);
            log.info("Hermes cancel requested: taskId={}, runId={}", taskId, runId);
        } catch (Exception e) {
            log.error("Hermes cancel failed for taskId={}, runId={}: {}", taskId, runId, e.getMessage());
        }
    }

    @Override
    public CallbackOutcome handleCallback(RawCallbackPayload payload) {
        // Hermes has no webhook; polling drives callbacks
        return null;
    }
>>>>>>> c15e88a (feat: add hermes-agent adapter)
}
