package org.team54.msgBean;

import lombok.*;
import org.team54.utils.Constants;

import java.util.List;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 11:02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class RoomListMessage {
    private String type = Constants.ROOM_LIST_JSON_TYPE;
    private List<RoomDTO> rooms;
}