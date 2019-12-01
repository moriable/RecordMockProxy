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
import spark.Request;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
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
    private File cert;
    private File recordDir;
    private File mockDir;

    public RecordMockProxyAdmin(int adminPort, File cert, Map<String, RecordModel> recordMap, File recordDir,
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
            responseFile(cert, response.raw().getOutputStream());
            return response.raw();
        });
        path("/api", () -> {
            path("/record", () -> {
                get("", (request, response) -> recordMap.values(), gson::toJson);
                delete("", (request, response) -> {
                    // TODO
                    return null;
                });
                get("/:id", (request, response) -> {
                    // TODO
                    return null;
                });
                get("/:recordId/response", (request, response) -> {
                    String recordId = encodeRequestName(request.params(":recordId"));

                    RecordModel recordDto = recordMap.get(recordId);
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
                    responseFile(bodyFile, response.raw().getOutputStream());
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
                    if (request.contentType().startsWith("application/json")) {
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
                            bodyStream.write(form.getResponse().getTextBody().getBytes());
                        }

                        mockStorage.put(model.getTarget().getId(), model);
                        mockStorage.save();

                        response.type("application/json");
                        return gson.toJson(model);
                    } else if (request.contentType().startsWith("multipart/form-data")) {
                        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

                        String json = getFormdataString(request, "mock");
                        MockForm form = gson.fromJson(json, MockForm.class);
                        MockModel model = new MockModel(form);

                        File targetDir = new File(mockDir.getAbsolutePath() + File.separator + model.getTarget().getId());
                        if (targetDir.exists()) {
                            FileUtils.deleteDirectory(targetDir);
                        }
                        targetDir.mkdirs();

                        String filename = String.format("%04d.%s", 0, model.getMockResponses().get(0).getId());
                        File bodyFile = new File(targetDir.getAbsolutePath() + File.separator + filename);
                        pipe(request.raw().getPart("responseBody").getInputStream(), new FileOutputStream(bodyFile));

                        mockStorage.put(model.getTarget().getId(), model);
                        mockStorage.save();

                        response.type("application/json");
                        return gson.toJson(model);
                    }

                    halt(400);
                    return "";
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
                    String targetId = encodeMockId(request.params(":targetId"));

                    File targetDir = new File(mockDir.getAbsolutePath() + File.separator + targetId);
                    System.out.println(targetDir.getAbsolutePath());
                    if (!targetDir.exists()) {
                        response.status(404);
                        response.type("application/json");
                        return "{}";
                    }

                    MockForm.MockResponseForm form = gson.fromJson(request.body(), MockForm.MockResponseForm.class);
                    MockModel.MockResponseModel responseModel = new MockModel.MockResponseModel(form);

                    MockModel model = mockStorage.get(targetId);
                    model.getMockResponses().add(responseModel);

                    String filename = String.format("%04d.%s", model.getMockResponses().size(), responseModel.getId());
                    File body = new File(targetDir.getAbsolutePath() + File.separator + filename);
                    try (OutputStream bodyStream = new FileOutputStream(body)) {
                        bodyStream.write(form.getTextBody().getBytes());
                    }

                    model.commit();
                    mockStorage.save();

                    response.type("application/json");
                    return gson.toJson(model);
                });
                get("/:targetId/:responseId", (request, response) -> {
                    // TODO
                    return null;
                });
                delete("/:targetId/:responseId", (request, response) -> {
                    // TODO
                    return null;
                });
                put("/:targetId/rule", "application/json", (request, response) -> {
                    // TODO
                    return null;
                });
                put("/:targetId/order", "application/json", (request, response) -> {
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

    private String encodeRequestName(String requestName) {
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

    private String encodeMockId(String mockId) {
        if (mockId == null) return "";

        String[] s = mockId.split("\\^");
        if (s.length != 4) {
            return mockId;
        }

        try {
            s[1] = new URLCodec().encode(s[1], "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }

        return s[0] + "^" + s[1] + "^" + s[2] + "^" + s[3];
    }

    private void responseFile(File file, OutputStream os) throws IOException {
        pipe(new FileInputStream(file), os);
    }

    private String getFormdataString(Request request, String name, String defaultValue) throws ServletException, IOException {
        String result = getFormdataString(request, name);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    private String getFormdataString(Request request, String name) throws ServletException, IOException {
        Part part = request.raw().getPart(name);
        if (part == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pipe(part.getInputStream(), out);

        return out.toString();
    }

    private int getFormdataInt(Request request, String name, int defaultValue) throws ServletException, IOException {
        try {
            return getFormdataInt(request, name);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getFormdataInt(Request request, String name) throws ServletException, IOException {
        String data = getFormdataString(request, name);
        return Integer.parseInt(data);
    }

    private void pipe(InputStream input, OutputStream out) throws IOException {
        try (BufferedInputStream is = new BufferedInputStream(input);
             BufferedOutputStream os = new BufferedOutputStream(out)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            os.flush();
        }
    }
}
