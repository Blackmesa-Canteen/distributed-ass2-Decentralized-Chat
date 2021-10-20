package org.team54.messageBean;

import lombok.*;

/**
 * @author Xiaotian
 * @program assignment1
 * @description used in RoomListMessage
 * @create 2021-08-18 00:35
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RoomDTO {
    private String roomid;
    private Integer count;
}