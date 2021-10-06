package org.team54.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.team54.utils.CharsetConvertor;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description model for encapsulate connection
 * @create 2021-10-06 12:30
 */
@Data
@Builder
@NoArgsConstructor
public class PeerConnection {
    private Peer peer;
    private SocketChannel socketChannel;

    /**
     * send text msg to this peer's socket channel
     * @param text text msg
     */
    public void sendTextMsgToMe(String text) {
        if (socketChannel != null) {
            try {
                socketChannel.write(CharsetConvertor.encode(
                        CharBuffer.wrap(text)
                ));

            } catch (IOException e) {
                System.out.println("err in sentTextMsgToMe");
                e.printStackTrace();
            }
        }
    }

    /**
     * close the channel
     */
    public void closeMe() throws IOException {
        socketChannel.close();
    }
}