package com.example.xo.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component that starts the standalone WebSocket server when Spring Boot starts.
 */
@Component
public class WebSocketServerStarter {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServerStarter.class);

    @Value("${websocket.port:8081}")
    private int port;

    private StandaloneGameWebSocketServer server;

    @PostConstruct
    public void start() {
        // validate port
        if (port < 1 || port > 65535) {
            log.error("Invalid websocket.port value: {}. Port must be between 1 and 65535.", port);
            throw new IllegalArgumentException("Invalid websocket.port: " + port);
        }

        try {
            // instantiate with the integer port (StandaloneGameWebSocketServer wraps InetSocketAddress)
            server = new StandaloneGameWebSocketServer(port);

            // start server (typically non-blocking; it will start background threads)
            server.start();
            log.info("StandaloneGameWebSocketServer started and listening on port {}", port);
        } catch (Exception e) {
            log.error("Failed to start StandaloneGameWebSocketServer on port {}: {}", port, e.getMessage(), e);
            // fail fast: if the websocket server is essential, fail application startup so user sees the error
            throw new RuntimeException("Unable to start WebSocket server on port " + port, e);
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        if (server != null) {
            try {
                log.info("Stopping StandaloneGameWebSocketServer...");
                // WebSocketServer#stop() may throw InterruptedException/IOException depending on underlying implementation
                server.stop();
                log.info("StandaloneGameWebSocketServer stopped.");
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while stopping WebSocket server: {}", ie.getMessage(), ie);
            } catch (Exception e) {
                log.error("Error while stopping StandaloneGameWebSocketServer: {}", e.getMessage(), e);
            }
        } else {
            log.debug("No StandaloneGameWebSocketServer instance to stop.");
        }
    }
}
