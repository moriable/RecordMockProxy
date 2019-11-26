package com.moriable.recordmockproxy;

import com.moriable.recordmockproxy.admin.RecordMockProxyAdmin;
import com.moriable.recordmockproxy.common.Util;
import rawhttp.core.RawHttp;
import rawhttp.core.RawHttpRequest;
import rawhttp.core.RawHttpResponse;
import rawhttp.core.errors.InvalidHttpRequest;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecordMockProxyWorker implements Runnable {
    private final Logger logger = Logger.getLogger(RecordMockProxyWorker.class.getSimpleName() + Thread.currentThread().getId());

    private Socket socket;
    private boolean isSSL;

    private RecordMockProxy serverRef;
    private RecordMockProxyService service;
    private RawHttp http;
    private RecordMockProxyAdmin admin;

    protected RecordMockProxyWorker(Socket socket) {
        this.socket = socket;
        this.isSSL = socket instanceof SSLSocket;
    }

    protected void init(RecordMockProxy server, RecordMockProxyService service, RawHttp http, RecordMockProxyAdmin admin) {
        this.serverRef = server;
        this.service = service;
        this.http = http;
        this.admin = admin;
    }

    @Override
    public void run() {
        RawHttpRequest request = null;
        try {
            request = http.parseRequest(socket.getInputStream());

            if ("CONNECT".equals(request.getMethod())) {
                doConnect(request);
            } else {
                doRequest(request);
            }
        } catch (InvalidHttpRequest e) {
            logger.warning("non http");
            try {
                if (!socket.isClosed()) socket.close();
            } catch (IOException ex) { }
        } catch (Exception e) {
            String uri = "";
            if (request != null) {
                uri = request.getUri().toString();
            }
            logger.log(Level.SEVERE, e.getMessage() + " " + uri, e);
            try {
                if (!socket.isClosed()) socket.close();
            } catch (IOException ex) { }
        }
    }

    private void doConnect(RawHttpRequest request) throws Exception {
        http.parseResponse("HTTP/1.1 200 Connection Established\r\n" +
                "\r\n").writeTo(socket.getOutputStream());

        KeyStore ks = RecordMockProxyCA.getKeyStoreWithCertificate(request.getUri().getHost());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
        kmf.init( ks, "password".toCharArray() );
        SSLContext sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( kmf.getKeyManagers() , null , null );
        SSLSocketFactory factory = sslContext.getSocketFactory();

        SSLSocket ssl = (SSLSocket) factory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
        ssl.setSoTimeout(30000);
        ssl.setUseClientMode(false);

        ssl.addHandshakeCompletedListener(event -> {
        });

        ssl.startHandshake();

        serverRef.submitWorker(new RecordMockProxyWorker((Socket) ssl));
    }

    private void doRequest(RawHttpRequest request) throws IOException, URISyntaxException {
        Date requestDate = new Date();

        int port = request.getUri().getPort();
        if (port == -1 && isSSL) {
            port = 443;
        } else if (port == -1 && !isSSL) {
            port = 80;
        }

        String requestName = Util.getRequestName(request, requestDate, port);

        admin.putRequest(requestName, requestDate, request, isSSL);
        logger.info(requestName);

        RawHttpResponse response = null;
        String mockId = Util.getMockId(request, port);
        File mockDir = new File("mock/" + mockId);
        logger.info(mockId);
        if (mockDir.exists()) {
            response = responseMock(mockDir);
        }

        Socket relaysocket = null;
        if (response == null) {
            if (isSSL) {
                relaysocket = SSLSocketFactory.getDefault().createSocket(request.getUri().getHost(), port);
            } else {
                relaysocket = new Socket(request.getUri().getHost(), port);
            }

            if (request.getBody().isPresent()) {
                File requestFile = new File("record/" + requestName);
                requestFile.createNewFile();
                try (FileOutputStream requestStrema = new FileOutputStream(requestFile)) {
                    request.writeTo(new OutputStream[]{relaysocket.getOutputStream(), requestStrema}, 8192);
                }
            } else {
                request.writeTo(new OutputStream[]{relaysocket.getOutputStream()}, 8192);
            }

            response = http.parseResponse(relaysocket.getInputStream());
        }

        String contentType = "Unknown";
        if (response.getHeaders().get("Content-Type") != null && response.getHeaders().get("Content-Type").size() > 0) {
            contentType = response.getHeaders().get("Content-Type").get(0);
            contentType = contentType.substring(contentType.indexOf("/") + 1);
        }
        String responseName = requestName + "^" + response.getStatusCode() + "^" + contentType + "^" + (new Date().getTime() - requestDate.getTime());

        if (!socket.isClosed()) {
            File responseFile = new File("record/" + responseName);
            responseFile.createNewFile();
            try(FileOutputStream responseStream = new FileOutputStream(responseFile)) {
                response.writeTo(new OutputStream[]{socket.getOutputStream(), responseStream}, 8192);
            }
            socket.close();
        } else {
            logger.warning("client socket closed");
        }

        admin.putResponse(requestName, responseName, response);

        if (relaysocket != null) {
            relaysocket.close();
        }
    }

    private RawHttpResponse responseMock(File mockDir) {
        try {
            SequenceInputStream inputStream = new SequenceInputStream(
                    new FileInputStream(mockDir.getAbsolutePath() + "/head"),
                    new FileInputStream(mockDir.getAbsolutePath() + "/body")
            );
            return http.parseResponse(inputStream);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            return null;
        }
    }
}
