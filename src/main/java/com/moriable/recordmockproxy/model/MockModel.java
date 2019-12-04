package com.moriable.recordmockproxy.model;

import com.moriable.recordmockproxy.admin.form.MockForm;
import com.moriable.recordmockproxy.common.Util;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

@Data
@EqualsAndHashCode(callSuper=false)
public class MockModel extends AbstractModel {

    private TargetModel target;
    private List<MockResponseModel> mockResponses;
    private MockRule rule;

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

        public MockResponseModel(MockForm.MockResponseForm form) {
            setId(Util.getHash(UUID.randomUUID().toString()));
            setEnable(true);
            setStatusCode(form.getStatus());
            setStatusMessage(form.getStatusMessage());
            setHeaders(form.getHeaders());
            if (getHeaders() == null) {
                setHeaders(new LinkedHashMap<>());
            }
            if (form.getType() != null) {
                getHeaders().put("Content-Type", form.getType());
            }
        }

        public InputStream getHeaderStream() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("HTTP/1.1 %d %s\r\n", statusCode, statusMessage));
            headers.forEach((key, value) -> {
                builder.append(key + ": " + value + "\r\n");
            });
            builder.append("\r\n");

            return new ByteArrayInputStream(builder.toString().getBytes());
        }
    }

    public enum MockRule {
        PROXY,
        ONCE,
        REPEAT,
        RANDOM
    }

    public MockModel() {
    }

    public MockModel(MockForm form) {
        target = new TargetModel();
        target.id = Util.getMockId(form);
        target.setMethod(form.getMethod());
        target.setHost(form.getHost());
        target.setPort(form.getPort());
        target.setPath(form.getPath());
        target.setQuery(form.getQuery());

        mockResponses = new ArrayList<>();
        MockResponseModel response = new MockResponseModel(form.getResponse());
        mockResponses.add(response);

        rule = MockRule.valueOf(form.getRule());
    }
}
