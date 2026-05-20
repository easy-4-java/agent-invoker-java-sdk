package io.github.hiwepy.agent.invoker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AI Agent 调用路由器。根据 providerCode 选择对应的 Provider 实现。
 *
 * <p>使用方式：
 * <pre>
 * Router router = new Router();
 * router.register(new OpenClawAiAgentInvoker(...));
 * AiAgentInvoker invoker = router.route("openclaw");
 * </pre>
 */
public class AiAgentInvokerRouter {

    private final List<AiAgentInvoker> invokers = new ArrayList<>();

    public AiAgentInvokerRouter() {}

    public AiAgentInvokerRouter(List<AiAgentInvoker> invokers) {
        if (invokers != null) {
            this.invokers.addAll(invokers);
        }
    }

    /** 注册一个 Provider 实现。 */
    public void register(AiAgentInvoker invoker) {
        this.invokers.add(Objects.requireNonNull(invoker, "invoker"));
    }

    /** 根据 providerCode 路由。null 或空字符串回退到 "openclaw"。 */
    public AiAgentInvoker route(String providerCode) {
        String code = (providerCode == null || providerCode.isEmpty()) ? "openclaw" : providerCode;
        return invokers.stream()
                .filter(it -> it.providerCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new AgentInvokeException("No AiAgentInvoker for provider: " + code));
    }

    /** 根据 cmd 中的 providerCode 路由。 */
    public AiAgentInvoker route(AgentInvokeCmd cmd) {
        return route(cmd != null ? cmd.getProviderCode() : null);
    }

    public List<AiAgentInvoker> getInvokers() { return invokers; }
}
