package com.moriable.recordmockproxy.admin.controller;

import com.moriable.recordmockproxy.model.RecordModel;
import com.moriable.recordmockproxy.model.RecordStorage;
import org.apache.commons.io.FileUtils;
import spark.Route;

import java.io.File;
import java.util.HashMap;

public class RecordController extends BaseController {

    private RecordStorage recordStorage;
    private File recordDir;

    public RecordController(RecordStorage recordStorage, File recordDir) {
        this.recordStorage = recordStorage;
        this.recordDir = recordDir;
    }

    public Route getRecord = (request, response) -> {
        String recordId = getOriginalId(request.params(":recordId"), 5, 2);

        response.type("application/json");
        return recordStorage.get(recordId);
    };

    public Route getRecordAll = (request, response) -> {
        response.type("application/json");
        return recordStorage.values();
    };

    public Route deleteRecordAll = (request, response) -> {
        for (File file : recordDir.listFiles()) {
            FileUtils.forceDelete(file);
        }
        recordStorage.clear();

        response.type("application/json");
        return new HashMap<>();
    };

    public Route getRequestBody = (request, response) -> {
        String recordId = getOriginalId(request.params(":recordId"), 5, 2);
        RecordModel recordModel = recordStorage.get(recordId);
        if (recordModel == null) {
            throw new Exception("record not found.");
        }

        if (recordModel.getRequest().getBodyfile() == null) {
            throw new Exception("request body is nothing.");
        }

        String contentType = recordModel.getRequest().getHeaders().get("Content-Type");
        String encoding = recordModel.getRequest().getHeaders().get("Content-Encoding");
        File bodyFile = new File(recordDir.getAbsolutePath() + File.separator + recordModel.getRequest().getBodyfile());

        responseFile(bodyFile, response, contentType, encoding);
        return response.raw();
    };

    public Route getResponseBody = (request, response) -> {
        String recordId = getOriginalId(request.params(":recordId"), 5, 2);

        RecordModel recordModel = recordStorage.get(recordId);
        if (recordModel == null) {
            throw new Exception("record not found.");
        }

        if (recordModel.getResponse().getBodyfile() == null) {
            throw new Exception("request body is nothing.");
        }

        String contentType = recordModel.getResponse().getHeaders().get("Content-Type");
        String encoding = recordModel.getResponse().getHeaders().get("Content-Encoding");
        File bodyFile = new File(recordDir.getAbsolutePath() + File.separator + recordModel.getResponse().getBodyfile());

        responseFile(bodyFile, response, contentType, encoding);
        return response.raw();
    };
}
