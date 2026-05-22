package io.github.hiwepy.agent.invoker.openclaw;

import io.github.hiwepy.agent.invoker.AgentInvokeCmd;
import io.github.hiwepy.openclaw.InvokeAgentRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OpenClawInvokeRequestMapper} 单元测试。
 */
class OpenClawInvokeRequestMapperTest {

    @Test
    void shouldPreferCmdCallbackUrlOverBaseUrl() {
        AgentInvokeCmd cmd = AgentInvokeCmd.builder()
                .callbackUrl("http://app/callback")
                .build();
        assertEquals("http://app/callback",
                OpenClawInvokeRequestMapper.resolveCallbackUrl(cmd, "http://base/default"));
    }

    @Test
    void shouldFallbackToCallbackBaseUrl() {
        AgentInvokeCmd cmd = AgentInvokeCmd.builder().build();
        assertEquals("http://base/default",
                OpenClawInvokeRequestMapper.resolveCallbackUrl(cmd, "http://base/default"));
    }

    @Test
    void shouldMapMetadataFieldsToInvokeRequest() {
        AgentInvokeCmd cmd = AgentInvokeCmd.builder()
                .agentId("main")
                .enhancedPrompt("Generate post")
                .tenantId("t1")
                .userId("u1")
                .channel("xiaohongshu")
                .taskId("task-001")
                .callbackUrl("http://localhost/save")
                .callbackToken("secret")
                .variables(Collections.<String, Object>singletonMap("tone", "casual"))
                .build();

        InvokeAgentRequest request = OpenClawInvokeRequestMapper.toInvokeRequest(cmd, "http://ignored");
        assertEquals("main", request.getAgentId());
        assertEquals("xiaohongshu", request.getChannel());
        assertEquals("u1", request.getTo());
        assertEquals("hook:t1.u1:task-001", request.getSessionKey());
        assertTrue(request.getMessage().contains("Generate post"));
        assertTrue(request.getMessage().contains("http://localhost/save"));
        assertTrue(request.getMessage().contains("task_id=\"task-001\""));
        assertTrue(request.getMessage().contains("Tenant ID: t1"));
        assertTrue(request.getMessage().contains("casual"));
        assertTrue(request.getMessage().contains("tone"));
    }

    @Test
    void shouldBuildSessionKeyFromTenantAndTaskOnly() {
        AgentInvokeCmd cmd = AgentInvokeCmd.builder()
                .tenantId("tenant-a")
                .taskId("task-b")
                .build();
        assertEquals("hook:tenant-a:task-b", OpenClawInvokeRequestMapper.buildSessionKey(cmd));
    }

    @Test
    void shouldMapOpenClawVariablesToHookFields() {
        AgentInvokeCmd cmd = AgentInvokeCmd.builder()
                .agentId("main")
                .enhancedPrompt("hello")
                .variables(java.util.Map.of(
                        "openclaw.deliver", true,
                        "openclaw.model", "openai/gpt-5.5",
                        "openclaw.thinking", "off",
                        "openclaw.wakeMode", "next-heartbeat",
                        "openclaw.timeoutSeconds", 120,
                        "openclaw.name", "CustomJob",
                        "openclaw.channel", "last",
                        "openclaw.to", "peer-9",
                        "tone", "casual"))
                .build();

        InvokeAgentRequest request = OpenClawInvokeRequestMapper.toInvokeRequest(cmd, "http://base");
        assertEquals(true, request.getDeliver());
        assertEquals("openai/gpt-5.5", request.getModel());
        assertEquals("off", request.getThinking());
        assertEquals("next-heartbeat", request.getWakeMode());
        assertEquals(120, request.getTimeoutSeconds());
        assertEquals("CustomJob", request.getName());
        assertEquals("last", request.getChannel());
        assertEquals("peer-9", request.getTo());
    }
}
