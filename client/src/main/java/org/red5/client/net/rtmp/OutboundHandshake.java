/*
 * RED5 Open Source Flash Server - https://github.com/Red5/ Copyright 2006-2015 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.client.net.rtmp;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.bouncycastle.util.BigIntegers;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.util.FileUtil;

/**
 * Performs handshaking for client connections.
 *
 * @author Paul Gregoire
 */
public class OutboundHandshake extends RTMPHandshake {

    private byte[] outgoingDigest = new byte[DIGEST_LENGTH];

    private byte[] incomingDigest = new byte[DIGEST_LENGTH];

    private byte[] swfHash;

    private int digestPosClient;

    private int digestPosServer;

    // client initial request C1
    private byte[] c1 = null;

    // server initial response S1
    private byte[] s1 = null;

    // whether or not verification is mandatory
    private boolean forceVerification;

    public OutboundHandshake() {
        super(RTMPConnection.RTMP_NON_ENCRYPTED);
    }

    public OutboundHandshake(byte handshakeType) {
        super(handshakeType);
    }

    public OutboundHandshake(byte handshakeType, int algorithm) {
        this(handshakeType);
        this.algorithm = algorithm;
    }

    @Override
    public IoBuffer doHandshake(IoBuffer input) {
        throw new UnsupportedOperationException("Not used, call server response decoders directly");
    }

    /**
     * Creates the servers handshake bytes
     */
    @Override
    protected void createHandshakeBytes() {
        log.trace("createHandshakeBytes");
        BigInteger bi = new BigInteger((Constants.HANDSHAKE_SIZE * 8), random);
        handshakeBytes = BigIntegers.asUnsignedByteArray(bi);
        if (handshakeBytes.length < Constants.HANDSHAKE_SIZE) {
            ByteBuffer b = ByteBuffer.allocate(Constants.HANDSHAKE_SIZE);
            b.put(handshakeBytes);
            b.put((byte) 0x13);
            b.flip();
            handshakeBytes = b.array();
        }
    }

    /**
     * Generates the C2 response bytes for the handshake process. 
     *
     * @param s1 the server's first handshake response (S1) used as input for digest calculation
     * @return the generated C2 response as an IoBuffer
     */
    private IoBuffer generateC2Response(byte[] s1) {
        BigInteger bi = new BigInteger(Constants.HANDSHAKE_SIZE * 8, random);
        byte[] c2 = BigIntegers.asUnsignedByteArray(bi);
        
        byte[] digestResp = new byte[DIGEST_LENGTH];
        byte[] signatureResp = new byte[DIGEST_LENGTH];
        calculateHandshakeDigest(s1, digestPosServer, GENUINE_FP_KEY, digestResp);
        calculateHMAC_SHA256(c2, 0, Constants.HANDSHAKE_SIZE - DIGEST_LENGTH, digestResp, DIGEST_LENGTH, signatureResp, 0);

        log.debug("Calculated digest key from secure key and server digest: {}", Hex.encodeHexString(digestResp));
        
        log.debug("Client signature calculated: {}", Hex.encodeHexString(signatureResp));
        
        IoBuffer response = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
        response.put(c2, 0, Constants.HANDSHAKE_SIZE - DIGEST_LENGTH);
        response.put(signatureResp);
        response.flip();

        return response;
    }

    /**
     * Create the first part of the outgoing connection request (C0 and C1).
     * <pre>
     * C0 = 0x03 (client handshake type - 0x03, 0x06, 0x08, or 0x09)
     * C1 = 1536 bytes from the client
     * </pre>
     * @return outgoing handshake C0+C1
     */
    public IoBuffer generateClientRequest1() {
        log.debug("generateClientRequest1");
        IoBuffer request = IoBuffer.allocate(Constants.HANDSHAKE_SIZE + 1);
        request.put(handshakeType);
        if (useEncryption() || swfSize > 0) {
            fp9Handshake = true;
            algorithm = 1;
            handleEncryptionSetup();
        } else {
            log.debug("Non-encrypted handshake in use.");
        }

        initHandshakeBytes();
        c1 = new byte[Constants.HANDSHAKE_SIZE];

        if (fp9Handshake) {
            digestPosClient = getDigestOffset(algorithm, handshakeBytes, 0);
            calculateHandshakeDigest(handshakeBytes, digestPosClient, GENUINE_FP_KEY, c1);
            System.arraycopy(c1, digestPosClient, outgoingDigest, 0, DIGEST_LENGTH);
            log.debug("Generated client digest: {}", Hex.encodeHexString(outgoingDigest));
        }

        if (log.isTraceEnabled()) {
            log.trace("C1 handshake data: {}", Hex.encodeHexString(c1));
        }

        request.put(c1);
        request.flip();
        handshakeBytes = null;
        return request;
    }

    /**
     * Initialise les bytes nécessaires pour le handshake C1 avec des données de timestamp et de version.
     */
    private void initHandshakeBytes() {
        int timestamp = 5;
        handshakeBytes[0] = (byte) (timestamp >>> 24);
        handshakeBytes[1] = (byte) (timestamp >>> 16);
        handshakeBytes[2] = (byte) (timestamp >>> 8);
        handshakeBytes[3] = (byte) timestamp;

        if (fp9Handshake) {
            handshakeBytes[4] = (byte) 0x80; 
            handshakeBytes[5] = 0;        
            handshakeBytes[6] = 7;        
            handshakeBytes[7] = 2;       
        } else {
            log.debug("Utilisation d'un handshake pré-v9.0.115.0");
            handshakeBytes[4] = 0;
            handshakeBytes[5] = 0;
            handshakeBytes[6] = 0;
            handshakeBytes[7] = 0;
        }

        if (log.isTraceEnabled()) {
            log.trace("Bytes de handshake initialisés : {}", Hex.encodeHexString(Arrays.copyOf(handshakeBytes, 8)));
        }
    }

    /**
     * Decodes the server's initial handshake response (S1) during the RTMP handshake process.
     *
     * @param in the input buffer containing the server's handshake response (S1)
     * @return an IoBuffer instance representing the processed handshake response or null if decoding fails
     */
    public IoBuffer decodeServerResponse1(IoBuffer in) {
        log.debug("decodeServerResponse1");
        IoBuffer response = null;
        s1 = new byte[Constants.HANDSHAKE_SIZE];
        in.get(s1);

        if (log.isDebugEnabled()) {
            log.debug("Server version {}", Hex.encodeHexString(Arrays.copyOfRange(s1, 4, 8)));
        }

        if (fp9Handshake && handshakeType == RTMPConnection.RTMP_NON_ENCRYPTED && s1[4] == 0) {
            log.debug("Switching to pre-fp9 handshake");
            fp9Handshake = false;
        }

        if (fp9Handshake) {
            if (!getServerDigestPosition()) {
                log.warn("Server digest position is invalid");
                return null;
            }
            System.arraycopy(s1, digestPosServer, incomingDigest, 0, DIGEST_LENGTH);
            log.debug("Server digest: {}", Hex.encodeHexString(incomingDigest));

            if (swfSize > 0) {
                calculateSwfVerification(s1, swfHash, swfSize);
            }

            if (useEncryption()) {
                initializeEncryptionKeys(s1);
            }

            response = generateC2Response(s1);
        } else {
            response = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
            response.put(s1, 0, Constants.HANDSHAKE_SIZE);
            response.flip();
        }
        return response;
    }

    /**
     * Initializes the encryption keys needed for secure data transmission during the handshake process.
     *
     * @param s1 the server's initial handshake response, used to extract the server's public key
     *           and calculate encryption keys
     */
    private void initializeEncryptionKeys(byte[] s1) {
        int serverDHOffset = getDHOffset(algorithm, s1, 0);
        log.trace("Incoming DH offset: {}", serverDHOffset);

        incomingPublicKey = new byte[KEY_LENGTH];
        System.arraycopy(s1, serverDHOffset, incomingPublicKey, 0, KEY_LENGTH);
        log.debug("Server public key: {}", Hex.encodeHexString(incomingPublicKey));

        initRC4Encryption(getSharedSecret(incomingPublicKey, keyAgreement));

        if (handshakeType == RTMPConnection.RTMP_ENCRYPTED) {
            byte[] dummyBytes = new byte[Constants.HANDSHAKE_SIZE];
            cipherIn.update(dummyBytes);
            cipherOut.update(dummyBytes);
        }
    }

    /**
     * Decodes the second server response (S2).
     * <pre>
     * S2 = Copy of C1 bytes
     * </pre>
     * @param in incoming handshake S2
     * @return true if validation passes and false otherwise
     */
    public boolean decodeServerResponse2(IoBuffer buf) {
        byte[] s2 = new byte[Constants.HANDSHAKE_SIZE];
        buf.get(s2);
        return decodeServerResponse2(s2);
    }

    /**
     * Decodes the second server response (S2) during the handshake process.
     *
     * @param s2 the server's second handshake response (S2) as a byte array
     * @return true if the server's response passes validation, false otherwise
     */
    public boolean decodeServerResponse2(byte[] s2) {
        log.debug("decodeServerResponse2");
        if (log.isTraceEnabled()) {
            log.trace("S2: {}\nC1: {}", Hex.encodeHexString(s2), Hex.encodeHexString(c1));
        }

        if (fp9Handshake) {
            if (s2[4] == 0 && s2[5] == 0 && s2[6] == 0 && s2[7] == 0) {
                log.warn("Server refused signed authentication");
            }

            boolean isValid = validateServerSignature(s2, c1, GENUINE_FMS_KEY, digestPosClient);
            if (!isValid) {
                log.info("Server not genuine");
                return false;
            } else {
                log.debug("Compatible flash server");
            }
        } else {
            if (!Arrays.equals(s2, c1)) {
                log.info("Client signature doesn't match!");
            }
        }
        return true;
    }


    /**
     * Calculates a handshake digest by computing an HMAC-SHA256 hash over the given source bytes
     * using the specified digest key and writes the result to the destination array.
     *
     * @param source the source byte array containing the data to be hashed
     * @param offset the offset in the source byte array to start reading data
     * @param digestKey the key to be used for calculating the HMAC-SHA256 hash
     * @param destination the byte array to store the resulting digest
     */
    private void calculateHandshakeDigest(byte[] source, int offset, byte[] digestKey, byte[] destination) {
        calculateHMAC_SHA256(source, offset, DIGEST_LENGTH, digestKey, digestKey.length, destination, 0);
    }


    /**
     * Handles the setup of encryption during the handshake process.
     */
    private void handleEncryptionSetup() {
        KeyPair keys = generateKeyPair();
        outgoingPublicKey = getPublicKey(keys);
        log.debug("Public key: {}", Hex.encodeHexString(outgoingPublicKey));

        int clientDHOffset = getDHOffset(algorithm, handshakeBytes, 0);
        System.arraycopy(outgoingPublicKey, 0, handshakeBytes, clientDHOffset, KEY_LENGTH);
    }

    /**
     * Validates the server's handshake signature to ensure it matches
     * the expected signature derived using the digest key and client handshake data.
     *
     * @param s2 the server's second handshake response (S2)
     * @param c1 the client's first handshake request (C1)
     * @param digestKey the key used for generating the digest
     * @param digestOffset the offset within the client's handshake data for digest calculation
     * @return true if the server's signature matches the expected signature, false otherwise
     */
    private boolean validateServerSignature(byte[] s2, byte[] c1, byte[] digestKey, int digestOffset) {
        byte[] signature = new byte[DIGEST_LENGTH];
        byte[] digest = new byte[DIGEST_LENGTH];

        calculateHandshakeDigest(c1, digestOffset, GENUINE_FMS_KEY, digest);
        calculateHMAC_SHA256(s2, 0, Constants.HANDSHAKE_SIZE - DIGEST_LENGTH, digest, DIGEST_LENGTH, signature, 0);

        return Arrays.equals(signature, Arrays.copyOfRange(s2, Constants.HANDSHAKE_SIZE - DIGEST_LENGTH, Constants.HANDSHAKE_SIZE));
    }

    /**
     * Gets and verifies the server digest.
     *
     * @return true if the server digest is found and verified, false otherwise
     */
    private boolean getServerDigestPosition() {
        boolean result = false;
        //log.trace("BigEndian bytes: {}", Hex.encodeHexString(s1));
        log.trace("Trying algorithm: {}", algorithm);
        digestPosServer = getDigestOffset(algorithm, s1, 0);
        log.debug("Server digest position offset: {}", digestPosServer);
        if (!(result = verifyDigest(digestPosServer, s1, GENUINE_FMS_KEY, 36))) {
            // try a different position
            algorithm ^= 1;
            log.trace("Trying algorithm: {}", algorithm);
            digestPosServer = getDigestOffset(algorithm, s1, 0);
            log.debug("Server digest position offset: {}", digestPosServer);
            if (!(result = verifyDigest(digestPosServer, s1, GENUINE_FMS_KEY, 36))) {
                log.warn("Server digest verification failed");
                // if we dont mind that verification routines failed
                if (!forceVerification) {
                    return true;
                }
            } else {
                log.debug("Server digest verified");
            }
        } else {
            log.debug("Server digest verified");
        }
        return result;
    }

    /**
     * Determines the validation scheme for given input.
     *
     * @param handshake the handshake bytes from the server
     * @return true if server used a supported validation scheme, false if unsupported
     */
    @Override
    public boolean validate(byte[] handshake) {
        if (validateScheme(handshake, 0)) {
            algorithm = 0;
            return true;
        }
        if (validateScheme(handshake, 1)) {
            algorithm = 1;
            return true;
        }
        log.error("Unable to validate server");
        return false;
    }

    private boolean validateScheme(byte[] handshake, int scheme) {
        int digestOffset = -1;
        switch (scheme) {
            case 0:
                digestOffset = getDigestOffset1(handshake, 0);
                break;
            case 1:
                digestOffset = getDigestOffset2(handshake, 0);
                break;
            default:
                log.error("Unknown algorithm: {}", scheme);
        }
        log.debug("Algorithm: {} digest offset: {}", scheme, digestOffset);
        byte[] tempBuffer = new byte[Constants.HANDSHAKE_SIZE - DIGEST_LENGTH];
        System.arraycopy(handshake, 0, tempBuffer, 0, digestOffset);
        System.arraycopy(handshake, digestOffset + DIGEST_LENGTH, tempBuffer, digestOffset, Constants.HANDSHAKE_SIZE - digestOffset - DIGEST_LENGTH);
        byte[] tempHash = new byte[DIGEST_LENGTH];
        calculateHMAC_SHA256(tempBuffer, 0, tempBuffer.length, GENUINE_FMS_KEY, 36, tempHash, 0);
        log.debug("Hash: {}", Hex.encodeHexString(tempHash));
        boolean result = true;
        for (int i = 0; i < DIGEST_LENGTH; i++) {
            if (handshake[digestOffset + i] != tempHash[i]) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Initialize SWF verification data.
     *
     * @param swfFilePath path to the swf file or null
     */
    public void initSwfVerification(String swfFilePath) {
        log.info("Initializing swf verification for: {}", swfFilePath);
        byte[] bytes = null;
        if (swfFilePath != null) {
            File localSwfFile = new File(swfFilePath);
            if (localSwfFile.exists() && localSwfFile.canRead()) {
                log.info("Swf file path: {}", localSwfFile.getAbsolutePath());
                bytes = FileUtil.readAsByteArray(localSwfFile);
            } else {
                bytes = "Red5 is awesome for handling non-accessable swf file".getBytes();
            }
        } else {
            bytes = new byte[42];
        }
        calculateHMAC_SHA256(bytes, 0, bytes.length, GENUINE_FP_KEY, 30, swfHash, 0);
        swfSize = bytes.length;
        log.info("Verification - size: {}, hash: {}", swfSize, Hex.encodeHexString(swfHash));
    }

    public byte[] getHandshakeBytes() {
        return c1;
    }

    public void setForceVerification(boolean forceVerification) {
        this.forceVerification = forceVerification;
    }

}
