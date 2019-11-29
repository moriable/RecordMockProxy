package com.moriable.recordmockproxy.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper=false)
public class MockModel extends AbstractModel {

    private TargetModel target;
    private List<MockResponseModel> mockResponses;
    private MockRule rule;
    private int callCount;

    @Data
    public static class TargetModel implements Serializable {
        private String id;
        private String method;
        private String host;
        private int port;
        private String path;
        private String query;
    }

    @Data
    public static class MockResponseModel implements Serializable {
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
