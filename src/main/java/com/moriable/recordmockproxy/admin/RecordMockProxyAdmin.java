package com.moriable.recordmockproxy.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moriable.recordmockproxy.admin.controller.CertController;
import com.moriable.recordmockproxy.admin.controller.MockController;
import com.moriable.recordmockproxy.admin.controller.RecordController;
import com.moriable.recordmockproxy.admin.validator.JsonValidator;
import com.moriable.recordmockproxy.common.ExcludeWithAnotateStrategy;
import com.moriable.recordmockproxy.model.MockStorage;
import com.moriable.recordmockproxy.model.RecordModel;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;

public class RecordMockProxyAdmin {

    private final Logger logger = Logger.getLogger(RecordMockProxyAdmin.class.getSimpleName());

    private int port;

    private CertController certController;
    private RecordController recordController;
    private MockController mockController;

    public RecordMockProxyAdmin(int adminPort, File cert, Map<String, RecordModel> recordMap, File recordDir,
                                MockStorage mockStorage, File mockDir) {
        this.port = adminPort;

        recordController = new RecordController(recordMap, recordDir);
        mockController = new MockController(mockStorage, mockDir);
        certController = new CertController(cert);
    }

    public void start() {
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExcludeWithAnotateStrategy()).create();

        port(port);

        get("/cert", certController.get);
        path("/api", () -> {
            path("/record", () -> {
                get("", recordController.getAll, gson::toJson);
                delete("", recordController.deleteAll, gson::toJson);
                get("/:recordId", recordController.get, gson::toJson);
                get("/:recordId/request", recordController.request);
                get("/:recordId/response", recordController.response);
                get("/csv", recordController.csv);
            });
            path("/mock", () -> {
                get("", mockController.getAll); // toJson is inside
                post("", mockController.create, gson::toJson);
                delete("", mockController.deleteAll, gson::toJson);
                get("/:targetId", mockController.get, gson::toJson);
                delete("/:targetId", mockController.delete, gson::toJson);
                post("/:targetId", mockController.addResponse, gson::toJson);
                get("/:targetId/:responseId", mockController.getResponse);
                put("/:targetId/:responseId", mockController.changeResponse, gson::toJson);
                delete("/:targetId/:responseId", mockController.deleteResponse, gson::toJson);
                put("/:targetId/rule", mockController.changeRule, gson::toJson);
                put("/:targetId/order", mockController.changeOrder, gson::toJson);
            });
        });

        exception(Exception.class, (exception, request, response) -> {
            logger.log(Level.SEVERE, exception.getMessage(), exception);
            Map<String, List<String>> result = new HashMap<>();
            List<String> errors = new ArrayList<>();
            result.put("errors", errors);
            if (exception instanceof JsonValidator.JsonValidatorException) {
                errors.addAll(((JsonValidator.JsonValidatorException) exception).getErrors());
            } else {
                errors.add(exception.getMessage());
            }

            response.status(400);
            response.body(gson.toJson(result));
        });
    }

    public void stop() {
        stop();
    }
}
