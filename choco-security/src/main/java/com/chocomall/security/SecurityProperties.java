package com.chocomall.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings shared by the back office and the storefront.
 *
 * @param ignoredUrls        public paths (login, documentation, health...)
 * @param authenticatedUrls  paths any signed-in user may call without a matching resource grant
 * @param dynamicAuthorization when true, every other request must match a resource pattern granted to the user
 *                           (back office); when false, being signed in is enough (storefront)
 * @param corsAllowedOrigins origins allowed to call the API from a browser
 * @param jwt                token settings
 */
@ConfigurationProperties(prefix = "secure")
public record SecurityProperties(
        List<String> ignoredUrls,
        List<String> authenticatedUrls,
        boolean dynamicAuthorization,
        List<String> corsAllowedOrigins,
        Jwt jwt) {

    public SecurityProperties {
        ignoredUrls = ignoredUrls == null ? List.of() : ignoredUrls;
        authenticatedUrls = authenticatedUrls == null ? List.of() : authenticatedUrls;
        corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : corsAllowedOrigins;
    }

    /**
     * @param secret         Base64-encoded HMAC key of at least 32 bytes
     * @param expirationSeconds token lifetime
     * @param audience       "admin" or "member": a token is only accepted by the application it was issued for
     * @param header         HTTP header carrying the token
     * @param tokenHead      prefix before the token, including the trailing space
     */
    public record Jwt(String secret, long expirationSeconds, String audience, String header, String tokenHead) {

        public Jwt {
            header = header == null ? "Authorization" : header;
            tokenHead = tokenHead == null ? "Bearer " : tokenHead;
            expirationSeconds = expirationSeconds <= 0 ? 3600 : expirationSeconds;
        }
    }
}
