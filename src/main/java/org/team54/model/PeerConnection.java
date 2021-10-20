package org.team54.model;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@NoArgsConstructor
public class PeerConnection {
    private String peerId;
    private SocketChannel socketChannel;

    /**
     * send text msg to this peer's socket channel
     * @param text text msg
     */
    public void sendTextMsgToMe(String text) {
        if (socketChannel != null) {
            try {
                if (socketChannel.isConnected()) {
                    socketChannel.write(CharsetConvertor.encode(
                            CharBuffer.wrap(text)
                    ));
                } else {
                    System.out.println(peerId + " has been disconnected, send message failed: " + text);
                }
            } catch (IOException e) {
                System.out.println("err in sentTextMsgTo " + peerId);
                e.printStackTrace();
            }
        }
    }

    /**
     * close the channel
     * if has been closed, do nothing
     */
    public void closeMe(){
        if (socketChannel != null && socketChannel.isConnected()) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                System.out.println("err in PeerConnection");
                e.printStackTrace();
            }
        }

    }
}