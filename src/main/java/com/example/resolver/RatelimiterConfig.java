package com.example.resolver;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 自定义keyResolver
 */
@Configuration
public class RatelimiterConfig {
    @Bean(name = "remoteAddrKeyResolver")
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }

    @Bean(name = "uriKeyResolver")
    public KeyResolver uriKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getURI().getPath());
    }

    @Primary
    @Bean(name = "apiKeyResolver")
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }
}
