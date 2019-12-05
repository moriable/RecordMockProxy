package com.moriable.recordmockproxy.admin.controller;

import spark.Route;

import java.io.File;

public class CertController extends BaseController {

    private File cert;

    public CertController(File cert) {
        this.cert = cert;
    }

    public Route get = (request, response) -> {
        response.type("application/x-x509-ca-cert");
        responseFile(cert, response);
        return response.raw();
    };
}
