package org.team54.msgBean;

import lombok.*;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 *
 * message received by the server is relayed to all clients
 * that are within the same room as the client who sent the message
 *
 * 2021-10-08 11:06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ServerRelayMessage {
    private String type = Constants.MESSAGE_JSON_TYPE;
    private String identity;
    private String content;
}