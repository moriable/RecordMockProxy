package com.moriable.recordmockproxy;

import com.moriable.recordmockproxy.common.Util;
import com.moriable.recordmockproxy.model.MockModel;
import com.moriable.recordmockproxy.model.MockStorage;
import com.moriable.recordmockproxy.model.RecordModel;
import com.moriable.recordmockproxy.model.RecordStorage;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecordMockProxyWorker implements Runnable {
    private final Logger logger = Logger.getLogger(RecordMockProxyWorker.class.getSimpleName() + Thread.currentThread().getId());

    private Socket socket;
    private boolean isSSL;

    private RecordMockProxy serverRef;
    private RawHttp http;
    private File recordDir;
    private File mockDir;
    private RecordStorage recordStorage;
    private MockStorage mockStorage;

    protected RecordMockProxyWorker(Socket socket) {
        this.socket = socket;
        this.isSSL = socket instanceof SSLSocket;
    }

    protected void init(RecordMockProxy server, RawHttp http, RecordStorage recordStorage, File recordDir,
                        MockStorage mockStorage, File mockDir) {
        this.serverRef = server;
        this.http = http;
        this.recordDir = recordDir;
        this.mockDir = mockDir;
        this.recordStorage = recordStorage;
        this.mockStorage = mockStorage;
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

        putRequest(requestName, requestDate, request, isSSL);
        logger.info(requestName);

        String mockId = Util.getMockId(request, port);
        RawHttpResponse response = responseMock(mockId);

        Socket relaysocket = null;
        if (response == null) {
            if (isSSL) {
                relaysocket = SSLSocketFactory.getDefault().createSocket(request.getUri().getHost(), port);
            } else {
                relaysocket = new Socket(request.getUri().getHost(), port);
            }

            if (request.getBody().isPresent()) {
                File requestFile = new File(recordDir.getAbsolutePath() + File.separator + requestName);
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
        String responseName = requestName + "^" + response.getStatusCode() + "^" + contentType;

        if (!socket.isClosed()) {
            File responseFile = new File(recordDir.getAbsolutePath() + File.separator + responseName);
            responseFile.createNewFile();
            try(FileOutputStream responseStream = new FileOutputStream(responseFile)) {
                response.writeTo(new OutputStream[]{socket.getOutputStream(), responseStream}, 8192);
            }
            socket.close();
        } else {
            logger.warning("client socket closed");
        }

        putResponse(requestName, responseName, response, new Date().getTime() - requestDate.getTime());

        if (relaysocket != null) {
            relaysocket.close();
        }
    }

    public void putRequest(String requestName, Date date, RawHttpRequest request, boolean isSSL) {
        RecordModel recordModel = new RecordModel();
        recordModel.setId(requestName);
        recordModel.setDate(date.getTime());

        int port = request.getUri().getPort();
        if (port == -1 && isSSL) {
            port = 443;
        } else if (port == -1 && !isSSL) {
            port = 80;
        }

        RecordModel.RequestModel requestModel = new RecordModel.RequestModel();
        requestModel.setMethod(request.getMethod());
        requestModel.setHost(request.getUri().getHost());
        requestModel.setPort(port);
        requestModel.setPath(request.getUri().getPath());
        requestModel.setQuery(request.getUri().getQuery());
        requestModel.setHeaders(new HashMap<>());
        request.getHeaders().getHeaderNames().forEach(s -> {
            requestModel.getHeaders().put(s, request.getHeaders().get(s).get(0));
            if (s.equals("Content-Type")) {
                requestModel.setContentType(request.getHeaders().get(s).get(0));
            }
        });
        if (request.getBody().isPresent()) {
            requestModel.setBodyfile(requestName);
        }

        recordModel.setRequest(requestModel);

        recordStorage.put(requestName, recordModel);
        recordStorage.notifyRequest(recordModel);
    }

    public void putResponse(String requestName, String responseName, RawHttpResponse response, long time) {
        RecordModel recordModel = recordStorage.get(requestName);

        RecordModel.ResponseModel responseModel = new RecordModel.ResponseModel();
        responseModel.setStatusCode(response.getStatusCode());
        responseModel.setHeaders(new HashMap<>());
        response.getHeaders().getHeaderNames().forEach(s -> {
            responseModel.getHeaders().put(s, response.getHeaders().get(s).get(0));
            if (s.equals("Content-Type")) {
                responseModel.setContentType(response.getHeaders().get(s).get(0));
            }
        });
        responseModel.setBodyfile(responseName);
        responseModel.setTime(time);

        recordModel.setResponse(responseModel);
        recordStorage.notifyResponse(recordModel);
    }

    private RawHttpResponse responseMock(String mockId) {
        MockModel mock = mockStorage.get(mockId);
        if (mock == null) {
            return null;
        }

        int call = mockStorage.call(mockId);
        List<MockModel.MockResponseModel> enableResponses = new ArrayList<>();
        mock.getMockResponses().stream().filter(mockResponseModel -> mockResponseModel.isEnable()).forEach(mockResponseModel -> {
            enableResponses.add(mockResponseModel);
        });

        int i = -1;
        if (MockModel.MockRule.ONCE == mock.getRule()) {
            if (enableResponses.size() <= call) {
                return null;
            }
            i = call;
        } else if (MockModel.MockRule.REPEAT == mock.getRule()) {
            i = call % enableResponses.size();
        } else if (MockModel.MockRule.RANDOM == mock.getRule()) {
            i = new Random().nextInt(enableResponses.size());
        } else if (MockModel.MockRule.PROXY == mock.getRule()) {
            return null;
        }

        File targetDir = new File(mockDir.getAbsolutePath() + File.separator + mockId);
        if (!targetDir.exists()) {
            return null;
        }

        try {
            InputStream bodyStream = new FileInputStream(targetDir.getAbsolutePath() + File.separator + enableResponses.get(i).getId());
            InputStream headStream = enableResponses.get(i).getHeaderStream();
            SequenceInputStream inputStream = new SequenceInputStream(
                    headStream,
                    bodyStream
            );
            return http.parseResponse(inputStream);
        } catch (IOException e) {
            logger.warning(e.getMessage());
            return null;
        }
    }
}
