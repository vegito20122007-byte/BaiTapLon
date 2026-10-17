package com.auction.common.protocol;

import com.auction.common.enums.CommandType;
import java.io.Serializable;

/**
 * Cấu trúc thông điệp Client gửi lên Server qua Socket (định dạng JSON).
 */
public class RequestMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private CommandType command;
    private String token; // Mã phiên/định danh đăng nhập
    private String payloadJson; // Dữ liệu tham số dạng JSON string

    public RequestMessage() {
    }

    public RequestMessage(CommandType command, String payloadJson) {
        this.command = command;
        this.payloadJson = payloadJson;
    }

    public RequestMessage(CommandType command, String token, String payloadJson) {
        this.command = command;
        this.token = token;
        this.payloadJson = payloadJson;
    }

    public CommandType getCommand() {
        return command;
    }

    public void setCommand(CommandType command) {
        this.command = command;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
