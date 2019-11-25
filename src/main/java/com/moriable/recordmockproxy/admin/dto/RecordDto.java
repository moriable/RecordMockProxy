package com.moriable.recordmockproxy.admin.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
public class RecordDto implements Serializable {

    private String id;
    private long date;
    private RequestDto request;
    private ResponseDto response;

    @Data
    public static class RequestDto implements Serializable {
        private String method;
        private String host;
        private int port;
        private String path;
        private String query;
        private Map<String, String> headers;
        private String bodyfile;
    }

    @Data
    public static class ResponseDto implements Serializable {
        private int statusCode;
        private Map<String, String> headers;
        private String bodyfile;
    }

}
