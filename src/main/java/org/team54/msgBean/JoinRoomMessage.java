package org.team54.msgBean;

import lombok.*;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 10:54
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class JoinRoomMessage {
    private String type = Constants.JOIN_JSON_TYPE;
    private String roomid = "";
}