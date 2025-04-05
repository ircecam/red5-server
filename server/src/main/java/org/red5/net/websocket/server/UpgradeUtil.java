package org.red5.net.websocket.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.security.ConcurrentMessageDigest;
import org.apache.tomcat.websocket.Constants;
import org.apache.tomcat.websocket.Transformation;
import org.apache.tomcat.websocket.TransformationFactory;
import org.apache.tomcat.websocket.Util;
import org.apache.tomcat.websocket.WsHandshakeResponse;
import org.apache.tomcat.websocket.pojo.PojoEndpointServer;
import org.red5.net.websocket.server.util.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeUtil {

    private static final Logger log = LoggerFactory.getLogger(UpgradeUtil.class);

    private static final StringManager sm = StringManager.getManager(UpgradeUtil.class.getPackage().getName());

    private static final byte[] WS_ACCEPT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(StandardCharsets.ISO_8859_1);

    private UpgradeUtil() {
        // Utility class. Hide default constructor.
    }

    /**
     * Checks to see if this is an HTTP request that includes a valid upgrade request to web socket.
     * <p>
     * Note: RFC 2616 does not limit HTTP upgrade to GET requests but the Java WebSocket spec 1.0, section 8.2 implies such a limitation and RFC 6455 section 4.1 requires that a WebSocket Upgrade uses GET.
     *
     * @param request
     *            The request to check if it is an HTTP upgrade request for a WebSocket connection
     * @param response
     *            The response associated with the request
     * @return <code>true</code> if the request includes a HTTP Upgrade request for the WebSocket protocol, otherwise <code>false</code>
     */
    public static boolean isWebSocketUpgradeRequest(ServletRequest request, ServletResponse response) {
        if (log.isTraceEnabled()) {
            List<String> headers = new ArrayList<>();
            Enumeration<String> en = ((HttpServletRequest) request).getHeaderNames();
            while (en.hasMoreElements()) {
                headers.add(en.nextElement());
            }
            log.trace("Headers: {}", headers);
        }
        log.debug("isWebSocketUpgradeRequest: {}", Constants.UPGRADE_HEADER_VALUE.equalsIgnoreCase(((HttpServletRequest) request).getHeader(Constants.UPGRADE_HEADER_NAME)));
        return ((request instanceof HttpServletRequest) && (response instanceof HttpServletResponse) && Constants.UPGRADE_HEADER_VALUE.equalsIgnoreCase(((HttpServletRequest) request).getHeader(Constants.UPGRADE_HEADER_NAME)));
    }

    /**
     * Handles the WebSocket upgrade request, validates it, negotiates protocols and extensions,
     * and establishes the WebSocket connection.
     *
     * @param sc          The WebSocket server container that manages the lifecycle of WebSocket endpoints.
     * @param req         The HTTP servlet request containing the WebSocket upgrade request.
     * @param resp        The HTTP servlet response used to send the WebSocket handshake response.
     * @param sec         The server endpoint configuration associated with the WebSocket endpoint.
     * @param pathParams  A map containing variables extracted from the WebSocket endpoint path template.
     * @throws ServletException If an error occurs during the WebSocket session initialization.
     * @throws IOException      If an I/O error occurs during the WebSocket handshake process.
     */
    public static void doUpgrade(DefaultWsServerContainer sc, HttpServletRequest req, HttpServletResponse resp, ServerEndpointConfig sec, Map<String, String> pathParams) throws ServletException, IOException {
        log.debug("doUpgrade - sc: {} sec: {} params: {}", sc, sec, pathParams);

        // validate initial request
        validateRequestAndUpgradeHeaders(req, resp);

        // extract WebSocket key
        String key = validateWebSocketKey(req, resp);
        if (key == null) {
            return; // Key validation failed, response already sent
        }

        // validate origin
        if (!validateOrigin(req, resp, sec)) {
            return; // Origin validation failed, response already sent
        }

        // negotiate Sub-protocols
        String subProtocol = negotiateSubProtocol(req, sec);

        // validate and handle extensions
        List<Transformation> transformations = handleExtensions(req, resp, sec);
        if (transformations == null) {
            return; // Extension validation failed
        }

        // build the transformation pipeline
        Transformation transformation = buildTransformationPipeline(transformations);

        // validate RSV bits of the transformation pipeline
        validateRsvBits(transformation);

        // convert transformations to extensions
        List<Extension> negotiatedExtensions = transformations.stream().map(Transformation::getExtensionResponse).collect(Collectors.toList());

        // prepare response headers
        prepareResponseHeaders(resp, key, subProtocol, transformations);

        // finalize WebSocket handshake and establish session
        finalizeWebSocketSession(sc, req, resp, sec, pathParams, key, subProtocol, negotiatedExtensions, transformation);

        log.debug("WebSocket upgrade and session initialization completed.");
    }

    /**
     * Validates the WebSocket upgrade request headers and ensures they contain the required tokens.
     * If the headers are invalid, an appropriate error is sent in the response and an exception is thrown.
     *
     * @param req  The HttpServletRequest containing the incoming request headers.
     * @param resp The HttpServletResponse used to send error responses if validation fails.
     * @throws IOException If an I/O error occurs while sending the response.
     */
    private static void validateRequestAndUpgradeHeaders(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!headerContainsToken(req, Constants.CONNECTION_HEADER_NAME, Constants.CONNECTION_HEADER_VALUE)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'Connection' header token");
            throw new IllegalArgumentException("Missing or invalid 'Connection' header");
        }

        if (!headerContainsToken(req, Constants.WS_VERSION_HEADER_NAME, Constants.WS_VERSION_HEADER_VALUE)) {
            resp.setStatus(426); // Upgrade Required
            resp.setHeader(Constants.WS_VERSION_HEADER_NAME, Constants.WS_VERSION_HEADER_VALUE);
            throw new IllegalArgumentException("Missing or invalid 'Sec-WebSocket-Version' header");
        }
    }

    /**
     * Validates the WebSocket key by checking the presence of the 'Sec-WebSocket-Key' header in the request.
     * If the header is missing or empty, sends an error response to the client and logs the issue.
     *
     * @param req  the HttpServletRequest object containing the WebSocket request
     * @param resp the HttpServletResponse object used to send error responses if validation fails
     * @return the value of the 'Sec-WebSocket-Key' header if validation succeeds, or null if validation fails
     * @throws IOException if an I/O error occurs while sending the error response
     */
    private static String validateWebSocketKey(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String key = req.getHeader(Constants.WS_KEY_HEADER_NAME);
        if (key == null || key.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'Sec-WebSocket-Key' header");
            log.error("WebSocket key is missing");
            return null;
        }
        return key;
    }

    /**
     * Validates the origin of the HTTP request for the WebSocket connection.
     * This ensures that the origin header value is allowed as per the server's configuration.
     * If the origin is invalid, a 403 Forbidden response is sent, and the upgrade request is rejected.
     *
     * @param req The HTTP servlet request containing the WebSocket upgrade request.
     * @param resp The HTTP servlet response to send error responses, if necessary.
     * @param sec The server endpoint configuration for the WebSocket endpoint, used to validate the origin.
     * @return {@code true} if the origin is valid and the upgrade request can proceed, otherwise {@code false}.
     * @throws IOException If an I/O error occurs while sending the error response for an invalid origin.
     */
    private static boolean validateOrigin(HttpServletRequest req, HttpServletResponse resp, ServerEndpointConfig sec) throws IOException {
        String origin = req.getHeader(Constants.ORIGIN_HEADER_NAME);
        if (!sec.getConfigurator().checkOrigin(origin)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid origin");
            log.warn("WebSocket origin rejected: {}", origin);
            return false;
        }
        return true;
    }

    /**
     * Negotiates the sub-protocol to be used in a WebSocket connection based on the values provided
     * in the HTTP request and the server's endpoint configuration.
     *
     * @param req The incoming HTTP request containing potential sub-protocol options in its headers.
     * @param sec The server endpoint configuration containing the list of supported sub-protocols
     *            and the configurator responsible for negotiation.
     * @return The agreed-upon WebSocket sub-protocol as a string, or an empty string if no match is found.
     */
    private static String negotiateSubProtocol(HttpServletRequest req, ServerEndpointConfig sec) {
        List<String> subProtocols = getTokensFromHeader(req, Constants.WS_PROTOCOL_HEADER_NAME);
        return sec.getConfigurator().getNegotiatedSubprotocol(sec.getSubprotocols(), subProtocols);
    }

    /**
     * Handles the negotiation and processing of WebSocket extensions during the upgrade process.
     *
     * @param req The HTTP servlet request representing the WebSocket upgrade request.
     * @param resp The HTTP servlet response used to send headers back to the client.
     * @param sec The server endpoint configuration containing server-side WebSocket settings.
     * @return A list of transformations corresponding to the negotiated WebSocket extensions.
     * @throws IOException If an I/O error occurs during the processing of extensions.
     */
    private static List<Transformation> handleExtensions(HttpServletRequest req, HttpServletResponse resp, ServerEndpointConfig sec) throws IOException {
        List<Extension> extensionsRequested = getRequestedExtensions(req);

        List<Extension> installedExtensions = new ArrayList<>();
        installedExtensions.addAll(sec.getExtensions());
        installedExtensions.addAll(Constants.INSTALLED_EXTENSIONS);

        List<Extension> negotiatedExtensions = sec.getConfigurator().getNegotiatedExtensions(installedExtensions, extensionsRequested);

        List<Transformation> transformations = createTransformations(negotiatedExtensions);
        if (transformations.isEmpty()) {
            resp.setHeader(Constants.WS_EXTENSIONS_HEADER_NAME, "");
        }

        return transformations;
    }

    /**
     * Parses the WebSocket extension headers from the given HTTP servlet request
     * and constructs a list of requested extensions.
     *
     * @param req the {@link HttpServletRequest} from which to extract WebSocket extension headers
     * @return a list of {@link Extension} objects representing the requested WebSocket extensions
     */
    private static List<Extension> getRequestedExtensions(HttpServletRequest req) {
        List<Extension> extensionsRequested = new ArrayList<>();
        Enumeration<String> extHeaders = req.getHeaders(Constants.WS_EXTENSIONS_HEADER_NAME);
        while (extHeaders.hasMoreElements()) {
            Util.parseExtensionHeader(extensionsRequested, extHeaders.nextElement());
        }
        return extensionsRequested;
    }

    /**
     * Constructs a linked transformation pipeline from a list of individual transformations.
     *
     * @param transformations A list of {@code Transformation} objects to link into a pipeline.
     *                         The order of transformations in the list defines the sequence in
     *                         which they are applied.
     * @return The head of the linked transformation pipeline, or {@code null} if the input
     *         list is empty.
     */
    private static Transformation buildTransformationPipeline(List<Transformation> transformations) {
        Transformation transformation = null;
        for (Transformation t : transformations) {
            if (transformation == null) {
                transformation = t;
            } else {
                transformation.setNext(t);
            }
        }
        return transformation;
    }

    /**
     * Validates the RSV (Reserved) bits in the given transformation.
     * Throws a {@link ServletException} if the validation fails.
     *
     * @param transformation The transformation to validate. If the transformation is not null and
     *                        the RSV bits are incompatible, a {@link ServletException} is thrown.
     * @throws ServletException If the RSV bits validation indicates incompatibility or if the transformation is null.
     */
    private static void validateRsvBits(Transformation transformation) throws ServletException {
        if (transformation != null && !transformation.validateRsvBits(0)) {
            throw new ServletException(sm.getString("upgradeUtil.incompatibleRsv"));
        }
    }

    /**
     * Prepares the HTTP response headers required for the WebSocket handshake.
     *
     * @param resp            The HttpServletResponse object used to set the headers.
     * @param key             The WebSocket key extracted from the client request, used to generate the WebSocket accept value.
     * @param subProtocol     The subprotocol negotiated during the WebSocket handshake. Can be null or empty if no subprotocol is selected.
     * @param transformations The list of transformations (extensions) negotiated during the WebSocket handshake. Used to build the extensions header.
     */
    private static void prepareResponseHeaders(HttpServletResponse resp, String key, String subProtocol, List<Transformation> transformations) {
        resp.setHeader(Constants.UPGRADE_HEADER_NAME, Constants.UPGRADE_HEADER_VALUE);
        resp.setHeader(Constants.CONNECTION_HEADER_NAME, Constants.CONNECTION_HEADER_VALUE);
        resp.setHeader(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, getWebSocketAccept(key));

        if (subProtocol != null && !subProtocol.isEmpty()) {
            resp.setHeader(Constants.WS_PROTOCOL_HEADER_NAME, subProtocol);
        }

        if (!transformations.isEmpty()) {
            resp.setHeader(Constants.WS_EXTENSIONS_HEADER_NAME, buildExtensionsHeader(transformations));
        }
    }

    /**
     * Builds the response header for negotiated WebSocket extensions.
     *
     * @param transformations The list of transformations representing negotiated extensions.
     * @return A comma-separated string suitable for the WebSocket handshake response header.
     */
    private static String buildExtensionsHeader(List<Transformation> transformations) {
        if (transformations == null || transformations.isEmpty()) {
            return "";
        }

        StringBuilder responseHeader = new StringBuilder();
        boolean first = true;

        for (Transformation transformation : transformations) {
            if (!first) {
                responseHeader.append(',');
            }
            first = false;
            append(responseHeader, transformation.getExtensionResponse());
        }

        return responseHeader.toString();
    }

    private static void finalizeWebSocketSession(DefaultWsServerContainer sc, HttpServletRequest req, HttpServletResponse resp, ServerEndpointConfig sec, Map<String, String> pathParams, String key, String subProtocol, List<Extension> negotiatedExtensions, Transformation transformation) throws ServletException, IOException {
        WsHandshakeRequest wsRequest = new WsHandshakeRequest(req, pathParams);
        WsHandshakeResponse wsResponse = new WsHandshakeResponse();
        WsPerSessionServerEndpointConfig perSessionServerEndpointConfig = new WsPerSessionServerEndpointConfig(sec);

        sec.getConfigurator().modifyHandshake(perSessionServerEndpointConfig, wsRequest, wsResponse);
        wsRequest.finished();

        for (Entry<String, List<String>> entry : wsResponse.getHeaders().entrySet()) {
            for (String headerValue : entry.getValue()) {
                resp.addHeader(entry.getKey(), headerValue);
            }
        }

        Endpoint ep;
        try {
            ep = createEndpointInstance(sec, pathParams);
        } catch (InstantiationException e) {
            throw new ServletException("Failed to instantiate Endpoint for WebSocket session", e);
        }

        WsHttpUpgradeHandler wsHandler = req.upgrade(WsHttpUpgradeHandler.class);
        SessionConfig sessionConfig = new SessionConfig(wsRequest, negotiatedExtensions, subProtocol, transformation, pathParams, req.isSecure());

        wsHandler.preInit(ep, perSessionServerEndpointConfig, sc, sessionConfig);
    }

    /**
     * Creates an instance of the Endpoint based on the ServerEndpointConfig.
     */
    private static Endpoint createEndpointInstance(ServerEndpointConfig sec, Map<String, String> pathParams) throws InstantiationException {
        try {
            Class<?> endpointClass = sec.getEndpointClass();

            if (Endpoint.class.isAssignableFrom(endpointClass)) {
                return (Endpoint) sec.getConfigurator().getEndpointInstance(endpointClass);
            } else {
                PojoEndpointServer endpoint = new PojoEndpointServer(pathParams, endpointClass);
                sec.getUserProperties().put("org.apache.tomcat.websocket.pojo.PojoEndpoint.pathParams", pathParams);

                return endpoint;
            }
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new InstantiationException("Failed to create WebSocket Endpoint instance: " + e.getMessage());
        }
    }

    private static List<Transformation> createTransformations(List<Extension> negotiatedExtensions) {
        TransformationFactory factory = TransformationFactory.getInstance();
        LinkedHashMap<String, List<List<Extension.Parameter>>> extensionPreferences = new LinkedHashMap<>();
        // Result will likely be smaller than this
        List<Transformation> result = new ArrayList<>(negotiatedExtensions.size());
        for (Extension extension : negotiatedExtensions) {
            List<List<Extension.Parameter>> preferences = extensionPreferences.get(extension.getName());
            if (preferences == null) {
                preferences = new ArrayList<>();
                extensionPreferences.put(extension.getName(), preferences);
            }
            preferences.add(extension.getParameters());
        }
        for (Map.Entry<String, List<List<Extension.Parameter>>> entry : extensionPreferences.entrySet()) {
            Transformation transformation = factory.create(entry.getKey(), entry.getValue(), true);
            if (transformation != null) {
                result.add(transformation);
            }
        }
        return result;
    }

    private static void append(StringBuilder sb, Extension extension) {
        if (extension == null || extension.getName() == null || extension.getName().length() == 0) {
            return;
        }
        sb.append(extension.getName());
        for (Extension.Parameter p : extension.getParameters()) {
            sb.append(';');
            sb.append(p.getName());
            if (p.getValue() != null) {
                sb.append('=');
                sb.append(p.getValue());
            }
        }
    }

    /*
     * This only works for tokens. Quoted strings need more sophisticated parsing.
     */
    private static boolean headerContainsToken(HttpServletRequest req, String headerName, String target) {
        Enumeration<String> headers = req.getHeaders(headerName);
        //log.debug("headerContainsToken - header name: {} target: {} headers: {}", headerName, target, headers);
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            String[] tokens = header.split(",");
            for (String token : tokens) {
                if (target.equalsIgnoreCase(token.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * This only works for tokens. Quoted strings need more sophisticated parsing.
     */
    private static List<String> getTokensFromHeader(HttpServletRequest req, String headerName) {
        log.debug("getTokensFromHeader - header name: {}", headerName);
        List<String> result = new ArrayList<>();
        Enumeration<String> headers = req.getHeaders(headerName);
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            String[] tokens = header.split(",");
            for (String token : tokens) {
                result.add(token.trim());
            }
        }
        return result;
    }

    private static String getWebSocketAccept(String key) {
        log.debug("getWebSocketAccept: {}", key);
        byte[] digest = ConcurrentMessageDigest.digestSHA1(key.getBytes(StandardCharsets.ISO_8859_1), WS_ACCEPT);
        return Base64.encodeBase64String(digest);
    }

}
