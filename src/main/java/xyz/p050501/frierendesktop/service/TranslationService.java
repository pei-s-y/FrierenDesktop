package xyz.p050501.frierendesktop.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
public final class TranslationService implements AutoCloseable {
    private final String appId = System.getenv("BAIDU_APP_ID");
    private final String secret = System.getenv("BAIDU_SECRET_KEY");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    public boolean configured() { return appId != null && !appId.isBlank() && secret != null && !secret.isBlank(); }
    public CompletableFuture<String> translate(String text, String target) {
        if (!configured()) return CompletableFuture.failedFuture(new IllegalStateException("请先配置 BAIDU_APP_ID 和 BAIDU_SECRET_KEY 环境变量，再重启。"));
        if (text == null || text.isBlank()) return CompletableFuture.failedFuture(new IllegalArgumentException("剪贴板里还没有文字。"));
        if (text.getBytes(StandardCharsets.UTF_8).length > 6000) return CompletableFuture.failedFuture(new IllegalArgumentException("文字太长，请选取不超过 6000 字节的片段。"));
        if (!target.equals("zh") && !target.equals("en")) throw new IllegalArgumentException("Unsupported target");
        String salt = UUID.randomUUID().toString();
        String body = "q=" + encode(text) + "&from=auto&to=" + target + "&appid=" + encode(appId)
            + "&salt=" + salt + "&sign=" + signature(appId + text + salt + secret);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://fanyi-api.baidu.com/api/trans/vip/translate"))
            .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) throw new IllegalStateException("翻译服务暂时不可用（HTTP " + response.statusCode() + "）。");
            return parseResponse(response.body());
        });
    }
    public String parseResponse(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root == null) throw new IllegalArgumentException();
            if (root.has("error_code")) throw new IllegalStateException("翻译失败，服务代码：" + root.path("error_code").asText());
            JsonNode results = root.path("trans_result");
            if (!results.isArray() || results.isEmpty()) throw new IllegalArgumentException();
            StringBuilder translated = new StringBuilder();
            for (JsonNode result : results) {
                if (!result.path("dst").isTextual()) throw new IllegalArgumentException();
                if (!translated.isEmpty()) translated.append('\n');
                translated.append(result.get("dst").asText());
            }
            return translated.toString();
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("翻译服务返回了无法识别的内容。", e); }
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String signature(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("无法创建翻译签名。", e); }
    }
    @Override public void close() { client.shutdownNow(); }
}
