package org.apache.isis.viewer.graphql.viewer.webmodule;

import org.springframework.graphql.web.WebInput;
import org.springframework.graphql.web.WebInterceptor;
import org.springframework.graphql.web.WebInterceptorChain;
import org.springframework.graphql.web.WebOutput;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class MyWebInterceptor implements WebInterceptor {

    @Override
    public Mono<WebOutput> intercept(WebInput webInput, WebInterceptorChain chain) {
        return null;
    }

    @Override
    public WebInterceptor andThen(WebInterceptor interceptor) {
        return WebInterceptor.super.andThen(interceptor);
    }
}
