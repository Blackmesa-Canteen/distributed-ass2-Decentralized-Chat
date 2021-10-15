package org.team54.client;

import java.nio.channels.SocketChannel;

public class ClientDataEvent {

    public NIOClient client;
    public SocketChannel socketChannel;
    public byte[] data;

    public ClientDataEvent(NIOClient client, SocketChannel socketChannel, byte[] data) {
        this.client = client;
        this.socketChannel = socketChannel;
        this.data = data;
    }
}

