package com.chocomall.search.config;

import com.chocomall.common.api.CommonResult;
import com.chocomall.common.api.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Search reads are public; changing the index requires the shared admin key. */
@Configuration
@EnableConfigurationProperties(SearchProperties.class)
@OpenAPIDefinition(info = @Info(title = "ChocoMall Search API", version = "1.0.0",
        description = "Product search, related-search facets and recommendations."))
@RequiredArgsConstructor
public class SearchWebConfig implements WebMvcConfigurer {

    public static final String KEY_HEADER = "X-Search-Admin-Key";

    private final SearchProperties props;
    private final ObjectMapper mapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String given = request.getHeader(KEY_HEADER);
                boolean ok = !props.adminKey().isBlank() && given != null
                        && MessageDigest.isEqual(props.adminKey().getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
                if (!ok) {
                    int status = props.adminKey().isBlank() ? 403 : 401;
                    response.setStatus(status);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    mapper.writeValue(response.getOutputStream(), CommonResult.failed(
                            status == 403 ? ResultCode.FORBIDDEN : ResultCode.UNAUTHORIZED,
                            props.adminKey().isBlank() ? "Index management is disabled (search.admin-key is not set)" : "Missing or wrong " + KEY_HEADER));
                }
                return ok;
            }
        }).addPathPatterns("/search/index/**");
    }
}
