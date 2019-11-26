package com.moriable.recordmockproxy.admin.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class RecordModel implements Serializable {

    private String id;
    private long date;
    private RequestModel request;
    private ResponseModel response;

}
