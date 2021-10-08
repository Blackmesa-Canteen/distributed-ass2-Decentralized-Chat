package org.team54.messageBean;

import lombok.*;
import org.team54.utils.Constants;

import java.util.List;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 11:00
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class RoomContentsMessage {

    private String type = Constants.ROOM_CONTENTS_JSON_TYPE;
    private String roomid;
    private List<String> identities;
}