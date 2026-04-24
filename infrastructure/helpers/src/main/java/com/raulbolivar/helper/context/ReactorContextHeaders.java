package com.raulbolivar.helper.context;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

public class ReactorContextHeaders {

    public static ExchangeFilterFunction propagateHeaders() {

        return (request, next) ->
                Mono.deferContextual(ctx -> {

                    ClientRequest.Builder builder = ClientRequest.from(request);

                    if (ctx.hasKey("Authorization")) {
                        String authorization = ctx.get("Authorization");
                        builder.header("Authorization", authorization);
                    }

                    if (ctx.hasKey("X-Correlation-Id")) {
                        String correlationId = ctx.get("X-Correlation-Id");
                        builder.header("X-Correlation-Id", correlationId);
                    }

                    return next.exchange(builder.build());
                });
    }

    private ReactorContextHeaders() {
    }
}
