package com.moriable.recordmockproxy.admin.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Map;

@Data
public class MockDto {

    private String id;
    private String method;
    private String host;
    private int port;
    private String path;
    private String query;

    private Map<String, String> mockHeaders;
    private String mockBodyfile;
}
