package org.team54.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-06 12:23
 */
@Data
@Builder
@NoArgsConstructor
@ToString
public class Room {

    private String roomId;
    private ArrayList<Peer> peers;

    /** owner will always be creator */
    private Peer owner;
}