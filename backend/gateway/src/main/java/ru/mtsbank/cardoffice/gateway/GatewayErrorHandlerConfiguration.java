package ru.mtsbank.cardoffice.gateway;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

/**
 * Story 0's I/O & Edge-Case Matrix requires: "Target service down -> Gateway
 * returns 502/503, not a hang". By default, an unhandled failure while
 * proxying a route (backing container stopped, DNS lookup failing, connection
 * refused, etc.) falls through to Spring Boot's generic reactive error
 * handler and comes back as a bare 500. This handler runs ahead of that
 * default (see {@code @Order}) and maps such proxying failures to 502 Bad
 * Gateway instead, using the error envelope shape the architecture spine
 * fixes for all services (AD-3/Consistency Conventions: {@code { error: {
 * code, message } } }), so the Gateway's own error responses already look
 * like what every backing service will eventually return.
 *
 * Only network-failure exception types are mapped this way (see
 * {@link #isNetworkFailure(Throwable)}); anything else -- a routing/config
 * mistake, a bug in this app -- is left to {@code Mono.error(ex)} so Boot's
 * default handler reports it normally instead of being mislabeled as an
 * unreachable backend.
 *
 * This does not touch the connect/response timeouts configured in
 * application.yml, which bound how long a proxy attempt can hang before this
 * handler even gets a chance to run.
 */
@Configuration
public class GatewayErrorHandlerConfiguration {

    @Bean
    @Order(-2)
    public ErrorWebExceptionHandler upstreamUnavailableExceptionHandler() {
        return (ServerWebExchange exchange, Throwable ex) -> {
            if (!isNetworkFailure(ex)) {
                return Mono.error(ex);
            }

            ServerHttpResponse response = exchange.getResponse();
            if (response.isCommitted()) {
                return Mono.error(ex);
            }

            response.setStatusCode(HttpStatus.BAD_GATEWAY);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String body = "{\"error\":{\"code\":\"UPSTREAM_UNAVAILABLE\",";
            body += "\"message\":\"The target service did not respond.\"}}";
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        };
    }

    /**
     * True only for failures that mean "couldn't reach, or didn't hear back
     * in time from, the target service": connection refused, DNS lookup
     * failure, connect/read/write timeout, or the connection closing
     * prematurely. Walks the cause chain because Reactor Netty routinely
     * wraps the real network exception inside another one before it reaches
     * this handler (e.g. a stopped compose container surfaces as an
     * {@link UnknownHostException} caused by a Netty DNS NXDOMAIN error).
     */
    private static boolean isNetworkFailure(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConnectException
                    || t instanceof UnknownHostException
                    || t instanceof TimeoutException
                    || t instanceof ConnectTimeoutException
                    || t instanceof ReadTimeoutException
                    || t instanceof WriteTimeoutException
                    || t instanceof PrematureCloseException) {
                return true;
            }
        }
        return false;
    }
}
