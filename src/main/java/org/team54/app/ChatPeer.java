package org.team54.app;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.team54.client.ClientWorker;
import org.team54.client.NIOClient;
import org.team54.client.ScannerWorker;
import org.team54.server.ChatServer;
import org.team54.server.MessageQueueWorker;
import org.team54.utils.Constants;
import org.team54.utils.StringUtils;

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
    private static NIOClient client;

    public static void main(String[] args) throws IOException {

        // handle args
        handleArgs(args);

        // init server listening logic
        MessageQueueWorker MQWorker = new MessageQueueWorker();
        server = new ChatServer(null, serverListenPort, MQWorker);
        new Thread(server).start();


        // init client logic
        /**
         * client has three thread
         *
         * scannerWorker thread: read user input, encode user input and send to server
         * client thread: handle connection, NIO selecter etc. Will pass the received message
         *                from server to clientWorker to avoid block.
         * clientWorker thread: do the mission passed by client thread
         *
         * Both the client thread and clientWoker thread will not start until the scannerWorker receives
         * the #connect command
         */
        ClientWorker clientWorker = new ClientWorker();
        //open a new thread for getting userinput and write
        ScannerWorker scannerWorker = new ScannerWorker(null,null,clientWorker);
        // init client
        client = new NIOClient(null,serverListenPort, clientBindPort, clientWorker,scannerWorker);
        // set client to scannnerWorker
        scannerWorker.setClient(client);
        new Thread(scannerWorker).start();
        // the client thread and the worker thread need to be started after #connet command
        // the scannerWorker will read the #connect and starts them.

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