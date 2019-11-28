package com.moriable.recordmockproxy.admin.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MockModel {

    private TragetModel target;
    private List<MockResponseModel> mockResponses;
    private MockRule rule;
    private int callCount;

    @Data
    public static class TragetModel {
        private String id;
        private String method;
        private String host;
        private int port;
        private String path;
        private String query;
    }

    @Data
    public static class MockResponseModel {
        private String id;
        private boolean enable;
        private int statusCode;
        private String statusMessage;
        private Map<String, String> headers;
    }

    public enum MockRule {
        PROXY,
        ONCE,
        REPEAT,
        RANDOM
    }
}
