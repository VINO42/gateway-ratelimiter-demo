package com.example.filter;

import com.example.response.ServiceResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.HasRouteId;
import org.springframework.cloud.gateway.support.HttpStatusHolder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayRequestRateLimiterGatewayFilterFactory extends
        AbstractGatewayFilterFactory<GatewayRequestRateLimiterGatewayFilterFactory.Config> implements Ordered {
    ObjectMapper mapper = new ObjectMapper();

    /**
     * Key-Resolver key.
     */
    public static final String KEY_RESOLVER_KEY = "keyResolver";

    private static final String EMPTY_KEY = "____EMPTY_KEY__";

    private final RateLimiter defaultRateLimiter;

    private final KeyResolver defaultKeyResolver;

    /**
     * Switch to deny requests if the Key Resolver returns an empty key, defaults to true.
     */
    private boolean denyEmptyKey = true;

    /**
     * HttpStatus to return when denyEmptyKey is true, defaults to FORBIDDEN.
     */
    private String emptyKeyStatusCode = HttpStatus.FORBIDDEN.name();

    public GatewayRequestRateLimiterGatewayFilterFactory(RateLimiter defaultRateLimiter,
                                                         KeyResolver defaultKeyResolver) {
        super(GatewayRequestRateLimiterGatewayFilterFactory.Config.class);
        this.defaultRateLimiter = defaultRateLimiter;
        this.defaultKeyResolver = defaultKeyResolver;
    }

    public KeyResolver getDefaultKeyResolver() {
        return defaultKeyResolver;
    }

    public RateLimiter getDefaultRateLimiter() {
        return defaultRateLimiter;
    }

    public boolean isDenyEmptyKey() {
        return denyEmptyKey;
    }

    public void setDenyEmptyKey(boolean denyEmptyKey) {
        this.denyEmptyKey = denyEmptyKey;
    }

    public String getEmptyKeyStatusCode() {
        return emptyKeyStatusCode;
    }

    public void setEmptyKeyStatusCode(String emptyKeyStatusCode) {
        this.emptyKeyStatusCode = emptyKeyStatusCode;
    }

    @SuppressWarnings("unchecked")
    @Override
    public GatewayFilter apply(GatewayRequestRateLimiterGatewayFilterFactory.Config config) {
        KeyResolver resolver = getOrDefault(config.keyResolver, defaultKeyResolver);
        RateLimiter<Object> limiter = getOrDefault(config.rateLimiter,
                defaultRateLimiter);
        boolean denyEmpty = getOrDefault(config.denyEmptyKey, this.denyEmptyKey);
        HttpStatusHolder emptyKeyStatus = HttpStatusHolder
                .parse(getOrDefault(config.emptyKeyStatus, this.emptyKeyStatusCode));

        return (exchange, chain) -> resolver.resolve(exchange).defaultIfEmpty(EMPTY_KEY)
                .flatMap(key -> {
                    if (EMPTY_KEY.equals(key)) {
                        if (denyEmpty) {
                            setResponseStatus(exchange, emptyKeyStatus);
                            return exchange.getResponse().setComplete();
                        }
                        return chain.filter(exchange);
                    }
                    String routeId = config.getRouteId();
                    if (routeId == null) {
                        Route route = exchange
                                .getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                        routeId = route.getId();
                    }
                    return limiter.isAllowed(routeId, key).flatMap(response -> {

                        for (Map.Entry<String, String> header : response.getHeaders()
                                .entrySet()) {
                            exchange.getResponse().getHeaders().add(header.getKey(),
                                    header.getValue());
                        }

                        if (response.isAllowed()) {
                            return chain.filter(exchange);
                        }

                        ServerHttpResponse httpResponse = exchange.getResponse();
                        setResponseStatus(exchange, config.getStatusCode());
                        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_UTF8_VALUE);
                        ServiceResponse serviceResponse = new ServiceResponse();
                        serviceResponse.setMessage(new String("您操作的太快啦".getBytes(StandardCharsets.UTF_8)));
                        serviceResponse.setStatus(429000);
                        serviceResponse.setData("Server throttling");
                        DataBuffer dataBuffer = null;
                        try {
                            byte[] bits =   mapper.writeValueAsBytes(serviceResponse);
                            dataBuffer = httpResponse.bufferFactory().wrap(bits);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        return httpResponse.writeWith(Mono.just(dataBuffer));
                    });
                });
    }

    private <T> T getOrDefault(T configValue, T defaultValue) {
        return (configValue != null) ? configValue : defaultValue;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }


    public static class Config implements HasRouteId {

        private KeyResolver keyResolver;

        private RateLimiter rateLimiter;

        private HttpStatus statusCode = HttpStatus.TOO_MANY_REQUESTS;

        private Boolean denyEmptyKey;

        private String emptyKeyStatus;

        private String routeId;

        public KeyResolver getKeyResolver() {
            return keyResolver;
        }

        public GatewayRequestRateLimiterGatewayFilterFactory.Config setKeyResolver(KeyResolver keyResolver) {
            this.keyResolver = keyResolver;
            return this;
        }

        public RateLimiter getRateLimiter() {
            return rateLimiter;
        }

        public GatewayRequestRateLimiterGatewayFilterFactory.Config setRateLimiter(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
            return this;
        }

        public HttpStatus getStatusCode() {
            return statusCode;
        }

        public GatewayRequestRateLimiterGatewayFilterFactory.Config setStatusCode(HttpStatus statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Boolean getDenyEmptyKey() {
            return denyEmptyKey;
        }

        public GatewayRequestRateLimiterGatewayFilterFactory.Config setDenyEmptyKey(Boolean denyEmptyKey) {
            this.denyEmptyKey = denyEmptyKey;
            return this;
        }

        public String getEmptyKeyStatus() {
            return emptyKeyStatus;
        }

        public GatewayRequestRateLimiterGatewayFilterFactory.Config setEmptyKeyStatus(String emptyKeyStatus) {
            this.emptyKeyStatus = emptyKeyStatus;
            return this;
        }

        @Override
        public void setRouteId(String routeId) {
            this.routeId = routeId;
        }

        @Override
        public String getRouteId() {
            return this.routeId;
        }

    }

}