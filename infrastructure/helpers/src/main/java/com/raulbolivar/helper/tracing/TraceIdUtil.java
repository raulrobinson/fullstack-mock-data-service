package com.raulbolivar.helper.tracing;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TraceIdUtil {

    private final ObjectProvider<Tracer> tracerProvider;

    public String getTraceId() {

        Tracer tracer = tracerProvider.getIfAvailable();

        if (tracer == null || tracer.currentSpan() == null) {
            return null;
        }

        return Objects.requireNonNull(tracer.currentSpan()).context().traceId();
    }
}
