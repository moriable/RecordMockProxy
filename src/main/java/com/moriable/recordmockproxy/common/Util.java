package com.moriable.recordmockproxy.common;

import com.moriable.recordmockproxy.admin.form.MockForm;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.net.URLCodec;
import rawhttp.core.RawHttpRequest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class Util {

    public static String getRequestName(RawHttpRequest request, Date date, int port) {
        String reqTime = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date);
        String shortUri = snipUri(request.getUri(), 32);
        String hosthash = getHostHash(request.getUri().getHost(), port);
        String pathhash = getPathHash(request.getUri().getPath(), request.getUri().getPath());

        return reqTime + "^" + request.getMethod() + "^" + shortUri + "^" + hosthash + "^" + pathhash;
    }

    public static String getMockId(RawHttpRequest request, int port) {
        String hosthash = getHostHash(request.getUri().getHost(), port);
        String pathhash = getPathHash(request.getUri().getPath(), request.getUri().getPath());

        return request.getMethod() + "^" + hosthash + "^" + pathhash;
    }

    public static String getMockId(MockForm form) {
        String hosthash = getHostHash(form.getHost(), form.getPort());
        String pathhash = getPathHash(form.getPath(), form.getPath());

        return form.getMethod() + "^" + hosthash + "^" + pathhash;
    }

    public static String getHostHash(String host, int port) {
        return getHash(host + ":" + port);
    }

    public static String getPathHash(String path, String query) {
        if (query == null || query.isEmpty()) {
            return getHash(path);
        }

        return getHash(path + "?" + query);
    }

    public static String snipUri(URI uri, int len) {
        String host = uri.getHost();
        String path = "";
        try {
            path = new URLCodec().encode(uri.getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) { }

        if ((host + path).length() <= len) {
            return host + path;
        }

        if (host.length() >= len) {
            return host.substring(0, len);
        }

        int pathLen = len - host.length() - 1;

        return host + "+" + path.substring(path.length() - pathLen);
    }

    public static String getHash(String str) {
        return Base64.getUrlEncoder().encodeToString(DigestUtils.md5(str)).replaceAll("=", "");
    }

}
