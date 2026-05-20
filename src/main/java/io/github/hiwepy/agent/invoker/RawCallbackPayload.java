package io.github.hiwepy.agent.invoker;

import java.util.Map;

/**
 * Provider 回调的原始载荷。在 adapter 层完成协议解析前，保留 HTTP 头与原始 Body。
 */
public class RawCallbackPayload {

    private Map<String, String> headers;
    private String rawBody;
    private String providerCode;
    private Map<String, String> queryParams;

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> v) { this.headers = v; }

    public String getRawBody() { return rawBody; }
    public void setRawBody(String v) { this.rawBody = v; }

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String v) { this.providerCode = v; }

    public Map<String, String> getQueryParams() { return queryParams; }
    public void setQueryParams(Map<String, String> v) { this.queryParams = v; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final RawCallbackPayload p = new RawCallbackPayload();
        public Builder headers(Map<String, String> v) { p.headers = v; return this; }
        public Builder rawBody(String v) { p.rawBody = v; return this; }
        public Builder providerCode(String v) { p.providerCode = v; return this; }
        public Builder queryParams(Map<String, String> v) { p.queryParams = v; return this; }
        public RawCallbackPayload build() { return p; }
    }
}
