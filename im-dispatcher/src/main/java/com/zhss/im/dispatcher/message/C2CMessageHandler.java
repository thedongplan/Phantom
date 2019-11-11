package com.zhss.im.dispatcher.message;

import com.alibaba.fastjson.JSONObject;
import com.zhss.im.dispatcher.mq.KafkaClient;
import com.zhss.im.dispatcher.session.Session;
import com.zhss.im.dispatcher.session.SessionManager;
import com.zhss.im.protocol.C2CMessageRequest;
import com.zhss.im.protocol.C2CMessageResponse;
import com.zhss.im.protocol.Constants;
import com.zhss.im.protocol.Message;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理C2C消息
 *
 * @author Jianfeng Wang
 * @since 2019/11/11 16:49
 */
@Slf4j
public class C2CMessageHandler extends AbstractMessageHandler {

    private KafkaClient client;

    public C2CMessageHandler(SessionManager sessionManager) {
        super(sessionManager);
        this.client = KafkaClient.getInstance(dispatcherConfig);
    }

    @Override
    public void handleMessage(Message message, SocketChannel channel) throws Exception {
        // 对于C2C消息来说，直接发送到Kafka
        // 1. 先判断是否有session
        C2CMessageRequest c2CMessageRequest = C2CMessageRequest.parseFrom(message.getBody());

        Session session = sessionManager.getSession(c2CMessageRequest.getSenderId());
        if (session == null) {
            log.info("找不到Session，发送消息失败");
            C2CMessageResponse response = C2CMessageResponse.newBuilder()
                    .setSenderId(c2CMessageRequest.getSenderId())
                    .setReceiverId(c2CMessageRequest.getReceiverId())
                    .setStatus(Constants.RESPONSE_STATUS_ERROR)
                    .build();
            Message resp = Message.buildC2cMessageResponse(response);
            channel.writeAndFlush(resp.getBuffer());
            return;

        }
        // 2. 基于snowflake算法生成messageId

        // 3. 投递到kafka
        C2CMessage msg = C2CMessage.builder()
                .senderId(c2CMessageRequest.getSenderId())
                .receiverId(c2CMessageRequest.getReceiverId())
                .content(c2CMessageRequest.getContent())
                .timestamp(System.currentTimeMillis())
                .messageId(1L)
                .build();
        String value = JSONObject.toJSONString(msg);
        log.info("投递单聊消息到Kafka -> {}", value);
        client.send(Constants.TOPIC_SEND_C2C_MESSAGE, c2CMessageRequest.getSenderId(), value);
    }
}