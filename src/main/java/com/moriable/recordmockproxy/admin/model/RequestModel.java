package com.moriable.recordmockproxy.admin.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class RequestModel implements Serializable {
    private String method;
    private String host;
    private int port;
    private String path;
    private String query;
    private Map<String, String> headers;
    private String bodyfile;
}
