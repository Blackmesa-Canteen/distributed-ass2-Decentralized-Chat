package org.team54.app;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 * @program assignment1
 * @description
 * @create 2021-08-18 19:59
 */
public class MyCmdOption {

    @Option(name="-p", usage = "server's listening port number.")
    public long listenPort = (int) Constants.NON_PORT_DESIGNATED;

    @Option(name="-i", usage = "port to make connection as a client.")
    public long clientPort = (int) Constants.NON_PORT_DESIGNATED;
}