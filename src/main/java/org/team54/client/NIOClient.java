package org.team54.client;


import org.team54.app.ChatPeer;
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
    private InetAddress address;
    private int localport;
    private int port;
    private ClientWorker worker;
    private ScannerWorker scannerWorker;
    private SocketChannel socketChannel;
    private ByteBuffer writeBuffer = ByteBuffer.allocate(2048);
    private ByteBuffer readBuffer = ByteBuffer.allocate(2048);


    public NIOClient(InetAddress address, int port, int localport, ClientWorker worker, ScannerWorker scannerWorker) throws IOException {
        this.address = address;
        this.port = port;
        //this.selector = initSelector();
        this.worker = worker;
        this.scannerWorker=scannerWorker;
        this.localport = localport;
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
        alive.set(false);
        socketChannel.close();
        // disconnect, connectNum -1
        connectNum -= 1;
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
        System.out.println("[debug] finish connect");
        connectNum += 1;
        this.socketChannel = socketChannel;

        return socketChannel;
    }


    public void Write(SocketChannel socketChannel,String message) throws IOException{
        writeBuffer.clear();
        writeBuffer.put(message.getBytes(StandardCharsets.UTF_8));
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
    }

    // overload Write function, if no socketChannel is given, use default socketchannel
    public void Write(String message) throws IOException{
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

    public void setSocketChannel(SocketChannel socketChannel){
        this.socketChannel = socketChannel;
    }

    public void setInetAddress(InetAddress address){
        this.address = address;
    }

    public void setLocalport(int localport){
        this.localport = localport;
    }

    public void setPort(int port){
        this.port = port;
    }

    public InetSocketAddress getServer(){
        InetSocketAddress isa = new InetSocketAddress(address,port);
        return isa;
    }

    public int getLocalport(){
        return this.localport;
    }

    // get server's port
    public int getPort(){
        return this.port;
    }

    public SocketChannel getSocketChannel(){
        return this.socketChannel;
    }

    public InetAddress getAddress(){
        return this.address;
    }

    /**
     * arr to store identity
     * {"peer server's address and port","peer's hash ID"}
     * e.g. {"192.168.0.0:1234","791P7i7pspC9U1tq27Q8O76yX41lYZ"}
     * @return
     */
    public String[] getIdentity(){
        String[] arr = {"",""};
        String identity = address.toString()+":"+ ChatPeer.getServerListenPort();
        String hashID = Constants.THIS_PEER_HASH_ID;
        arr[0] = identity;
        arr[1] = hashID;
        return arr;
    }
}


