package com.moriable.recordmockproxy.admin.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ResponseModel implements Serializable {
    private int statusCode;
    private Map<String, String> headers;
    private String bodyfile;
}
