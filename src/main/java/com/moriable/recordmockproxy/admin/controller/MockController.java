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

public class MockController extends BaseController {

    private JsonValidator validator;
    private File mockDir;
    private MockStorage mockStorage;

    public MockController(MockStorage mockStorage, File mockDir) {
        this.mockStorage = mockStorage;
        this.mockDir = mockDir;
        this.validator = new JsonValidator();
    }

    public Route getAll = (request, response) -> {
        response.type("application/json");
        return mockStorage.dump();
    };

    public Route create = (request, response) -> {
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

    public Route deleteAll = (request, response) -> {
        return null;
    };

    public Route get = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);
        response.type("application/json");
        return mockStorage.get(targetId);
    };

    public Route delete = (request, response) -> {
        return null;
    };

    public Route addResponse = (request, response) -> {
        String targetId = getOriginalId(request.params(":targetId"), 4, 1);

        response.type("application/json");
        if (request.contentType().startsWith("application/json")) {
            return addResponse(targetId, request.body(), null, "text/plain");
        } else if (request.contentType().startsWith("multipart/form-data")) {
            return addResponse(targetId,
                    getFormdataString(request, "form"),
                    request.raw().getPart("responseBody").getInputStream(),
                    request.raw().getPart("responseBody").getContentType()
            );
        }

        throw new Exception("invalid Content-Type");
    };

    public Route getResponse = (request, response) -> {
        return null;
    };

    public Route changeResponse = (request, response) -> {
        return null;
    };

    public Route deleteResponse = (request, response) -> {
        return null;
    };

    public Route changeRule = (request, response) -> {
        return null;
    };

    public Route changeOrder = (request, response) -> {
        return null;
    };

    private MockModel createMock(String formString, InputStream body, String defaultContentType) throws JsonValidator.JsonValidatorException, IOException {
        String json = validator.validate("/mockForm.schema.json", formString);
        MockForm form = new Gson().fromJson(json, MockForm.class);

        MockModel model = new MockModel(form);

        File targetDir = new File(mockDir.getAbsolutePath() + File.separator + model.getTarget().getId());
        if (targetDir.exists()) {
            FileUtils.deleteDirectory(targetDir);
        }
        targetDir.mkdirs();

        if (body == null) {
            body = new ByteArrayInputStream(form.getResponse().getTextBody().getBytes());
        }

        addResponseBody(model.getTarget().getId(), model, body, defaultContentType);

        mockStorage.put(model.getTarget().getId(), model);
        mockStorage.save();

        return model;
    }

    private MockModel addResponse(String targetId, String formString, InputStream body, String defaultContentType) throws JsonValidator.JsonValidatorException, IOException {
        String json = validator.validate("/mockResponseForm.schema.json", formString);

        MockForm.MockResponseForm form = new Gson().fromJson(json, MockForm.MockResponseForm.class);
        MockModel.MockResponseModel responseModel = new MockModel.MockResponseModel(form);

        if (body == null) {
            body = new ByteArrayInputStream(form.getTextBody().getBytes());
        }

        MockModel mockModel = mockStorage.get(targetId);
        mockModel.getMockResponses().add(responseModel);

        addResponseBody(targetId, mockModel, body, defaultContentType);

        mockModel.commit();
        mockStorage.save();

        return mockModel;
    }

    private void addResponseBody(String targetId, MockModel mockModel, InputStream body, String defaultContentType) throws IOException {
        File targetDir = new File(mockDir.getAbsolutePath() + File.separator + targetId);
        if (!targetDir.exists()) {
            throw new IllegalStateException("target directory not found.");
        }

        int i = mockModel.getMockResponses().size() - 1;

        String filename = String.format("%04d.%s", i, mockModel.getMockResponses().get(i).getId());
        File bodyFile = new File(targetDir.getAbsolutePath() + File.separator + filename);
        pipe(body, new FileOutputStream(bodyFile));

        if (!mockModel.getMockResponses().get(i).getHeaders().containsKey("Content-Type")) {
            mockModel.getMockResponses().get(i).getHeaders().put("Content-Type", defaultContentType);
        }
    }
}
