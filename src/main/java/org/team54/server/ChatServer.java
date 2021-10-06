package org.team54.server;

import org.team54.utils.CharsetConvertor;
import org.team54.utils.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description NIO server
 * @create 2021-10-06 10:48
 * <p>
 * Some codes are from tutorial 7.
 */
public class ChatServer implements Runnable {

    /**
     * The host:port combination to listen on
     */
    private InetAddress hostAddress;
    private final int port;

    private boolean isRunning = false;

    /**
     * The channel on which we'll accept connections
     */
    private ServerSocketChannel serverChannel;

    /**
     * The selector we'll be monitoring
     */
    private final Selector selector;

    /**
     * The buffer into which we'll read data when it's available
     */
    private final ByteBuffer readBuffer;

    public ChatServer(InetAddress hostAddress, int port, Selector selector) throws IOException {
        this.hostAddress = hostAddress;
        this.port = port;

        // allocated buffer
        this.readBuffer = ByteBuffer.allocate(Constants.CHANNEL_BUFFER_SIZE);

        // init selector
        this.selector = this.initSelector();

    }

    /**
     * run server logic:
     * 1. room & neighbor management
     * 2. handle commands
     * 3. handle incoming messages and relay them
     */
    @Override
    public void run() {
        listen();
    }

    /**
     * listen for incoming connection
     */
    private void listen() {
        while (true) {
            try {
                // n == number of ready channel
                int n = selector.select();
                if (n == 0) {
                    continue;
                }
                // 1 selector -> n selectionKey, 1 selectionKey -> 1 Channel
                Iterator<SelectionKey> keyIterator =
                        selector.selectedKeys().iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    handleKey(key);

                    // remove duplicates
                    keyIterator.remove();
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    /**
     * handle SelectableChannel
     *
     * @param key SelectableChannel
     * @throws IOException
     * @throws ClosedChannelException
     */
    private void handleKey(SelectionKey key) throws IOException, ClosedChannelException {
        SocketChannel acceptedChannel = null;

        try {

            // if selectionKey can be accepted, start to get connection
            if (key.isAcceptable()) {
                // get incoming channel
                ServerSocketChannel server = (ServerSocketChannel) key.channel();

                // accept this channel
                acceptedChannel = server.accept();

                // register this channel
                registerChannel(selector, acceptedChannel, SelectionKey.OP_READ);

                // if channel is ready to read, read from channel
            } else if (key.isReadable()) {
                acceptedChannel = (SocketChannel) key.channel();

                // get incoming string
                String incomingString = readStringFromChannel(acceptedChannel);

            }
        } catch (Throwable t) {
            t.printStackTrace();
            // close buggy channel
            if (acceptedChannel != null) {
                acceptedChannel.close();
            }
        }

    }

    private void registerChannel(Selector selector, SocketChannel channel, int opRead)
            throws IOException {
        if (channel == null) {
            return;
        }

        channel.configureBlocking(false);
        channel.register(selector, opRead);
    }

    /**
     * read out string from channel
     *
     * @param socketChannel
     * @return String string msg from channel
     * @throws IOException
     */
    private String readStringFromChannel(SocketChannel socketChannel) throws IOException {
        int count = 0;
        String res = null;

        readBuffer.clear();
        count = socketChannel.read(readBuffer);
        if (count < 0) {
            // When a client channel is closed, read events are always received,
            // but no message is received, that is, the read method returns -1.
            // Therefore, the server also needs to close the channel to avoid
            // infinite invalid processing.
            socketChannel.close();

        } else {
            // from write mode to read mode
            readBuffer.flip();

            // convert char to UTF-8 string
            CharBuffer charBuffer = CharsetConvertor.decode(readBuffer);
            res = charBuffer.toString();

            readBuffer.clear();
        }

        return res;
    }

    /**
     * write String to the channel
     *
     * @param channel
     * @param inputString string
     * @throws IOException
     */
    private void writeStringToChannel(SocketChannel channel, String inputString)
            throws IOException {

        channel.write(CharsetConvertor.encode(
                CharBuffer.wrap(inputString)
        ));
    }

    private Selector initSelector() throws IOException {
        // Create a new selector
        Selector socketSelector = SelectorProvider.provider().openSelector();

        // Create a new non-blocking server socket channel
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // Bind the server socket to the specified address and port
        InetSocketAddress isa = new InetSocketAddress(this.hostAddress, this.port);
        serverChannel.socket().bind(isa);

        // Register the server socket channel, indicating an interest in
        // accepting new connections
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }
}