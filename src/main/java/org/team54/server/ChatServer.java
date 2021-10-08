package org.team54.server;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.team54.messageBean.RoomChangeMessage;
import org.team54.model.Peer;
import org.team54.service.RoomMessageService;
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
    private final InetAddress hostAddress;
    private final int port;

    private InetSocketAddress localSocketAddress;

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

    private final MessageQueueWorker MQWorker;

    private final NeighborPeerManager neighborPeerManager;
    private final ChatRoomManager chatRoomManager;

    public ChatServer(InetAddress hostAddress, int port, MessageQueueWorker MQWorker) throws IOException {
        this.hostAddress = hostAddress;
        this.port = port;

        // allocated buffer
        this.readBuffer = ByteBuffer.allocate(Constants.CHANNEL_BUFFER_SIZE);

        // init selector
        this.selector = this.initSelector();
        this.MQWorker = MQWorker;
        this.neighborPeerManager = NeighborPeerManager.getInstance();
        // attach server to this manager
        this.neighborPeerManager.setChatServer(this);
        this.chatRoomManager = ChatRoomManager.getInstance();
    }

    /**
     * run server logic:
     * 1. room & neighbor management
     * 2. handle commands
     * 3. handle incoming messages and relay them
     */
    @Override
    public void run() {
        // run MQ manager
        new Thread(MQWorker).start();
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

                    // remove this duplicate
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

        SocketChannel socketChannel = null;

        try {
            // if selectionKey can be accepted, start to get connection
            if (key.isAcceptable()) {
                // get incoming channel
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

                // accept this channel
                socketChannel = serverSocketChannel.accept();
                registerChannel(selector, socketChannel, SelectionKey.OP_READ);

                // register this new connected peer into manager
                // will also check blacklist
                neighborPeerManager.registerNewSocketChannelAsNeighbor(socketChannel);

            } else if (key.isReadable()) {
                // if channel is ready to read, read from channel
                socketChannel = (SocketChannel) key.channel();

                // get incoming string
                String incomingString = readStringFromChannel(socketChannel);

                // get source peer based on socketChannel
                Peer srcPeer = neighborPeerManager.getPeerWithSocketChannel(socketChannel);

                // put in the MQ waiting for running
                // handle logic is in 'handleRequest' method that is in this ChatServer class
                MQWorker.handleIncomingTextMessage(this, srcPeer, incomingString);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            // close buggy channel
            if (socketChannel != null) {
//                socketChannel.close();
                System.out.println("[debug] closed a buggy socket");
                neighborPeerManager.handleDisconnectNeighborSocketChannel(socketChannel);
            }
        }

    }

    /**
     * Callback that handle text requests
     * <p>
     * parse incoming text request, and react to it
     *
     * @param sourcePeer
     * @param text
     */
    public void handleRequestCallback(Peer sourcePeer, String text) {
        // check peer livness, only handle living peer's request
        if (neighborPeerManager.isPeerLivingByPeerId(sourcePeer.getId())) {
            System.out.println("[debug] get request: " + text);
            JSONObject requestDataObject = JSONObject.parseObject(text);
            if (requestDataObject == null) {
                throw new JSONException("JSON object parse ERROR");
            }

            String requestType = requestDataObject.getString("type");
            if (requestType != null) {

                if (requestType.equals(Constants.MESSAGE_JSON_TYPE)) {
                    String content = requestDataObject.getString("content");
                    if (content == null) {
                        throw new JSONException("Missing attributes");
                    }

                    String relayMessage = RoomMessageService.genRelayMessage(sourcePeer.getId(), text);
                    chatRoomManager.broadcastMessageInRoom(sourcePeer
                            , sourcePeer.getRoomId()
                            , relayMessage,
                            null);

                } else if (requestType.equals(Constants.HOST_CHANGE_JSON_TYPE)) {
                    // TODO host change

                } else if (requestType.equals(Constants.JOIN_JSON_TYPE)) {
                    String roomId = requestDataObject.getString("roomid");
                    if (roomId == null) {
                        throw new JSONException("Missing attributes");
                    }
                    chatRoomManager.joinPeerToRoom(roomId, sourcePeer);

                } else if (requestType.equals(Constants.WHO_JSON_TYPE)) {
                    String roomId = requestDataObject.getString("roomid");
                    if (roomId == null) {
                        throw new JSONException("Missing attributes");
                    }

                    chatRoomManager.sendRoomContentMsgToPeer(sourcePeer, roomId);

                } else if (requestType.equals(Constants.LIST_JSON_TYPE)) {
                    chatRoomManager.sendRoomListMsgToPeer(sourcePeer);

                } else if (requestType.equals(Constants.QUIT_JSON_TYPE)) {
                    neighborPeerManager.handleDisconnectNeighborPeer(sourcePeer);

                } else if (requestType.equals(Constants.LIST_NEIGHBORS_JSON_TYPE)) {
                    // TODO list neighbors


                } else if (requestType.equals(Constants.SHOUT_JSON_TYPE)) {
                    // TODO shout

                }


            } else {
                throw new JSONException("JSON's type is not exist");
            }


        }
    }

    /**
     * register the socketChannel to a selector
     *
     * @param selector
     * @param channel
     * @param opRead
     * @throws IOException
     */
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
//            socketChannel.close();
            neighborPeerManager.handleDisconnectNeighborSocketChannel(socketChannel);

        } else {
            // switch write mode to read mode
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
        localSocketAddress = isa;
        serverChannel.socket().bind(isa);

        // Register the server socket channel, indicating an interest in
        // accepting new connections
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }

    public InetSocketAddress getLocalSocketAddress() {
        return localSocketAddress;
    }

    /**
     * get string of local listening host text: local hoststring + listening port
     * <p>
     * can be used for generate 'hostchange' message after connection
     *
     * @return String local listening host text
     */
    public String getLocalListeningHostText() {
        InetSocketAddress localSocketAddress = getLocalSocketAddress();

        return localSocketAddress.getHostString()
                + ":" + localSocketAddress.getPort();
    }

    public int getPort() {
        return port;
    }
}