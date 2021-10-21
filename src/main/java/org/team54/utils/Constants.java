package org.team54.utils;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description some constants
 * @create 2021-10-06 10:35
 */
public class Constants {

    /** TODO at start, gen random hashId for this Chat peer */
    /** can be called by ChatPeer.PEER_HASH_ID */
    public static final String THIS_PEER_HASH_ID = StringUtils.genHashId();

    /** status consts */
    public static final int NON_PORT_DESIGNATED = -1;

    /** config consts*/
    public static final int CHANNEL_BUFFER_SIZE = 8192;

    /**message type*/
    public static final String HOST_CHANGE_JSON_TYPE = "hostchange";
    public static final String LIST_NEIGHBORS_JSON_TYPE = "listneighbors";
    public static final String LIST_NEIGHBORS_RESPOND_JSON_TYPE = "listneighborsrespond";
    public static final String SHOUT_JSON_TYPE = "shout";

    public static final String JOIN_JSON_TYPE = "join";
    public static final String ROOM_CHANGE_JSON_TYPE = "roomchange";
    public static final String ROOM_CONTENTS_JSON_TYPE = "roomcontents";
    public static final String WHO_JSON_TYPE = "who";
    public static final String ROOM_LIST_JSON_TYPE = "roomlist";
    public static final String LIST_JSON_TYPE = "list";
    public static final String MESSAGE_JSON_TYPE = "message";
    public static final String QUIT_JSON_TYPE = "quit";
    public static final String Connect_TYPE = "connect";
    public static final String ROOM_CREATE_JSON_TYPE = "createroom";
    public static final String ROOM_Delete_JSON_TYPE = "delete";
    public static final String KICK_TYPE = "kick";
    public static final String Search_Net_TYPE = "searchnetwork";

}