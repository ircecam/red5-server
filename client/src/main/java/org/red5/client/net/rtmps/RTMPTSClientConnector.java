/*
 * RED5 Open Source Flash Server - https://github.com/Red5/ Copyright 2006-2015 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.client.net.rtmps;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.client.net.rtmp.OutboundHandshake;
import org.red5.client.net.rtmp.RTMPClientConnManager;
import org.red5.client.net.rtmpt.RTMPTClientConnection;
import org.red5.client.net.rtmpt.RTMPTClientConnector;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.util.HttpConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client connector for RTMPT/S (RTMPS Tunneled)
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPTSClientConnector extends RTMPTClientConnector {

    private static final Logger log = LoggerFactory.getLogger(RTMPTSClientConnector.class);

    {
        httpClient = HttpConnectionUtil.getSecureClient();
    }

    public RTMPTSClientConnector(String server, int port, RTMPTSClient client) {
        targetHost = new HttpHost(server, port, "https");
        this.client = client;
    }

    /**
     * Executes the main loop that handles the RTMPTS client connection lifecycle.
     */
    @Override
    public void run() {
        HttpPost post = null;
        try {
            RTMPTClientConnection conn = openConnection();
            client.setConnection((RTMPConnection) conn);
            Red5.setConnectionLocal(conn);

            while (shouldContinue(conn)) {
                post = createRequest(conn);
                HttpResponse response = httpClient.execute(targetHost, post);

                validateResponse(response);
                IoBuffer data = processResponse(response);

                handleMessage(conn, data);
            }

            finalizeConnection();
            client.connectionClosed(conn);
        } catch (Throwable e) {
            handleRunException(post, e);
        } finally {
            Red5.setConnectionLocal(null);
        }
    }

    /**
     * Establishes and initializes a new RTMPT client connection by sending an HTTP request
     *
     * @return A newly initialized instance of {@link RTMPTClientConnection}.
     * @throws IOException If an I/O error occurs during the connection process.
     */
    private RTMPTClientConnection openConnection() throws IOException {
        HttpPost openPost = getPost("/open/1");
        setCommonHeaders(openPost);
        openPost.addHeader("Content-Type", CONTENT_TYPE);
        openPost.setEntity(ZERO_REQUEST_ENTITY);

        HttpResponse response = httpClient.execute(targetHost, openPost);
        validateResponse(response);

        return initializeConnection(response);
    }

    /**
     * Determines whether the connection should continue operating based on its closing status
     * and a stop request condition.
     *
     * @param conn the RTMPTClientConnection to check for its status
     * @return true if the connection is not closing and no stop has been requested, false otherwise
     */
    private boolean shouldContinue(RTMPTClientConnection conn) {
        return !conn.isClosing() && !stopRequested;
    }

    /**
     * Creates an HTTP POST request for the provided RTMPTClientConnection.
     *
     * @param conn the RTMPTClientConnection instance used to determine pending messages
     *             and create the appropriate HTTP POST request
     * @return an HttpPost object representing the created request, ready to be executed
     */
    private HttpPost createRequest(RTMPTClientConnection conn) {
        HttpPost post;

        IoBuffer toSend = conn.getPendingMessages(SEND_TARGET_SIZE);
        if (hasPendingMessages(toSend)) {
            post = makePost("send");
            post.setEntity(new InputStreamEntity(toSend.asInputStream(), toSend.limit()));
        } else {
            post = makePost("idle");
            post.setEntity(ZERO_REQUEST_ENTITY);
        }
        post.addHeader("Content-Type", CONTENT_TYPE);
        return post;
    }

    /**
     * Checks if there are pending messages to be sent based on the provided {@link IoBuffer}.
     *
     * @param toSend the {@link IoBuffer} containing the data to be sent. It may be null.
     * @return {@code true} if the provided {@link IoBuffer} is not null and contains data (limit greater than 0),
     *         otherwise {@code false}.
     */
    private boolean hasPendingMessages(IoBuffer toSend) {
        return toSend != null && toSend.limit() > 0;
    }

    /**
     * Validates the HTTP response by checking its status code.
     *
     * @param response the HTTP response to validate
     * @throws IOException if an I/O error occurs during response validation
     */
    private void validateResponse(HttpResponse response) throws IOException {
        checkResponseCode(response);
    }

    /**
     * Processes the HTTP response and converts the response entity into an IoBuffer.
     *
     * @param response the HTTP response to process, containing the entity to be converted
     * @return an IoBuffer wrapping the byte array extracted from the HTTP response entity
     * @throws IOException if an error occurs while reading the response entity
     */
    private IoBuffer processResponse(HttpResponse response) throws IOException {
        byte[] received = EntityUtils.toByteArray(response.getEntity());
        return IoBuffer.wrap(received);
    }

    /**
     * Processes the incoming message data for the specified connection.
     *
     * @param conn the RTMPT client connection for which the message is being handled
     * @param data the incoming message data to be processed
     */
    private void handleMessage(RTMPTClientConnection conn, IoBuffer data) {
        if (isHandshakeDone(conn)) {
            client.messageReceived(data);
        } else {
            handleDecodedMessages(conn, data);
        }
    }

    /**
     * Checks if the handshake has been completed for the specified RTMPT client connection.
     *
     * @param conn the RTMPT client connection object to check for handshake completion
     * @return {@code true} if the handshake is complete, otherwise {@code false}
     */
    private boolean isHandshakeDone(RTMPTClientConnection conn) {
        return conn.hasAttribute(RTMPConnection.RTMP_HANDSHAKE);
    }

    /**
     * Handles the processing of decoded messages from a provided connection and data buffer.
     *
     * @param conn the RTMPT client connection used for decoding and processing messages
     * @param data the IoBuffer containing data to be decoded
     */
    private void handleDecodedMessages(RTMPTClientConnection conn, IoBuffer data) {
        if (data.limit() > 0) {
            data.skip(1);
        }
        List<?> messages = conn.decode(data);
        if (messages == null || messages.isEmpty()) {
            sleepForPolling();
        } else {
            processMessages(messages);
        }
    }

    /**
     * Causes the current thread to sleep for a short duration, typically used for polling or to introduce a delay
     * in operations where no immediate processing is necessary.
     */
    private void sleepForPolling() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Processes a list of messages by invoking the client's messageReceived method for
     * each individual message. Logs an error if message processing fails.
     *
     * @param messages a list of messages to be processed
     */
    private void processMessages(List<?> messages) {
        for (Object message : messages) {
            try {
                client.messageReceived(message);
            } catch (Exception e) {
                log.error("Could not process message", e);
            }
        }
    }

    /**
     * Handles exceptions that occur while running the process. Logs the exception,
     * delegates the handling to the client, and aborts the HTTP request if provided.
     *
     * @param post the HTTP POST request that may need to be aborted if an
     *             exception occurs; can be null
     * @param e    the exception or error that occurred
     */
    private void handleRunException(HttpPost post, Throwable e) {
        log.debug("RTMPT handling exception", e);
        client.handleException(e);
        if (post != null) {
            post.abort();
        }
    }

    /**
     * Initializes an RTMPTClientConnection using the provided HTTP response.
     *
     * @param response the HTTP response from which the session ID is extracted
     * @return an initialized RTMPTClientConnection if the HTTP response contains an entity,
     *         otherwise returns null
     * @throws IOException if an error occurs while processing the HTTP response
     */
    private RTMPTClientConnection initializeConnection(HttpResponse response) throws IOException {
        RTMPTClientConnection conn = null;
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String sessionId = extractSessionId(EntityUtils.toString(entity));
            conn = createRTMPConnection(sessionId);
            initConnection(conn);
        }
        return conn;
    }

    /**
     * Extracts the session ID from the provided response string.
     *
     * @param responseStr the response string from which the session ID is to be extracted
     * @return the extracted session ID as a string
     */
    private String extractSessionId(String responseStr) {
        sessionId = responseStr.substring(0, responseStr.length() - 1);
        log.debug("Got an id {}", sessionId);
        return sessionId;
    }

    /**
     * Creates and returns an instance of {@link RTMPTClientConnection} associated with the specified session ID.
     *
     * @param sessionId the session ID to be associated with the created connection
     * @return an instance of {@link RTMPTClientConnection} initialized with the provided session ID
     */
    private RTMPTClientConnection createRTMPConnection(String sessionId) {
        return (RTMPTClientConnection) RTMPClientConnManager.getInstance()
                .createConnection(RTMPTClientConnection.class, sessionId);
    }

    /**
     * Initializes the RTMPT client connection by setting up the handler, decoder,
     * encoder, and performing the initial handshake.
     *
     * @param conn the RTMPT client connection instance to initialize
     */
    private void initConnection(RTMPTClientConnection conn) {
        conn.setHandler(client);
        conn.setDecoder(client.getDecoder());
        conn.setEncoder(client.getEncoder());

        OutboundHandshake outgoingHandshake = new OutboundHandshake();
        outgoingHandshake.setHandshakeType(RTMPConnection.RTMP_NON_ENCRYPTED);
        conn.setAttribute(RTMPConnection.RTMP_HANDSHAKE, outgoingHandshake);

        IoBuffer handshake = outgoingHandshake.generateClientRequest1();
        conn.writeRaw(handshake);
    }
}
