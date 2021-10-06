package org.team54.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-06 11:37
 */
public class CharsetConvertor {
    private static final String UTF_8 = "UTF-8";
    private static final CharsetEncoder encoder = Charset.forName(UTF_8).newEncoder();
    private static final CharsetDecoder decoder = Charset.forName(UTF_8).newDecoder();

    public static ByteBuffer encode(CharBuffer in) throws CharacterCodingException {
        return encoder.encode(in);
    }

    public static CharBuffer decode(ByteBuffer in) throws CharacterCodingException{
        return decoder.decode(in);
    }

    public static String stringToUtf8(String str) {
        return new String(str.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}