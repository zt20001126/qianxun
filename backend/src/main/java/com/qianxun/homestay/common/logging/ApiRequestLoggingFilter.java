package com.qianxun.homestay.common.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.qianxun.homestay.common.security.AuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/** 在控制台记录 HTTP 请求参数、正文摘要、认证身份和处理结果。 */
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final int REQUEST_BODY_CACHE_LIMIT = 64 * 1024;
    private static final Pattern SENSITIVE_TEXT_VALUE = Pattern.compile(
            "(?i)(\\\"?(?:password|passwd|pwd|token|authorization|secret|access[_-]?key|api[_-]?key|credential|login[_-]?code|phone[_-]?code|js[_-]?code|openid|recipient[_-]?(?:name|phone)|shipping[_-]?address)\\\"?\\s*[:=]\\s*\\\"?)[^\\\"&,}\\s]*");

    private final ObjectMapper objectMapper;

    public ApiRequestLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        int status = HttpServletResponse.SC_OK;
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, REQUEST_BODY_CACHE_LIMIT);
        try {
            filterChain.doFilter(cachedRequest, response);
            status = response.getStatus();
        } catch (IOException | ServletException | RuntimeException exception) {
            status = response.getStatus() >= HttpServletResponse.SC_BAD_REQUEST
                    ? response.getStatus()
                    : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            throw exception;
        } finally {
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            String query = sanitizeQuery(request.getQueryString());
            String body = request.getRequestURI().startsWith("/api/payment/wechat/notify/")
                    ? "[wechat notification omitted]"
                    : getRequestBody(cachedRequest);
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            LOGGER.info("HTTP 请求 method={} path={} query={} auth={} auth_scheme={} body={} status={} duration_ms={}",
                    request.getMethod(), request.getRequestURI(), query, authenticationInfo(),
                    authorizationScheme(authorization), body, status, durationMillis);
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] body = request.getContentAsByteArray();
        if (body.length == 0) {
            return "-";
        }

        String contentType = request.getContentType();
        if (contentType != null && isBinaryContent(contentType)) {
            return "[binary body omitted, bytes=" + body.length + "]";
        }

        Charset charset = request.getCharacterEncoding() == null
                ? StandardCharsets.UTF_8
                : Charset.forName(request.getCharacterEncoding());
        String rawBody = new String(body, charset);
        String safeBody;
        if (contentType != null && isJsonContent(contentType)) {
            safeBody = sanitizeJsonBody(body, rawBody);
        } else if (contentType != null && contentType.toLowerCase(Locale.ROOT)
                .startsWith("application/x-www-form-urlencoded")) {
            safeBody = sanitizeQuery(rawBody);
        } else {
            safeBody = sanitizeText(rawBody);
        }

        if (request.getContentLengthLong() > body.length && body.length == REQUEST_BODY_CACHE_LIMIT) {
            safeBody += "…[truncated]";
        }
        return safeBody;
    }

    private String sanitizeJsonBody(byte[] body, String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(body);
            redactSensitiveJsonFields(root);
            return objectMapper.writeValueAsString(root);
        } catch (IOException exception) {
            return sanitizeText(rawBody);
        }
    }

    private void redactSensitiveJsonFields(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            node.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                if (isSensitiveField(fieldName)) {
                    objectNode.set(fieldName, TextNode.valueOf("[REDACTED]"));
                } else {
                    redactSensitiveJsonFields(objectNode.get(fieldName));
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::redactSensitiveJsonFields);
        }
    }

    private String sanitizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "-";
        }

        String[] pairs = query.split("&", -1);
        for (int index = 0; index < pairs.length; index++) {
            int separator = pairs[index].indexOf('=');
            String rawName = separator < 0 ? pairs[index] : pairs[index].substring(0, separator);
            String decodedName;
            try {
                decodedName = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exception) {
                decodedName = rawName;
            }
            if (isSensitiveField(decodedName) && separator >= 0) {
                pairs[index] = rawName + "=[REDACTED]";
            }
        }
        return sanitizeLineBreaks(String.join("&", pairs));
    }

    private String sanitizeText(String value) {
        Matcher matcher = SENSITIVE_TEXT_VALUE.matcher(value);
        return sanitizeLineBreaks(matcher.replaceAll("$1[REDACTED]"));
    }

    private boolean isSensitiveField(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.contains("password")
                || normalized.equals("passwd")
                || normalized.equals("pwd")
                || normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("secret")
                || normalized.contains("accesskey")
                || normalized.contains("apikey")
                || normalized.contains("credential")
                || normalized.equals("code")
                || normalized.endsWith("code")
                || normalized.equals("recipientname")
                || normalized.equals("recipientphone")
                || normalized.equals("shippingaddress")
                || normalized.contains("openid");
    }

    private boolean isJsonContent(String contentType) {
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.contains("application/json") || normalized.contains("+json");
    }

    private boolean isBinaryContent(String contentType) {
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("multipart/")
                || normalized.startsWith("application/octet-stream")
                || normalized.startsWith("image/")
                || normalized.startsWith("audio/")
                || normalized.startsWith("video/");
    }

    private String authenticationInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }

        if (authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return principal.role().name() + "#" + principal.id();
        }
        return "authenticated";
    }

    private String authorizationScheme(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "-";
        }
        int separator = authorization.indexOf(' ');
        return separator < 0 ? "provided" : authorization.substring(0, separator);
    }

    private String sanitizeLineBreaks(String value) {
        return value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
    }
}
