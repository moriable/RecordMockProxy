package com.moriable.recordmockproxy.admin.controller;

import com.moriable.recordmockproxy.model.RecordModel;
import spark.Route;

import java.io.File;
import java.util.Map;

public class RecordController extends BaseController {

    private Map<String, RecordModel> recordMap;
    private File recordDir;

    public RecordController(Map<String, RecordModel> recordMap, File recordDir) {
        this.recordMap = recordMap;
        this.recordDir = recordDir;
    }

    public Route get = (request, response) -> {
        // TODO
        return null;
    };

    public Route getAll = (request, response) -> {
        response.type("application/json");
        return recordMap.values();
    };

    public Route deleteAll = (request, response) -> {
        // TODO
        return null;
    };

    public Route request = (request, response) -> {
        // TODO
        return null;
    };

    public Route response = (request, response) -> {
        String recordId = getOriginalId(request.params(":recordId"), 5, 2);

        RecordModel recordDto = recordMap.get(recordId);
        if (recordDto == null) {
            throw new Exception("record not found.");
        }

        String contentType = recordDto.getResponse().getHeaders().get("Content-Type");
        response.type(contentType);

        String encoding = recordDto.getResponse().getHeaders().get("Content-Encoding");
        if (encoding != null && !encoding.isEmpty()) {
            response.header("Content-Encoding", encoding);
        }

        File bodyFile = new File(recordDir.getAbsolutePath() + File.separator + recordDto.getResponse().getBodyfile());
        responseFile(bodyFile, response);
        return response.raw();
    };

    public Route csv = (request, response) -> {
        // TODO
        return null;
    };

}
