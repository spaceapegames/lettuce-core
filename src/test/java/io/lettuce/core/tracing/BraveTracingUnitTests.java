/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import zipkin2.Span;
import brave.Tracer;
import brave.Tracing;
import brave.internal.recorder.MutableSpan;
import brave.propagation.CurrentTraceContext;
import io.lettuce.core.TestSupport;
import io.netty.channel.unix.DomainSocketAddress;

/**
 * @author Mark Paluch
 * @author Daniel Albuquerque
 */
class BraveTracingUnitTests extends TestSupport {

    private static Tracing clientTracing;
    private static Queue<Span> spans = new LinkedBlockingQueue<>();

    @BeforeAll
    static void beforeClass() {

        clientTracing = Tracing.newBuilder().localServiceName("client")
                .currentTraceContext(CurrentTraceContext.Default.create()).spanReporter(spans::add).build();
    }

    @BeforeEach
    void before() {

        Tracer tracer = clientTracing.tracer();
        if (tracer.currentSpan() != null) {
            clientTracing.tracer().currentSpan().abandon();
        }

        spans.clear();
    }

    @AfterAll
    static void afterClass() {

        clientTracing.close();
    }

    @Test
    void shouldReportSimpleServiceName() {

        BraveTracing tracing = BraveTracing.create(clientTracing);
        BraveTracing.BraveEndpoint endpoint = (BraveTracing.BraveEndpoint) tracing
                .createEndpoint(new DomainSocketAddress("foo"));

        assertThat(endpoint.endpoint.serviceName()).isEqualTo("redis");
        assertThat(endpoint.endpoint.port()).isNull();
        assertThat(endpoint.endpoint.ipv4()).isNull();
        assertThat(endpoint.endpoint.ipv6()).isNull();
    }

    @Test
    void shouldReportCustomServiceName() {

        BraveTracing tracing = BraveTracing.builder().tracing(clientTracing).serviceName("custom-name-goes-here").build();

        BraveTracing.BraveEndpoint endpoint = (BraveTracing.BraveEndpoint) tracing
                .createEndpoint(new DomainSocketAddress("foo"));

        assertThat(endpoint.endpoint.serviceName()).isEqualTo("custom-name-goes-here");
        assertThat(endpoint.endpoint.port()).isNull();
        assertThat(endpoint.endpoint.ipv4()).isNull();
        assertThat(endpoint.endpoint.ipv6()).isNull();
    }

    @Test
    void shouldCustomizeEndpoint() {

        BraveTracing tracing = BraveTracing.builder().tracing(clientTracing)
                .endpointCustomizer(it -> it.serviceName("foo-bar")).build();
        BraveTracing.BraveEndpoint endpoint = (BraveTracing.BraveEndpoint) tracing
                .createEndpoint(new DomainSocketAddress("foo"));

        assertThat(endpoint.endpoint.serviceName()).isEqualTo("foo-bar");
    }

    @Test
    void shouldCustomizeSpan() {

        BraveTracing tracing = BraveTracing.builder().tracing(clientTracing)
                .spanCustomizer(it -> it.remoteServiceName("remote")).build();

        BraveTracing.BraveSpan span = (BraveTracing.BraveSpan) tracing.getTracerProvider().getTracer().nextSpan();
        span.finish();

        MutableSpan braveSpan = (MutableSpan) ReflectionTestUtils.getField(span.getSpan(), "state");

        assertThat(braveSpan.remoteServiceName()).isEqualTo("remote");
    }
}
