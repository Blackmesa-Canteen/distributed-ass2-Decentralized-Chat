package org.team54.app;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.team54.client.Client;
import org.team54.client.ScannerWorker;
import org.team54.model.Peer;
import org.team54.server.ChatRoomManager;
import org.team54.server.ChatServer;
import org.team54.server.MessageQueueWorker;
import org.team54.server.NeighborPeerManager;
import org.team54.utils.Constants;

import java.io.IOException;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-05 20:32
 */
public class ChatPeer {

    /** server listen port: -p */
    private static int serverListenPort = 4444;
    // a socketchannel can connect to seversocketchannel without any local port bond
    // when clientBindPort = -1, NIOClient will connect to server directly with out any local port bond
    // in such case, the localport will be assigned randomly
    private static int clientBindPort = Constants.NON_PORT_DESIGNATED;

    /** client connect port: -i, if not designated in argument, use OS random port !! */
//    private static int clientPort = Constants.NON_PORT_DESIGNATED;

    private static ChatServer server;
    private static Client client;

    private static ChatRoomManager chatRoomManager;
    private static NeighborPeerManager neighborPeerManager;

    public static void main(String[] args) throws IOException {

        // handle args
        handleArgs(args);

        // init local Peer
        Peer localPeer = Peer.builder()
                .hashId(Constants.THIS_PEER_HASH_ID)
                .listenPort(serverListenPort)
                .isSelfPeer(true)
                .build();

        // init server listening logic
        MessageQueueWorker MQWorker = new MessageQueueWorker();

        // init server-used managers
        chatRoomManager = ChatRoomManager.getInstance();
        neighborPeerManager = NeighborPeerManager.getInstance();
        chatRoomManager.setNeighborPeerManager(neighborPeerManager);
        neighborPeerManager.setChatRoomManager(chatRoomManager);


        server = new ChatServer(null, serverListenPort, MQWorker);
        new Thread(server).start();


        // init client logic
        /**
         * client has two thread
         *
         * scannerWorker thread: read user input, encode user input and send to server
         * client thread: handle connection, read from server.
         *
         */
        //open a new thread for getting userinput and write
        ScannerWorker scannerWorker = new ScannerWorker(null, localPeer);
        // init client
        client = new Client(localPeer,server);
        // set client to scannnerWorker
        scannerWorker.setClient(client);
        new Thread(scannerWorker).start();
        new Thread(client).start();

        server.setLocalclient(client);

    }

    private static void handleArgs(String[] args) {
        MyCmdOption option = new MyCmdOption();
        CmdLineParser cmdLineParser = new CmdLineParser(option);

        try {
            if (args.length == 0) {
                // no args, default server port is 4444
                serverListenPort = 4444;

                // no arges, default client connection port will be this
                // need to allocate randomly when create connection
                clientBindPort = Constants.NON_PORT_DESIGNATED;

            } else {
                cmdLineParser.parseArgument(args);

                if (option.listenPort != Constants.NON_PORT_DESIGNATED
                        && (option.listenPort < 1
                        || option.listenPort > 65535)) {
                    // port too large
                    System.out.println("port should in [1,65535]");
                    System.exit(-1);
                }

                if (option.clientPort != Constants.NON_PORT_DESIGNATED
                        && (option.clientPort < 1
                        || option.clientPort > 65535)) {
                    // port too large
                    System.out.println("port should in [1,65535]");
                    System.exit(-1);
                }

                serverListenPort = (int) option.listenPort;
                clientBindPort = (int) option.clientPort;
            }

        } catch (CmdLineException e) {
            System.out.println("Command line error: " + e.getMessage());
            argHelpInfo(cmdLineParser);
            System.exit(-1);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void argHelpInfo(CmdLineParser cmdLineParser) {
        System.out.println("-p [listen port number], -i [client port for connection]");
        cmdLineParser.printUsage(System.out);
    }

    public static int getServerListenPort() {
        return serverListenPort;
    }

    public static int getClientPort() {
        return clientBindPort;
    }

    public static void setClientPort(int clientPort) {
        ChatPeer.clientBindPort = clientPort;
    }
}