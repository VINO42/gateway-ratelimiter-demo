package com.example.handler;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class TestHandler implements HandlerFunction<ServerResponse> {
    private static final Integer DEFAULT_IMAGE_WIDTH = 100;
    private static final Integer DEFAULT_IMAGE_HEIGHT = 40;

    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
        return ServerResponse
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromResource(new ByteArrayResource("你好网关限流".getBytes(StandardCharsets.UTF_8))));
    }
}
