package org.red5.server.net.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RTMPDecodeStateTest {

    @Test
    void testBufferDecoding() {
        RTMPDecodeState decodeState = new RTMPDecodeState("test-session-1");
        decodeState.bufferDecoding(10);

        Assertions.assertEquals(10, decodeState.getDecoderBufferAmount());
        Assertions.assertFalse(decodeState.canStartDecoding(5));
        Assertions.assertFalse(decodeState.canContinueDecoding());
    }

    @Test
    void testBufferDecodingZero() {
        RTMPDecodeState decodeState = new RTMPDecodeState("test-session-2");
        decodeState.bufferDecoding(0);

        Assertions.assertEquals(0, decodeState.getDecoderBufferAmount());
        Assertions.assertTrue(decodeState.canStartDecoding(5));
        Assertions.assertFalse(decodeState.canContinueDecoding());
    }

    @Test
    void testBufferDecodingNegative() {
        RTMPDecodeState decodeState = new RTMPDecodeState("test-session-3");
        decodeState.bufferDecoding(-10);

        Assertions.assertEquals(-10, decodeState.getDecoderBufferAmount());
        Assertions.assertTrue(decodeState.canStartDecoding(5));
        Assertions.assertFalse(decodeState.canContinueDecoding());
    }

    @Test
    void testDecodingFlowStart() {
        RTMPDecodeState decodeState = new RTMPDecodeState("test-session-4");
        decodeState.bufferDecoding(15);
        Assertions.assertEquals(15, decodeState.getDecoderBufferAmount());
        Assertions.assertFalse(decodeState.canStartDecoding(10));
        decodeState.startDecoding();
        Assertions.assertEquals(0, decodeState.getDecoderBufferAmount());
        Assertions.assertTrue(decodeState.hasDecodedObject());
    }

    @Test
    void testDecodingAfterBuffering() {
        RTMPDecodeState decodeState = new RTMPDecodeState("test-session-5");
        decodeState.bufferDecoding(20);
        Assertions.assertEquals(20, decodeState.getDecoderBufferAmount());
        decodeState.startDecoding();
        Assertions.assertTrue(decodeState.hasDecodedObject());
        decodeState.continueDecoding();
        Assertions.assertTrue(decodeState.canContinueDecoding());
    }

    @Test
    void testDecodingStop() {
        RTMPDecodeState decodeState = new RTMPDecodeState("test-session-6");
        decodeState.bufferDecoding(5);
        decodeState.stopDecoding();
        Assertions.assertFalse(decodeState.canContinueDecoding());
    }

}