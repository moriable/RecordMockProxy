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

    private int responseStatus;
    private String responseStatusMessage;
    private Map<String, String> responseHeaders;
    private String responseType;
    private String responseBody;
}
