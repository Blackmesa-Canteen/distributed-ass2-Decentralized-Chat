package org.team54.server;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.team54.client.Client;
import org.team54.model.Peer;
import org.team54.service.MessageServices;
import org.team54.utils.CharsetConvertor;
import org.team54.utils.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
    public final ChatRoomManager chatRoomManager;

    private Client localclient;

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

                // if doesn't get src Peer, it is removed peer's info, will not handle the msg
                if (srcPeer != null) {
                    // put in the MQ waiting for running
                    // handle logic is in 'handleRequest' method that is in this ChatServer class
                    MQWorker.handleIncomingTextMessage(this, srcPeer, incomingString);
                }
            }
        } catch (Throwable t) {
//            t.printStackTrace();
            // close buggy channel
            if (socketChannel != null) {
//                socketChannel.close();
//                 System.out.println("[debug] closed a buggy socket");
                neighborPeerManager.handleDisconnectNeighborSocketChannel(socketChannel);
            }
        }

    }

    /**
     * Record whether the local server peer forwards the same shout message
     */
    private final HashSet<String> shoutHashIdHistory = new HashSet<>();

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

        if (sourcePeer != null && neighborPeerManager.isPeerLivingByPeerId(sourcePeer.getIdentity())) {
//            System.out.println("[debug] server is handling a request: " + text + " , from " + sourcePeer.getIdentity());
            JSONObject requestDataObject = JSONObject.parseObject(text);

            if (requestDataObject == null) {
                throw new JSONException("JSON object parse ERROR");
            }

            String requestType = requestDataObject.getString("type");
            if (requestType != null) {
                // System.out.println("[debug] server got chat message.");
                // chat message broadcast
                if (requestType.equals(Constants.MESSAGE_JSON_TYPE)) {
                    String content = requestDataObject.getString("content");
                    if (content == null) {
                        throw new JSONException("Missing attributes");
                    }

                    String relayMessage = MessageServices.genRelayMessage(sourcePeer.getIdentity(), content);
                    chatRoomManager.broadcastMessageInRoom(sourcePeer
                            , sourcePeer.getRoomId()
                            , relayMessage,
                            null);

                    // The client sends the Hostchange information immediately after the connection to assign
                    // the Listening port information to the Peer model
                } else if (requestType.equals(Constants.HOST_CHANGE_JSON_TYPE)) {
                    // System.out.println("[debug] server got hostchange message");
                    String host = requestDataObject.getString("host");
                    String peerHashId = requestDataObject.getString("hashId");
                    if (host == null || peerHashId == null) {
                        throw new JSONException("Missing attributes");
                    }
                    neighborPeerManager.updatePeerWithHostChange(sourcePeer, host, peerHashId);

                    // join
                } else if (requestType.equals(Constants.JOIN_JSON_TYPE)) {
                    // System.out.println("[debug] server got join message.");
                    String roomId = requestDataObject.getString("roomid");
                    if (roomId == null) {
                        throw new JSONException("Missing attributes");
                    }
                    chatRoomManager.joinPeerToRoom(roomId, sourcePeer);

                    // who
                } else if (requestType.equals(Constants.WHO_JSON_TYPE)) {
                    // System.out.println("[debug] server got who message.");
                    String roomId = requestDataObject.getString("roomid");
                    if (roomId == null) {
                        throw new JSONException("Missing attributes");
                    }

                    chatRoomManager.sendRoomContentMsgToPeer(sourcePeer, roomId);

                    // list
                } else if (requestType.equals(Constants.LIST_JSON_TYPE)) {
                    // System.out.println("[debug] server got list message.");
                    chatRoomManager.sendRoomListMsgToPeer(sourcePeer);

                    // quit
                } else if (requestType.equals(Constants.QUIT_JSON_TYPE)) {
                    // System.out.println("[debug] server got quit message.");
                    neighborPeerManager.handleDisconnectNeighborPeer(sourcePeer);

                    // listNeighbors
                } else if (requestType.equals(Constants.LIST_NEIGHBORS_JSON_TYPE)) {
                    // System.out.println("[debug] server got listneighbors message.");
                    List<Peer> allNeighborPeers = neighborPeerManager.getAllNeighborPeers(sourcePeer);
                    String responseMsg = MessageServices.genListNeighborsResponseMsg(sourcePeer, allNeighborPeers);
                    sourcePeer.getPeerConnection().sendTextMsgToMe(responseMsg);

                    // shout
                } else if (requestType.equals(Constants.SHOUT_JSON_TYPE)) {
                    // System.out.println("[debug] server got shout message.");

                    String content = requestDataObject.getString("content");
                    String rootIdentity = requestDataObject.getString("rootIdentity");
                    String shoutMessageHashId = requestDataObject.getString("shoutMessageHashId");

                    if (content == null || rootIdentity == null || shoutMessageHashId == null) {
                        throw new JSONException("Missing attributes");
                    }

                    // Only peers that have joined the room can shout
                    if ("".equals(sourcePeer.getRoomId())) {
                        return;
                    }

                    synchronized (shoutHashIdHistory) {
                        // Check if the shout message has been forwarded. If it has been forwarded, ignore the request
                        if (shoutHashIdHistory.contains(shoutMessageHashId)) {
                            return;
                        }

                        // New Shout shoutMessageHashId, record it
                        shoutHashIdHistory.add(shoutMessageHashId);
                    }

                    // If rootIdentity is "", it means that the client is the root that sends shout,
                    // assigns the public network identity of the client peer to message, and then forwards it
                    if ("".equals(rootIdentity)) {
                        // System.out.println("[debug] server got shout message, it is root shout.");
                        rootIdentity = sourcePeer.getPublicHostName() + ":" + sourcePeer.getOutgoingPort();

                        // Re-generate a shoutMsg with rootIdentity
                        String newShoutMessageWithRootIdentity =
                                MessageServices.genRelayShoutChatMessage(shoutMessageHashId, content, rootIdentity);

                        // Broadcast the new shoutMessage to all child rooms
                        chatRoomManager.broadcastMessageInAllRoom(newShoutMessageWithRootIdentity);

                        // List<Peer> allNeighborPeers = neighborPeerManager.getAllNeighborPeers();
                        if(this.localclient.alive.get()){ // if the local client has connection with other server
                            try{
                                this.localclient.Write(newShoutMessageWithRootIdentity);
                            }catch(IOException e){
                                System.out.println("fail to send shout message from local peer to other server");
                            }

                        }

                    } else {
                        // System.out.println("[debug] server got shout message, it is NOT a root shout.");
                        // The source request shoutMessage is directly forwarded without modifying the source request
                        // Broadcast to all sub-rooms
                        // Text indicates the original JSON string text received
                        chatRoomManager.broadcastMessageInAllRoom(text);

                        if(this.localclient.alive.get()){ // if the local client has connection with other server
                            try{
                                this.localclient.Write(text);
                            }catch(IOException e){
                                System.out.println("fail to send shout message from local peer to other server");
                            }
                        }
                    }
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

    public void setLocalclient(Client client){
        this.localclient = client;
    }
}