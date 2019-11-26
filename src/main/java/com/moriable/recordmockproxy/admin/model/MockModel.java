package com.moriable.recordmockproxy.admin.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MockModel {

    private TragetModel target;
    private List<ResponseModel> mockResponses;
    private MockRule rule;

    @Data
    public static class TragetModel {
        private String id;
        private String method;
        private String host;
        private int port;
        private String path;
        private String query;
    }

    public enum MockRule {
        PROXY,
        ONCE,
        REPEAT,
        RANDOM
    }
}
