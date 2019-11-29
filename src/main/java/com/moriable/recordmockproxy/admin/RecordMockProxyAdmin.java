package com.moriable.recordmockproxy.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moriable.recordmockproxy.admin.form.MockForm;
import com.moriable.recordmockproxy.common.ExcludeWithAnotateStrategy;
import com.moriable.recordmockproxy.model.MockModel;
import com.moriable.recordmockproxy.model.ModelStorage;
import com.moriable.recordmockproxy.model.RecordModel;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;

public class RecordMockProxyAdmin {

    private final Logger logger = Logger.getLogger(RecordMockProxyAdmin.class.getSimpleName());

    Map<String, RecordModel> recordMap;
    ModelStorage<String, MockModel> mockStorage;

    private int port;
    private String cert;
    private File recordDir;
    private File mockDir;

    public RecordMockProxyAdmin(int adminPort, String cert, Map<String, RecordModel> recordMap, File recordDir,
                                ModelStorage<String, MockModel> mockStorage, File mockDir) {
        this.port = adminPort;
        this.cert = cert;
        this.recordDir = recordDir;
        this.mockDir = mockDir;

        this.recordMap = recordMap;
        this.mockStorage = mockStorage;
    }

    public void start() {
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExcludeWithAnotateStrategy()).create();

        port(port);
        get("/cert", (request, response) -> {
            response.type("application/x-x509-ca-cert");
            try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(cert));
                 BufferedOutputStream output = new BufferedOutputStream(response.raw().getOutputStream())) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = input.read(buffer)) > 0) {
                    output.write(buffer, 0, len);
                }

                output.flush();
            }

            return response.raw();
        });
        path("/api", () -> {
            path("/record", () -> {
                get("", (request, response) -> {
                    response.type("application/json");
                    return gson.toJson(recordMap.values());
                });
                delete("", (request, response) -> {
                    // TODO
                    return null;
                });
                get("/:id", (request, response) -> {
                    // TODO
                    return null;
                });
                get("/:id/response", (request, response) -> {
                    String requestName = decodeRequestName(request.params(":id"));

                    logger.info(requestName);

                    RecordModel recordDto = recordMap.get(requestName);
                    if (recordDto == null) {
                        response.status(404);
                        return "{}";
                    }

                    String contentType = recordDto.getResponse().getHeaders().get("Content-Type");
                    response.type(contentType);

                    String encoding = recordDto.getResponse().getHeaders().get("Content-Encoding");
                    if (encoding != null && !encoding.isEmpty()) {
                        response.header("Content-Encoding", encoding);
                    }

                    File bodyFile = new File(recordDir.getAbsolutePath() + File.separator + recordDto.getResponse().getBodyfile());
                    try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(bodyFile));
                         BufferedOutputStream output = new BufferedOutputStream(response.raw().getOutputStream())) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = input.read(buffer)) > 0) {
                            output.write(buffer, 0, len);
                        }

                        output.flush();
                    }

                    return response.raw();
                });
                get("/csv", (request, response) -> {
                    // TODO
                    return null;
                });
            });
            path("/mock", () -> {
                get("", (request, response) -> {
                    // TODO
                    return null;
                });
                post("", (request, response) -> {
                    MockForm form = gson.fromJson(request.body(), MockForm.class);

                    MockModel model = new MockModel(form);

                    File targetDir = new File(mockDir.getAbsolutePath() + File.separator + model.getTarget().getId());
                    if (targetDir.exists()) {
                        FileUtils.deleteDirectory(targetDir);
                    }
                    targetDir.mkdirs();

                    String filename = String.format("%04d.%s", 0, model.getMockResponses().get(0).getId());
                    File body = new File(targetDir.getAbsolutePath() + File.separator + filename);
                    try (OutputStream bodyStream = new FileOutputStream(body)) {
                        bodyStream.write(form.getResponseBody().getBytes());
                    }

                    mockStorage.put(model.getTarget().getId(), model);
                    mockStorage.save();

                    response.type("application/json");
                    return gson.toJson(model);
                });
                get("/:targetId", (request, response) -> {
                    // TODO
                    return null;
                });
                delete("/:targetId", (request, response) -> {
                    // TODO
                    return null;
                });
                post("/:targetId", (request, response) -> {
                    // TODO
                    return null;
                });
                get("/:targetId/:responseId", (request, response) -> {
                    // TODO
                    return null;
                });
                delete("/:targetId/:responseId", (request, response) -> {
                    // TODO
                    return null;
                });
                put("/:targetId/rule", (request, response) -> {
                    // TODO
                    return null;
                });
                put("/:targetId/order", (request, response) -> {
                    // TODO
                    return null;
                });
            });
        });

        exception(Exception.class, (exception, request, response) -> {
            logger.log(Level.SEVERE, exception.getMessage(), exception);
        });
    }

    public void stop() {
        stop();
    }

    private String decodeRequestName(String requestName) {
        if (requestName == null) return "";

        String[] names = requestName.split("\\^");
        if (names.length != 5) {
            return requestName;
        }

        try {
            names[2] = new URLCodec().encode(names[2], "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }

        return names[0] + "^" + names[1] + "^" + names[2] + "^" + names[3] + "^" + names[4];
    }

}
