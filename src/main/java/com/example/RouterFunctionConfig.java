package com.example;

import com.example.handler.TestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

@Configuration
public class RouterFunctionConfig {

    @Autowired
    TestHandler testHandler;

    @Bean
    public RouterFunction routerFunction() {

        return
                //静态图形验证码获取校验
                RouterFunctions.route(RequestPredicates.path("/test").and(RequestPredicates.accept(MediaType.ALL)), testHandler::handle);

    }
}
