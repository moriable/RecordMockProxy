package com.moriable.recordmockproxy.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moriable.recordmockproxy.admin.controller.CertController;
import com.moriable.recordmockproxy.admin.controller.MockController;
import com.moriable.recordmockproxy.admin.controller.RecordController;
import com.moriable.recordmockproxy.admin.controller.WebSocketController;
import com.moriable.recordmockproxy.admin.validator.JsonValidator;
import com.moriable.recordmockproxy.common.ExcludeWithAnotateStrategy;
import com.moriable.recordmockproxy.model.MockStorage;
import com.moriable.recordmockproxy.model.RecordStorage;

import java.io.File;
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

    private WebSocketController webSocketController;
    private CertController certController;
    private RecordController recordController;
    private MockController mockController;

    public RecordMockProxyAdmin(int adminPort, File cert, RecordStorage recordStorage, File recordDir,
                                MockStorage mockStorage, File mockDir) {
        this.port = adminPort;

        webSocketController = new WebSocketController();
        recordController = new RecordController(recordStorage, recordDir);
        mockController = new MockController(mockStorage, mockDir);
        certController = new CertController(cert);

        recordStorage.addListener(webSocketController);
    }

    public void start() {
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExcludeWithAnotateStrategy()).create();

        port(port);

        staticFiles.location("/static");

        webSocket("/api/websocket", webSocketController);

        get("/cert", certController.get);
        path("/api", () -> {
            path("/record", () -> {
                get("", recordController.getRecordAll, gson::toJson);
                delete("", recordController.deleteRecordAll, gson::toJson);
                get("/:recordId", recordController.getRecord, gson::toJson);
                get("/:recordId/request", recordController.getRequestBody);
                get("/:recordId/response", recordController.getResponseBody);
            });
            path("/mock", () -> {
                get("", mockController.getMockAll); // toJson is inside
                post("", mockController.createMock, gson::toJson);
                delete("", mockController.deleteMockAll, gson::toJson);
                get("/:targetId", mockController.getMock, gson::toJson);
                delete("/:targetId", mockController.deleteMcok, gson::toJson);
                put("/:targetId/rule", mockController.changeMockRule, gson::toJson);
                put("/:targetId/order", mockController.changeMockResponseOrder, gson::toJson);
                post("/:targetId", mockController.addMockResponse, gson::toJson);
                get("/:targetId/:responseId", mockController.getMockResponse);
                put("/:targetId/:responseId", mockController.changeMockResponse, gson::toJson);
                delete("/:targetId/:responseId", mockController.deleteMockResponse, gson::toJson);
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
