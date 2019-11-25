package com.moriable.recordmockproxy.admin;

import com.google.gson.Gson;
import com.moriable.recordmockproxy.admin.dto.RecordDto;
import com.moriable.recordmockproxy.admin.form.MockForm;
import com.moriable.recordmockproxy.common.Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.net.URLCodec;
import rawhttp.core.RawHttpRequest;
import rawhttp.core.RawHttpResponse;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;

public class RecordMockProxyAdmin {

    private final Logger logger = Logger.getLogger(RecordMockProxyAdmin.class.getSimpleName());

    Map<String, RecordDto> record = Collections.synchronizedMap(new LinkedHashMap<>());

    public void start() {
        Gson gson = new Gson();

        path("/api", () -> {
           path("/record", () -> {
               get("", (request, response) -> {
                   response.type("application/json");
                   return gson.toJson(record.values());
               });
               get("/response/:id", (request, response) -> {
                   String requestName = decodeRequestName(request.params(":id"));

                   logger.info(requestName);

                   RecordDto recordDto = record.get(requestName);
                   if (recordDto == null) {
                       response.status(404);
                       return "{}";
                   }

                   String contentType = recordDto.getResponse().getHeaders().get("Content-Type");
                   String encoding = recordDto.getResponse().getHeaders().get("Content-Encoding");

                   response.type(contentType);
                   if (encoding != null && !encoding.isEmpty()) {
                       response.header("Content-Encoding", encoding);
                   }

                   File bodyFile = new File("record/" + recordDto.getResponse().getBodyfile());
                   try(BufferedInputStream input = new BufferedInputStream(new FileInputStream(bodyFile));
                       BufferedOutputStream output = new BufferedOutputStream(response.raw().getOutputStream())) {
                       byte[] buffer = new byte[4096];
                       int len;
                       while ((len = input.read(buffer)) > 0) {
                           output.write(buffer,0,len);
                       }

                       output.flush();
                   }

                   return response.raw();
               });
           });
           path("/mock", () -> {
               post("", (request, response) -> {
                   MockForm form = gson.fromJson(request.body(), MockForm.class);

                   String mockId = Util.getMockId(form);
                   File mockDir = new File("mock/" + mockId);
                   if (!mockDir.exists()) {
                       mockDir.mkdirs();
                   }

                   if (form.getResponseHeaders() != null) {
                       File head = new File(mockDir.getAbsolutePath() + "/head");
                       try(OutputStream headStream = new FileOutputStream(head)) {
                           StringBuilder b = new StringBuilder();
                           form.getResponseHeaders().forEach((key, value) -> {
                               b.append(key + ": " + value + "\r\n");
                           });
                       }
                   }

                   if (form.getResponseHeaders() != null) {
                       StringBuilder b = new StringBuilder();
                       form.getResponseHeaders().forEach((key, value) -> {
                           b.append(key + ": " + value + "\r\n");
                       });
                       b.append("\r\n");

                       File head = new File(mockDir.getAbsolutePath() + "/head");
                       try(OutputStream headStream = new FileOutputStream(head)) {
                           for(String key : form.getResponseHeaders().keySet()){
                               String value = form.getResponseHeaders().get(key);
                               headStream.write((key + ": " + value + "\r\n").getBytes());
                           }
                       }
                   }

                   if (form.getResponseBody() != null) {
                       File body = new File(mockDir.getAbsolutePath() + "/body^" + new URLCodec().encode(form.getResponseType()));
                       try(OutputStream bodyStream = new FileOutputStream(body)) {
                           bodyStream.write(form.getResponseBody().getBytes());
                       }
                   }

                   response.type("application/json");
                   return "{\"id\":\"" + mockId + "\"}";
               });
               put("/:id/body", (request, response) -> {
                   response.type("application/json");
                   return "{}";
               });
           });
        });

        exception(Exception.class, (exception, request, response) -> {
            logger.log(Level.SEVERE, exception.getMessage(), exception);
        });
    }

    public void putRequest(String requestName, Date date, RawHttpRequest request, boolean isSSL) {
        RecordDto dto = new RecordDto();
        dto.setId(requestName);
        dto.setDate(date.getTime());

        int port = request.getUri().getPort();
        if (port == -1 && isSSL) {
            port = 443;
        } else if (port == -1 && !isSSL) {
            port = 80;
        }

        RecordDto.RequestDto requestDto = new RecordDto.RequestDto();
        requestDto.setHost(request.getUri().getHost());
        requestDto.setPort(port);
        requestDto.setPath(request.getUri().getPath());
        requestDto.setQuery(request.getUri().getQuery());
        requestDto.setHeaders(new HashMap<>());
        request.getHeaders().getHeaderNames().forEach(s -> {
            requestDto.getHeaders().put(s, request.getHeaders().get(s).get(0));
        });
        if (request.getBody().isPresent()) {
            requestDto.setBodyfile(requestName);
        }

        dto.setRequest(requestDto);

        record.put(requestName, dto);
    }

    public void putResponse(String requestName, String responseName, RawHttpResponse response) {
        RecordDto dto = record.get(requestName);

        RecordDto.ResponseDto responseDto = new RecordDto.ResponseDto();
        responseDto.setStatusCode(response.getStatusCode());
        responseDto.setHeaders(new HashMap<>());
        response.getHeaders().getHeaderNames().forEach(s -> {
            responseDto.getHeaders().put(s, response.getHeaders().get(s).get(0));
        });
        responseDto.setBodyfile(responseName);

        dto.setResponse(responseDto);
    }

    private String decodeRequestName(String requestName) {
        if (requestName == null) return "";

        String[] names = requestName.split("\\^");
        if (names.length != 5) {
            return requestName;
        }

        try {
            names[2] = new URLCodec().encode(names[2], "UTF-8");
        } catch (UnsupportedEncodingException e) { }

        return names[0] + "^" + names[1] + "^" + names[2] + "^" + names[3] + "^" + names[4];
    }

}
