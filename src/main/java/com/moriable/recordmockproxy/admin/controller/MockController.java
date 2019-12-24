package com.moriable.recordmockproxy.admin.controller;

import com.google.gson.Gson;
import com.moriable.recordmockproxy.admin.form.MockForm;
import com.moriable.recordmockproxy.admin.validator.JsonValidator;
import com.moriable.recordmockproxy.model.MockModel;
import com.moriable.recordmockproxy.model.MockStorage;
import org.apache.commons.io.FileUtils;
import spark.Route;

import javax.servlet.MultipartConfigElement;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockController extends BaseController {

    private JsonValidator validator;
    private File mockDir;
    private MockStorage mockStorage;

    public MockController(MockStorage mockStorage, File mockDir) {
        this.mockStorage = mockStorage;
        this.mockDir = mockDir;
        this.validator = new JsonValidator();
    }

    public Route getMockAll = (request, response) -> {
        response.type("application/json");
        return mockStorage.dump();
    };

    public Route createMock = (request, response) -> {
        response.type("application/json");
        if (request.contentType().startsWith("application/json")) {
            return createMock(request.body(), null, "text/plain");
        } else if (request.contentType().startsWith("multipart/form-data")) {
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            return createMock(
                    getFormdataString(request, "form"),
                    request.raw().getPart("responseBody").getInputStream(),
                    request.raw().getPart("responseBody").getContentType()
            );
        }

        throw new Exception("invalid Content-Type");
    };

    public Route deleteMockAll = (request, response) -> {
        FileUtils.deleteQuietly(mockDir);
        mockDir.mkdirs();

        mockStorage.clear();

        response.type("application/json");
        return new HashMap<>();
    };

    public Route getMock = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);
        response.type("application/json");
        return mockStorage.get(targetId);
    };

    public Route deleteMcok = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);
        mockStorage.remove(targetId);
        mockStorage.save();

        File targetDir = new File(mockDir.getAbsolutePath() + File.separator + targetId);
        FileUtils.deleteQuietly(targetDir);

        response.type("application/json");
        return new HashMap<>();
    };

    public Route addMockResponse = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);

        response.type("application/json");
        if (request.contentType().startsWith("application/json")) {
            return addResponse(targetId, null, request.body(), null, "text/plain", -1);
        } else if (request.contentType().startsWith("multipart/form-data")) {
            return addResponse(targetId,
                    null,
                    getFormdataString(request, "form"),
                    request.raw().getPart("responseBody").getInputStream(),
                    request.raw().getPart("responseBody").getContentType(),
                    -1
            );
        }

        throw new Exception("invalid Content-Type");
    };

    public Route getMockResponse = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);
        String responseId = request.params(":responseId");

        MockModel mockModel = mockStorage.get(targetId);
        if (mockModel == null) {
            throw new Exception("mock not found.");
        }

        MockModel.MockResponseModel mockResponseModel = mockModel.getMockResponses().stream().filter(m -> m.getId().equals(responseId))
                .findFirst().get();

        if (mockResponseModel == null) {
            throw new Exception("response not found.");
        }

        String contentType = mockResponseModel.getHeaders().get("Content-Type");
        String encoding = mockResponseModel.getHeaders().get("Content-Encoding");
        File bodyFile = new File(mockDir.getAbsolutePath() + File.separator + targetId + File.separator + mockResponseModel.getId());

        responseFile(bodyFile, response, contentType, encoding);
        return response.raw();
    };

    public Route changeMockResponse = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);
        String responseId = request.params(":responseId");

        int pos = deleteResponse(targetId, responseId);

        response.type("application/json");
        if (request.contentType().startsWith("application/json")) {
            return addResponse(targetId, responseId, request.body(), null, "text/plain", pos);
        } else if (request.contentType().startsWith("multipart/form-data")) {
            return addResponse(targetId,
                    responseId,
                    getFormdataString(request, "form"),
                    request.raw().getPart("responseBody").getInputStream(),
                    request.raw().getPart("responseBody").getContentType(),
                    pos
            );
        }

        throw new Exception("invalid Content-Type");
    };

    public Route deleteMockResponse = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);
        String responseId = request.params(":responseId");

        deleteResponse(targetId, responseId);

        response.type("application/json");
        return new HashMap<>();
    };

    public Route changeMockRule = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);
        Map<String, String> form = new Gson().fromJson(request.body(), Map.class);
        MockModel.MockRule rule = MockModel.MockRule.valueOf(form.get("rule"));

        MockModel mockModel = mockStorage.get(targetId);
        mockModel.setRule(rule);
        mockModel.commit();
        mockStorage.save();

        response.type("application/json");
        return mockModel;
    };

    public Route changeMockResponseOrder = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);
        List<String> form = new Gson().fromJson(request.body(), List.class);

        MockModel mockModel = mockStorage.get(targetId);
        mockModel.getMockResponses().sort((a, b) -> {
            int ia = form.indexOf(a.getId());
            if (ia == -1) ia = Integer.MAX_VALUE;
            int ib = form.indexOf(b.getId());
            if (ib == -1) ib = Integer.MAX_VALUE;
            return ia - ib;
        });
        mockModel.commit();
        mockStorage.save();

        response.type("application/json");
        return mockModel;
    };

    public MockModel createMock(String formString, InputStream body, String defaultContentType) throws JsonValidator.JsonValidatorException, IOException {
        String json = validator.validate("/mockForm.schema.json", formString);
        MockForm form = new Gson().fromJson(json, MockForm.class);

        return createMock(form, body, defaultContentType);
    }

    public MockModel createMock(MockForm form, InputStream body, String defaultContentType) throws JsonValidator.JsonValidatorException, IOException {
        MockModel model = new MockModel(form);

        File targetDir = new File(mockDir.getAbsolutePath() + File.separator + model.getTarget().getId());
        if (targetDir.exists()) {
            FileUtils.deleteDirectory(targetDir);
        }
        targetDir.mkdirs();

        if (body == null) {
            body = new ByteArrayInputStream(form.getResponse().getTextBody().getBytes());
        }

        addResponseBody(model.getTarget().getId(), model.getMockResponses().get(0), body, defaultContentType);

        mockStorage.put(model.getTarget().getId(), model);
        mockStorage.save();

        return model;
    }

    private MockModel addResponse(String targetId, String responseId, String formString, InputStream body, String defaultContentType, int addPos) throws JsonValidator.JsonValidatorException, IOException {
        String json = validator.validate("/mockResponseForm.schema.json", formString);

        MockForm.MockResponseForm form = new Gson().fromJson(json, MockForm.MockResponseForm.class);
        MockModel.MockResponseModel responseModel = new MockModel.MockResponseModel(form, responseId);

        if (body == null) {
            body = new ByteArrayInputStream(form.getTextBody().getBytes());
        }

        MockModel mockModel = mockStorage.get(targetId);
        if (addPos < 0) {
            mockModel.getMockResponses().add(responseModel);
        } else {
            mockModel.getMockResponses().add(addPos, responseModel);
        }

        addResponseBody(targetId, responseModel, body, defaultContentType);

        mockModel.commit();
        mockStorage.save();

        return mockModel;
    }

    private void addResponseBody(String targetId, MockModel.MockResponseModel mockResponseModel, InputStream body, String defaultContentType) throws IOException {
        File targetDir = new File(mockDir.getAbsolutePath() + File.separator + targetId);
        if (!targetDir.exists()) {
            throw new IllegalStateException("target directory not found.");
        }

        File bodyFile = new File(targetDir.getAbsolutePath() + File.separator + mockResponseModel.getId());
        pipe(body, new FileOutputStream(bodyFile));

        if (!mockResponseModel.getHeaders().containsKey("Content-Type")) {
            mockResponseModel.getHeaders().put("Content-Type", defaultContentType);
        }
    }

    private int deleteResponse(String targetId, String responseId) {
        MockModel mockModel = mockStorage.get(targetId);
        int pos = -1;
        for (int i = 0; i < mockModel.getMockResponses().size(); i++) {
            if (mockModel.getMockResponses().get(i).getId().equals(responseId)) {
                pos = i;
                break;
            }
        }

        if (pos == -1) {
            throw new IllegalArgumentException("response not found.");
        }

        MockModel.MockResponseModel mockResponseModel = mockModel.getMockResponses().remove(pos);

        File targetDir = new File(mockDir.getAbsolutePath() + File.separator + targetId);
        if (!targetDir.exists()) {
            throw new IllegalStateException("target directory not found.");
        }

        File bodyFile = new File(targetDir.getAbsolutePath() + File.separator + mockResponseModel.getId());
        bodyFile.delete();

        mockModel.commit();
        mockStorage.save();

        return pos;
    }
}
