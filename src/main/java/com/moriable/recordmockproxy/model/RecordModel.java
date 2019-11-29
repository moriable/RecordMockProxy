package com.moriable.recordmockproxy.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class RecordModel implements Serializable {
    private String id;
    private long date;
    private RequestModel request;
    private ResponseModel response;

    @Data
    public static class RequestModel implements Serializable {
        private String method;
        private String host;
        private int port;
        private String path;
        private String query;
        private Map<String, String> headers;
        private String bodyfile;
    }

    @Data
    public static class ResponseModel implements Serializable {
        private int statusCode;
        private String statusMessage;
        private Map<String, String> headers;
        private String bodyfile;
        private long time;
    }
}
