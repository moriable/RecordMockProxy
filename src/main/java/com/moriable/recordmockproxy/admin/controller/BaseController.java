package com.moriable.recordmockproxy.admin.controller;

import org.apache.commons.codec.net.URLCodec;
import spark.Request;
import spark.Response;

import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.*;

public abstract class BaseController {
    protected String getOriginalId(String id, int size, int pos) {
        if (id == null) return "";

        String[] names = id.split("\\^");
        if (names.length != size) {
            return id;
        }

        try {
            names[pos] = new URLCodec().encode(names[pos], "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            result.append(names[i]);
            if (i != size - 1) {
                result.append("^");
            }
        }

        return result.toString();
    }

    protected void responseFile(File file, Response response) throws IOException {
        pipe(new FileInputStream(file), response.raw().getOutputStream());
    }

    protected void pipe(InputStream input, OutputStream out) throws IOException {
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

    protected String getFormdataString(Request request, String name, String defaultValue) throws ServletException, IOException {
        String result = getFormdataString(request, name);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    protected String getFormdataString(Request request, String name) throws ServletException, IOException {
        Part part = request.raw().getPart(name);
        if (part == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pipe(part.getInputStream(), out);

        return out.toString();
    }
}
