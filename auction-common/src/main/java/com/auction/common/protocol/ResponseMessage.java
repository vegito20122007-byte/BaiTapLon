package com.auction.common.protocol;

import java.io.Serializable;

/**
 * Cấu trúc phản hồi từ Server trả về cho Client.
 */
public class ResponseMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        SUCCESS,
        ERROR
    }

    private Status status;
    private String message;
    private String dataJson;

    public ResponseMessage() {
    }

    public ResponseMessage(Status status, String message, String dataJson) {
        this.status = status;
        this.message = message;
        this.dataJson = dataJson;
    }

    public static ResponseMessage success(String message, String dataJson) {
        return new ResponseMessage(Status.SUCCESS, message, dataJson);
    }

    public static ResponseMessage error(String message) {
        return new ResponseMessage(Status.ERROR, message, null);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }
}
