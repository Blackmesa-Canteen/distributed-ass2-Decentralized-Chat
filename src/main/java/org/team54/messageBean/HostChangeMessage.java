package org.team54.messageBean;

import lombok.*;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 10:44
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
// TODO 使用 MessageServices里的genHostChangeRequestMessage生成这个实例
public class HostChangeMessage {
    @Builder.Default
    private String type = Constants.HOST_CHANGE_JSON_TYPE;
    private String host;

    // TODO 新变量: 记录当前client peer 的 id信息, 当你client peer主动链接到另一个peer, 把Constants.THIS_PEER_HASH_ID包含在这个字段里,
    private String hashId;
}