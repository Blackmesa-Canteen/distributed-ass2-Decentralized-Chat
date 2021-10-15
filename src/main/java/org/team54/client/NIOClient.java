package org.team54.client;


import org.team54.utils.Constants;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class NIOClient implements Runnable{
    public AtomicBoolean alive = new AtomicBoolean();
    // record the number of connect the client established
    // connectNum should in [0,1], cannot connect to more than 1 server
    public int connectNum = 0;

    private InetAddress address;
    private int localport;
    private int port;
    private Selector selector;
    private LinkedBlockingQueue<ByteBuffer> missionQueue = new LinkedBlockingQueue(128);
    private ClientWorker worker;
    private ScannerWorker scannerWorker;
    //private ByteBuffer writeBuffer = ByteBuffer.allocate(2048);
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
            try {
                selector.select();
                if(selector.isOpen()){
                    Iterator selectedKeys = this.selector.selectedKeys().iterator();
                    while(selectedKeys.hasNext()){
                        SelectionKey next = (SelectionKey) selectedKeys.next();
                        selectedKeys.remove(); // remove the selected key, otherwise will meet it again in the next iteration
                        if(next.isConnectable()){// judge if the channel is connectable
                            this.Connect(next);
                            break;
                        } else if (next.isReadable()){
                            this.Read(next);
                        } else if (next.isWritable()){
                            this.Write(next);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeSelector() throws IOException {
        selector.close();
    }

    public void stop() throws IOException{
        // Traverse all the registered key
        // close the bond socket and then remove the key
        Iterator keys =  selector.keys().iterator();
        while(keys.hasNext()){
            SelectionKey key = (SelectionKey) keys.next();
            SocketChannel socketChannel = (SocketChannel) key.channel();
            //System.out.println("[debug] port: " + socketChannel.socket().getPort());
            //System.out.println("[debug] Localport: " + socketChannel.socket().getLocalPort());
            socketChannel.socket().close();
            socketChannel.close();
            key.cancel();
        }
        // selector will not close the socketchannel until the next select
        // select now to close
        selector.selectNow();

        // disconnect, connectNum -1
        connectNum -= 1;
    }


    /**
     * encapsulate Connect function,
     * will be called when the soceketchannel is connectable (the first time to connect to server)
     * will finish the connection and change interest operation to OP_Read
     * @param key
     * @throws IOException
     */
    private void Connect(SelectionKey key){
        //get socketchannel by key
        SocketChannel socketChannel = (SocketChannel) key.channel();
        try{
            // finish the connect sequence
            socketChannel.finishConnect();
            // register the channel as read
            socketChannel.register(selector,SelectionKey.OP_READ);
        }catch (IOException e){
            // if the localport is in TIME_WAIT status, socketChannel.fininshConnect() will throw a Bind exception
            System.out.println("the localport is being used right now, change another port or try again later");
        }

        //System.out.println("[debug] server connected...");
        //System.out.println("[debug] in client, server port " + socketChannel.socket().getPort());

        // pass socketchannel to scannerwork for sending message to server
        this.scannerWorker.setSocketChannel(socketChannel);
    }

    private void Write(SelectionKey key) throws IOException{
        //get socketchannel by key
        SocketChannel socketChannel = (SocketChannel) key.channel();
//        Scanner scanner = new Scanner(System.in);
//        String message = scanner.nextLine();
//        socketChannel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * read message from the socketchannel
     * and then, pass it to worker's queue
     * @param key
     * @throws IOException
     */
    private void Read(SelectionKey key) throws IOException{
        //get socketchannel by key
        SocketChannel socketChannel = (SocketChannel) key.channel();
        // clear readbuffer for next read operation
        readBuffer.clear();
        // get the length of data in readbuffer
        int readNum = socketChannel.read(readBuffer);
        if(readNum == -1){
            System.out.println("[debug] in client, bad read, client close");
            socketChannel.close();
            key.cancel();

            return;
        }
        // pass the message to client worker
        worker.processEvent(this,socketChannel,readBuffer.array(),readNum);

        // System.out.println(new String(readBuffer.array(),0,readNum));
        // after reading, rigister the socketchannel to OP_READ to prepare for next read operation;
        socketChannel.register(selector,SelectionKey.OP_READ);
    }

    private Selector initSelector() throws IOException {
        // create selector
        Selector selector = SelectorProvider.provider().openSelector();

        // open a nonblocking socket channel
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
        connectNum += 1;

        // register connect operation
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        return selector;
    }


    public void send(){}

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

    public void startInit() throws IOException{
        this.selector =  initSelector();
    }

    public int getLocalport(){
        return this.localport;
    }
    /**
     * arr to store identity
     * {"peer server's address and port","peer's hash ID"}
     * e.g. {"192.168.0.0:1234","791P7i7pspC9U1tq27Q8O76yX41lYZ"}
     * @return
     */
    public String[] getIdentity(){
        String[] arr = {"",""};
        String identity = address.toString()+":"+Integer.toString(port);
        String hashID = Constants.THIS_PEER_HASH_ID;
        arr[0] = identity;
        arr[1] = hashID;
        return arr;
    }


    public int genRandomPort(){
        // gen a radom port number
        int port = (int) 1 + (int) (Math.random() * (65535 - 1));
        // test if the port has been used
        while(!isPortUsable(port)){
            port = (int) 1 + (int) (Math.random() * (65535 - 1));;
        }
        return port;
    }

    public boolean isPortUsable(int port){
        Socket socket = new Socket();
        try{
            socket.connect(new InetSocketAddress("127.0.0.1",port));
        }catch(IOException e){
            return false;
        }finally {
            try{
                socket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return true;
    }


}

