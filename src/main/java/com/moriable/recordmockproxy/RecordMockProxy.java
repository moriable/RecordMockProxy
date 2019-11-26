package com.moriable.recordmockproxy;

import com.moriable.recordmockproxy.admin.RecordMockProxyAdmin;
import rawhttp.core.RawHttp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordMockProxy {
    private ExecutorService execService = Executors.newFixedThreadPool(4);

    private InetSocketAddress serverAddress;

    private boolean loop = true;

    private RecordMockProxyService service = new RecordMockProxyService();

    private RawHttp http = new RawHttp();

    private RecordMockProxyAdmin admin = new RecordMockProxyAdmin();

    public RecordMockProxy(InetSocketAddress serverAddress, String caCertPath, String caPrivateKeyPath) throws InvalidKeySpecException, CertificateException, NoSuchAlgorithmException, IOException {
        this.serverAddress = serverAddress;
        RecordMockProxyCA.init(caCertPath, caPrivateKeyPath);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$s %3$s %5$s%6$s%n");

        RecordMockProxy server = new RecordMockProxy(new InetSocketAddress("", 8080),
                "/Users/hiroki.nakamori/tmp/proxyca/ca.crt",
                "/Users/hiroki.nakamori/tmp/proxyca/ca.key");
        server.start();
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket()) {
                server.bind(serverAddress);
                while (loop) {
                    Socket socket = server.accept();
                    socket.setSoTimeout(30000);
                    submitWorker(new RecordMockProxyWorker(socket));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        admin.start();
    }

    public void stop() {
        this.loop = false;
    }

    protected void submitWorker(RecordMockProxyWorker worker) {
        worker.init(this, service, http, admin);
        execService.submit(worker);
    }
}
