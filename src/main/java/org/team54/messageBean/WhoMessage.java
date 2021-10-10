package org.team54.messageBean;

import lombok.*;
import org.team54.utils.Constants;

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
public class WhoMessage {
    @Builder.Default
    private String type = Constants.WHO_JSON_TYPE;
    private String roomid;
}