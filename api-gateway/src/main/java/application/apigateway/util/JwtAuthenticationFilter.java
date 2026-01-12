package application.apigateway.util;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private JwtValidator jwtValidator;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = extractToken(request);

            if (!validateToken(exchange, token)) {
                return onError(exchange, "Unauthorized", HttpStatus.UNAUTHORIZED);
            }

            String userId = jwtValidator.getUserIdFromToken(token);
            String email = jwtValidator.getEmailFromToken(token);
            String role = jwtValidator.getRoleFromToken(token);
            String permissions = jwtValidator.getPermissionsFromToken(token);


            // Forward the request with user details if authorized
            ServerHttpRequest modifiedRequest = addHeaders(request, email, userId, role, permissions);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private boolean validateToken(ServerWebExchange exchange, String token) {
        if (token == null) {
            onError(exchange, "Unauthorized", HttpStatus.UNAUTHORIZED).subscribe();
            return false;
        }
        if (!jwtValidator.validateToken(token)) {
            onError(exchange, "Unauthorized", HttpStatus.UNAUTHORIZED).subscribe();
            return false;
        }
        return true;
    }

    private ServerHttpRequest addHeaders(ServerHttpRequest request, String email, String userId, String role, String permissions) {
        return request.mutate()
                .header("X-User-Id", userId)
                .header("X-Auth-User", email)
                .header("User-Id", userId)
                .header("X-User-Role", role)
                .header("X-User-Permission", permissions)
                .build();
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);

        // Create a meaningful JSON response
        String responseBody = String.format(
                "{\"message\": \"%s\", \"status\": \"%s\"}",
                errorMessage,
                status.value()
        );

        byte[] bytes = responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Don't try to set headers - let Spring handle it
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(bytes)));
    }

    public static class Config {
        // Configuration properties
    }
}