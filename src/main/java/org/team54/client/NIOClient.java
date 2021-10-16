package org.team54.client;


import org.team54.app.ChatPeer;
import org.team54.model.Peer;
import org.team54.utils.Constants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;

public class NIOClient implements Runnable{
    public AtomicBoolean alive = new AtomicBoolean();
    // record the number of connect the client established
    // connectNum should in [0,1], cannot connect to more than 1 server
    public int connectNum = 0;
    private ClientWorker worker;
    private ScannerWorker scannerWorker;
    private Peer localPeer;
    private SocketChannel socketChannel;
    private ByteBuffer writeBuffer = ByteBuffer.allocate(2048);
    private ByteBuffer readBuffer = ByteBuffer.allocate(2048);


    public NIOClient(ClientWorker worker, ScannerWorker scannerWorker, Peer localPeer) throws IOException {

        //this.selector = initSelector();
        this.worker = worker;
        this.scannerWorker=scannerWorker;

        this.localPeer = localPeer;
    }



    @Override
    public void run(){
        alive.set(true);
        // spin to get available socketchannel
        while(alive.get()){
            try{
                Read(socketChannel);
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }

    public void stop() throws IOException{
        if(!this.socketChannel.isConnected()){
            System.out.println("not connected yet");
            return;
        }
        alive.set(false);
        socketChannel.close();
        // disconnect, connectNum -1
        connectNum -= 1;
        // need to set ClientBind port to -1 for next connection
        ChatPeer.setClientPort(-1);
        localPeer.setOutgoingPort(-1);
    }


    public SocketChannel startConn(int localport, int port, InetAddress address) throws IOException{
        // init a socketchannel and set to NIO mode
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        // bind local port to the socket
        // if localport is -1, do not bind, establish the connect with random localport
        if(localport!=-1){
            // init localaddress for socket to bind with
            InetSocketAddress localaddress = new InetSocketAddress("127.0.0.1",localport);
            // let the bond port can be used in TIME_WAIT status after disconnect
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            socketChannel.bind(localaddress);

        }

        // server's address, address+port e.g. 127.0.0.1:1234,
        InetSocketAddress isa = new InetSocketAddress(address,port);
        //connect to sever, connectNum +1
        socketChannel.connect(isa);
        socketChannel.finishConnect();


        this.socketChannel = socketChannel;

        if(socketChannel.isConnected()){
            // set changes to localPeer
            localPeer.setOutgoingPort(socketChannel.socket().getLocalPort());
            localPeer.setLocalHostName(address.toString());
            localPeer.setIdentity(socketChannel.getLocalAddress().toString().replace("/",""));
            localPeer.setIdentity(localPeer.getLocalHostName()+":"+localPeer.getListenPort());
            System.out.println("[debug] localhostName : " + localPeer.getLocalHostName());
            System.out.println("[debug] outgoingPort : " + localPeer.getOutgoingPort());
            System.out.println("[debug] listeningPort : " + localPeer.getListenPort());
            System.out.println("[debug] identity : " + localPeer.getIdentity());
            // record the success connection
            connectNum += 1;
        }else{
            System.out.println("connect fails, maybe bad server address");
        }

        System.out.println("[debug] finish connect");
        return socketChannel;
    }


    public void Write(SocketChannel socketChannel,String message) throws IOException{
        if(!this.socketChannel.isConnected()){
            System.out.println("not connected yet");
            return;
        }
        writeBuffer.clear();
        writeBuffer.put(message.getBytes(StandardCharsets.UTF_8));
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
    }

    // overload Write function, if no socketChannel is given, use default socketchannel
    public void Write(String message) throws IOException{
        if(!this.socketChannel.isConnected()){
            System.out.println("not connected yet");
            return;
        }
        writeBuffer.clear();
        writeBuffer.put(message.getBytes(StandardCharsets.UTF_8));
        writeBuffer.flip();
        this.socketChannel.write(writeBuffer);
    }

    /**
     * read message from the socketchannel
     * and then, pass it to worker's queue
     * @param socketChannel
     * @throws IOException
     */
    private void Read(SocketChannel socketChannel) throws IOException{
        if(!socketChannel.isConnected()){
            //System.out.println("not connected yet");
            return;
        }
        readBuffer.clear();
        //System.out.println("connected " +socketChannel.isConnected());
        // get the length of data in readbuffer
        int readNum = socketChannel.read(readBuffer);
        //System.out.println("[debug] here ");
        if(readNum == -1){
            System.out.println("[debug] in client, bad read, client close");
            socketChannel.close();

            return;
        }
        // pass the message to client worker
        if(readNum!=0){
            worker.processEvent(this,socketChannel,readBuffer.array(),readNum);
        }


    }

    public String getServer(){
        try{
            return this.socketChannel.getRemoteAddress().toString();
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }


    /**
     * arr to store identity
     * {"peer server's address and port","peer's hash ID"}
     * e.g. {"192.168.0.0:1234","791P7i7pspC9U1tq27Q8O76yX41lYZ"}
     * @return
     */
    public String[] getIdentity(){
        String[] arr = {"",""};
        String identity = localPeer.getIdentity();
        String hashID = localPeer.getHashId();
        //String identity = address.toString()+":"+ ChatPeer.getServerListenPort();
        //String hashID = Constants.THIS_PEER_HASH_ID;
        System.out.println("[debug] in client, getIdnetity, identity: " + identity + " hashID: " + hashID);
        arr[0] = identity;
        arr[1] = hashID;
        return arr;
    }
}


