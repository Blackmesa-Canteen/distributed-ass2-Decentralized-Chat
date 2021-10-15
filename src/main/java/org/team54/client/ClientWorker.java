package org.team54.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientWorker implements Runnable{
    public AtomicBoolean alive = new AtomicBoolean();
    // mission queue to store mission from client thread
    private LinkedBlockingQueue<ClientDataEvent> queue = new LinkedBlockingQueue(128);

    @Override
    public void run(){
        alive.set(true);
        ClientDataEvent dataEvent;
        while(alive.get()){
            try {
                dataEvent = queue.take();
            } catch (InterruptedException e) {
                continue;
            }

            // try to handle json message here
            handleData(dataEvent);
        }
    }

    /**
     * add the received event to quest queue
     * this method will be called when socket channel receives a new message
     * after that, the soket channel's thread can continue without block
     * client worker takes the message from the quest queue and dealing with them in another thread
     * @param nioClient
     * @param socketChannel the socket channel data comes from
     * @param data
     * @param size data size
     */
    public void processEvent(NIOClient nioClient, SocketChannel socketChannel, byte[] data, int size){
        byte[] copiedData = new byte[size];
        System.arraycopy(data,0,copiedData,0,size);
        ClientDataEvent clientdataEvent = new ClientDataEvent(nioClient,socketChannel,copiedData);
        queue.offer(clientdataEvent);
    }



    private void handleData(ClientDataEvent clientdataEvent){
        //get the received data
        String data = new String(clientdataEvent.data,0,clientdataEvent.data.length);
        if(ScannerWorker.waitingInput.get()){
            System.out.println("\n" + "[debug] received data is " + data);
            ScannerWorker.waitingInput.set(false);
        }else{
            System.out.println("[debug] received data is " + data);
        }
        // start to handle json messages from server, hasn't finished yet;
    }
}
