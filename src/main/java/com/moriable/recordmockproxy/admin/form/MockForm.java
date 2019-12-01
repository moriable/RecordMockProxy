package com.moriable.recordmockproxy.admin.form;

import lombok.Data;

import java.util.Map;

@Data
public class MockForm {

    private String method;
    private String host;
    private int port;
    private String path;
    private String query;

    private String rule;

    private MockResponseForm response;

    @Data
    public class MockResponseForm {
        private int status;
        private String statusMessage;
        private String type;
        private Map<String, String> headers;
        private String textBody;
    }
}