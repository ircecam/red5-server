package org.red5.net.websocket.server.util;

import org.apache.tomcat.websocket.Transformation;
import org.red5.net.websocket.server.WsHandshakeRequest;


import javax.websocket.Extension;
import java.util.List;
import java.util.Map;

/**
 * Represents the configuration of a WebSocket session.
 * This class encapsulates details about the session, including the handshake request, negotiated extensions,
 * the selected sub-protocol, applied transformations, path parameters, and security information.
 */
public class SessionConfig {
    /**
     * Represents the WebSocket handshake request used to initialize the session.
     */
    private final WsHandshakeRequest handshakeRequest;
    /**
     * A list of WebSocket protocol extensions that were successfully negotiated during the handshake process.
     */
    private final List<Extension> negotiatedExtensions;
    /**
     * Represents the sub-protocol negotiated during the WebSocket handshake.
     */
    private final String subProtocol;
    /**
     * Represents the transformation applied to WebSocket messages.
     */
    private final Transformation transformation;
    /**
     * A map representing path parameters derived from the WebSocket session's URI template.
     */
    private final Map<String, String> pathParameters;
    /**
     * Indicates whether the WebSocket session is conducted over a secure connection (e.g., using wss).
     * If true, the session uses a secure protocol; if false, it uses an insecure protocol.
     */
    private final boolean secure;

    /**
     * Constructs a new SessionConfig instance with the specified parameters.
     *
     * @param handshakeRequest The WebSocket handshake request associated with this session.
     * @param negotiatedExtensions A list of extensions negotiated during the WebSocket handshake.
     * @param subProtocol The sub-protocol selected for this WebSocket session.
     * @param transformation The transformation logic applied to data within this WebSocket session.
     * @param pathParameters A map of path parameters inferred from the WebSocket endpoint URI.
     * @param secure A boolean indicating whether the WebSocket session is operating over a secure (e.g., wss) connection.
     */
    public SessionConfig(WsHandshakeRequest handshakeRequest,
                         List<Extension> negotiatedExtensions,
                         String subProtocol,
                         Transformation transformation,
                         Map<String, String> pathParameters,
                         boolean secure) {
        this.handshakeRequest = handshakeRequest;
        this.negotiatedExtensions = negotiatedExtensions;
        this.subProtocol = subProtocol;
        this.transformation = transformation;
        this.pathParameters = pathParameters;
        this.secure = secure;
    }

    /**
     * Retrieves the WebSocket handshake request associated with the current session configuration.
     *
     * @return the instance of {@link WsHandshakeRequest} containing details about the WebSocket handshake,
     *         such as the request URI, query parameters, headers, user principal, and associated HTTP session.
     */
    public WsHandshakeRequest getHandshakeRequest() {
        return handshakeRequest;
    }

    /**
     * Retrieves the list of negotiated WebSocket extensions for the current session.
     * @return a list of negotiated extensions for the session
     */
    public List<Extension> getNegotiatedExtensions() {
        return negotiatedExtensions;
    }

    /**
     * Retrieves the negotiated sub-protocol for the WebSocket session.
     *
     * @return the sub-protocol selected during the WebSocket handshake, or {@code null} if no sub-protocol was negotiated.
     */
    public String getSubProtocol() {
        return subProtocol;
    }

    /**
     * Retrieves the transformation applied to the WebSocket session.
     *
     * @return the {@link Transformation} object associated with the session,
     *         representing the applied data transformations or filters.
     */
    public Transformation getTransformation() {
        return transformation;
    }

    /**
     * Retrieves the path parameters associated with the WebSocket session.
     *
     * @return a map containing the path parameters where the keys are parameter names and the values are parameter values.
     */
    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    /**
     * Indicates whether the session is secure.
     *
     * @return {@code true} if the session is secure (e.g., using a secure protocol like "wss");
     *         {@code false} otherwise.
     */
    public boolean isSecure() {
        return secure;
    }
}
