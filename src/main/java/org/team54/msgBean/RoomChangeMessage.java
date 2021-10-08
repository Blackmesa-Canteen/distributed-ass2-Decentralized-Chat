package org.team54.msgBean;

import lombok.*;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 10:59
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class RoomChangeMessage {
    private String type = Constants.ROOM_CHANGE_JSON_TYPE;
    private String identity;
    private String former = "";
    private String roomid;
}