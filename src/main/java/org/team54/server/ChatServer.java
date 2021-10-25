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
    private final ChatRoomManager chatRoomManager;

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
                } else {
                    // cancel the non-owner connection key
                    key.cancel();
                }
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
     * 记录本server peer是否已经转发过同一个shout message
     */
    private final HashSet<String> shoutHashIdHistory = new HashSet<>();

    /**
     * TODO 在这里,peer会应付从监听端口到来的下游peers们的请求,并给对应下游peer或者room members的out port给对应的回应
     *
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
                // chat聊天信息广播转发
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

                    // Hostchange信息,client连接后立刻会发送过来, 用来给Peer model赋予listening port信息
                } else if (requestType.equals(Constants.HOST_CHANGE_JSON_TYPE)) {
                    // System.out.println("[debug] server got hostchange message");
                    String host = requestDataObject.getString("host");
                    String peerHashId = requestDataObject.getString("hashId");
                    if (host == null || peerHashId == null) {
                        throw new JSONException("Missing attributes");
                    }
                    neighborPeerManager.updatePeerWithHostChange(sourcePeer, host, peerHashId);

                    // join请求
                } else if (requestType.equals(Constants.JOIN_JSON_TYPE)) {
                    // System.out.println("[debug] server got join message.");
                    String roomId = requestDataObject.getString("roomid");
                    if (roomId == null) {
                        throw new JSONException("Missing attributes");
                    }
                    chatRoomManager.joinPeerToRoom(roomId, sourcePeer);

                    // who请求, 回应所需信息给该client
                } else if (requestType.equals(Constants.WHO_JSON_TYPE)) {
                    // System.out.println("[debug] server got who message.");
                    String roomId = requestDataObject.getString("roomid");
                    if (roomId == null) {
                        throw new JSONException("Missing attributes");
                    }

                    chatRoomManager.sendRoomContentMsgToPeer(sourcePeer, roomId);

                    // list请求, 回应所需信息给该client
                } else if (requestType.equals(Constants.LIST_JSON_TYPE)) {
                    // System.out.println("[debug] server got list message.");
                    chatRoomManager.sendRoomListMsgToPeer(sourcePeer);

                    // quit请求
                } else if (requestType.equals(Constants.QUIT_JSON_TYPE)) {
                    // System.out.println("[debug] server got quit message.");
                    neighborPeerManager.handleDisconnectNeighborPeer(sourcePeer);

                    // listNeighbors请求, 回应所需信息给该client
                } else if (requestType.equals(Constants.LIST_NEIGHBORS_JSON_TYPE)) {
                    // System.out.println("[debug] server got listneighbors message.");
                    List<Peer> allNeighborPeers = neighborPeerManager.getAllNeighborPeers(sourcePeer);
                    String responseMsg = MessageServices.genListNeighborsResponseMsg(sourcePeer, allNeighborPeers);
                    sourcePeer.getPeerConnection().sendTextMsgToMe(responseMsg);

                    // TODO shout
                    // shout请求
                } else if (requestType.equals(Constants.SHOUT_JSON_TYPE)) {
                    // System.out.println("[debug] server got shout message.");

                    String content = requestDataObject.getString("content");
                    String rootIdentity = requestDataObject.getString("rootIdentity");
                    String shoutMessageHashId = requestDataObject.getString("shoutMessageHashId");

                    if (content == null || rootIdentity == null || shoutMessageHashId == null) {
                        throw new JSONException("Missing attributes");
                    }

                    // 只有加入了房间的peer才能shout
                    if ("".equals(sourcePeer.getRoomId())) {
                        return;
                    }

                    synchronized (shoutHashIdHistory) {
                        // 判断是否已经转发过这个shout信息了.如果已经转发过,无视这个请求
                        if (shoutHashIdHistory.contains(shoutMessageHashId)) {
                            return;
                        }

                        // 新的shout shoutMessageHashId, 记录之
                        shoutHashIdHistory.add(shoutMessageHashId);
                    }

                    // 如果 rootIdentity 为"",说明该客户是发shout的root, 赋值这个客户peer的公网identity在message上,然后转发
                    if ("".equals(rootIdentity)) {
                        // System.out.println("[debug] server got shout message, it is root shout.");
                        rootIdentity = sourcePeer.getPublicHostName() + ":" + sourcePeer.getOutgoingPort();

                        // 重新生成一个带rootIdentity的shoutMsg
                        String newShoutMessageWithRootIdentity =
                                MessageServices.genRelayShoutChatMessage(shoutMessageHashId, content, rootIdentity);

                        // 将这个新的shoutMessage广播到所有子房间
                        chatRoomManager.broadcastMessageInAllRoom(newShoutMessageWithRootIdentity);

                        // TODO 本peer还要将这个新消息发往上游的chatroom server(如果存在的话)
                        // 需要在client里实现,因为只有client里能够和上游server的listening port直接通信
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
                        // 不修改源请求,直接转发源请求shoutMessage
                        // 广播到所有子房间
                        // text 为收到的原始的JSON string text
                        chatRoomManager.broadcastMessageInAllRoom(text);

                        // TODO 本peer还要将这个新消息发往上游的chatroom, 如果存在的话
                        // 需要在client里实现,因为只有client里能够和上游server的listening port直接通信
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