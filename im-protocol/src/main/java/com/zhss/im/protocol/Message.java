package com.zhss.im.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IM消息协议
 * 消息头长度 20
 *
 * @author Jianfeng Wang
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    /**
     * 客户端SDK版本号
     */
    protected int appSdkVersion;
    /**
     * 消息类型：请求 / 响应
     */
    protected int messageType;
    /**
     * 请求类型
     */
    protected int requestType;
    /**
     * 请求顺序
     */
    protected int sequence;
    /**
     * 消息体长度
     */
    protected int bodyLength;
    /**
     * 消息体
     */
    protected byte[] body;

    public static Message buildAuthenticateRequest(AuthenticateRequestProto.AuthenticateRequest authenticateRequest) {
        byte[] body = authenticateRequest.toByteArray();
        return Message.builder()
                .appSdkVersion(Constants.APP_SDK_VERSION_1)
                .messageType(Constants.MESSAGE_TYPE_REQUEST)
                .requestType(Constants.REQUEST_TYPE_AUTHENTICATE)
                .bodyLength(body.length)
                .body(body)
                .build();
    }

    public static Message buildAuthenticateResponse(AuthenticateResponseProto.AuthenticateResponse authenticateResponse) {
        byte[] body = authenticateResponse.toByteArray();
        return Message.builder()
                .appSdkVersion(Constants.APP_SDK_VERSION_1)
                .messageType(Constants.MESSAGE_TYPE_RESPONSE)
                .requestType(Constants.REQUEST_TYPE_AUTHENTICATE)
                .bodyLength(body.length)
                .body(body)
                .build();
    }

    public ByteBuf getBuffer() {
        ByteBuf buffer = Unpooled.buffer(Constants.HEADER_LENGTH +
                body.length + Constants.DELIMITER.length);
        buffer.writeInt(appSdkVersion);
        buffer.writeInt(messageType);
        buffer.writeInt(requestType);
        buffer.writeInt(sequence);
        buffer.writeInt(bodyLength);
        buffer.writeBytes(body);
        buffer.writeBytes(Constants.DELIMITER);
        return buffer;
    }

    public static Message parse(ByteBuf byteBuf) {
        Message message = Message.builder()
                .appSdkVersion(byteBuf.readInt())
                .messageType(byteBuf.readInt())
                .requestType(byteBuf.readInt())
                .sequence(byteBuf.readInt())
                .bodyLength(byteBuf.readInt())
                .build();
        message.body = new byte[message.getBodyLength()];
        byteBuf.readBytes(message.body);
        return message;
    }


}
