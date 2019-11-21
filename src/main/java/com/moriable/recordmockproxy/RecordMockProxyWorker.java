package com.moriable.recordmockproxy;

import rawhttp.core.RawHttp;
import rawhttp.core.RawHttpRequest;
import rawhttp.core.RawHttpResponse;
import rawhttp.core.errors.InvalidHttpRequest;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecordMockProxyWorker implements Runnable {
    private final Logger logger = Logger.getLogger(RecordMockProxyWorker.class.getSimpleName());
    private Socket socket;
    private RecordMockProxy serverRef;
    private RawHttp http = new RawHttp();
    private boolean isSSL;

    protected RecordMockProxyWorker(Socket socket, RecordMockProxy server) {
        this.socket = socket;
        this.serverRef = server;
        this.isSSL = socket instanceof SSLSocket;
    }

    @Override
    public void run() {
        try {
            RawHttpRequest request = http.parseRequest(socket.getInputStream());

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
            logger.log(Level.SEVERE, e.getMessage(), e);
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

        serverRef.submitWorker(new RecordMockProxyWorker((Socket) ssl, serverRef));
    }

    private void doRequest(RawHttpRequest request) throws IOException, URISyntaxException {
        logger.info(request.getUri().toString());

        int port = request.getUri().getPort();
        if (port == -1 && isSSL) {
            port = 443;
        } else if (port == -1 && !isSSL) {
            port = 80;
        }

        Socket relaysocket;
        if (isSSL) {
            relaysocket = SSLSocketFactory.getDefault().createSocket(request.getUri().getHost(), port);
        } else {
            relaysocket = new Socket(request.getUri().getHost(), port);
        }

        request.writeTo(relaysocket.getOutputStream(), 8192);

        RawHttpResponse response = http.parseResponse(relaysocket.getInputStream());

        if (!socket.isClosed()) {
            response.writeTo(socket.getOutputStream(), 8192);
            socket.close();
        } else {
            logger.warning("client socket closed");
        }

        relaysocket.close();
    }
}
