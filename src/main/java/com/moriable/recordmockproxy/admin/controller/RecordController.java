package com.moriable.recordmockproxy.admin.controller;

import com.moriable.recordmockproxy.model.RecordModel;
import spark.Route;

import java.util.Map;

public class RecordController {

    private Map<String, RecordModel> recordMap;

    public RecordController(Map<String, RecordModel> recordMap) {
        this.recordMap = recordMap;
    }

    public Route get = (request, response) -> {
        return null;
    };

    public Route getAll = (request, response) -> {
        response.type("application/json");
        return recordMap.values();
    };

    public Route deleteAll = (request, response) -> {
        return null;
    };

    public Route response = (request, response) -> {
        return null;
    };

    public Route csv = (request, response) -> {
        return null;
    };

}
