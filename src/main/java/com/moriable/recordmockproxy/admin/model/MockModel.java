package com.moriable.recordmockproxy.admin.model;

import lombok.Data;

import java.util.Map;

@Data
public class MockModel {

    private String id;
    private String method;
    private String host;
    private int port;
    private String path;
    private String query;

    private Map<String, String> mockHeaders;
    private String mockBodyfile;
}
