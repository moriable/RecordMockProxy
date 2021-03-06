package com.moriable.recordmockproxy;

import com.moriable.recordmockproxy.admin.RecordMockProxyAdmin;
import com.moriable.recordmockproxy.model.MockStorage;
import com.moriable.recordmockproxy.model.RecordStorage;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import rawhttp.core.RawHttp;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordMockProxy {
    private ExecutorService execService = Executors.newFixedThreadPool(4);

    private File recordDir;

    private File mockDir;

    private InetSocketAddress serverAddress;

    private RawHttp http = new RawHttp();
    private RecordStorage recordStorage;
    private MockStorage mockStorage;

    private RecordMockProxyAdmin admin;

    private boolean loop = true;

    public RecordMockProxy(InetSocketAddress serverAddress, String caCertPath, String caPrivateKeyPath, int adminPort) throws InvalidKeySpecException, CertificateException, NoSuchAlgorithmException, IOException {

        recordDir = new File("record");
        FileUtils.deleteDirectory(recordDir);
        recordDir.mkdirs();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(recordDir)));

        mockDir = new File("mock");
        if (!mockDir.exists()) {
            mockDir.mkdirs();
        }

        recordStorage = new RecordStorage(Collections.synchronizedMap(new LinkedHashMap<>()));
        mockStorage = new MockStorage(new File(mockDir + File.separator + "mock.json"));

        this.serverAddress = serverAddress;
        RecordMockProxyCA.init(caCertPath, caPrivateKeyPath);
        admin = new RecordMockProxyAdmin(adminPort, new File(caCertPath), recordStorage, recordDir, mockStorage, mockDir);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$s %3$s %5$s%6$s%n");

        Options options = new Options();
        options.addOption(Option.builder("pp")
                .longOpt("proxy-port")
                .hasArg()
                .argName("proxy port number")
                .desc("proxy port number")
                .required()
                .type(Number.class)
                .build());

        options.addOption(Option.builder("ap")
                .longOpt("admin-port")
                .hasArg()
                .argName("admin port number")
                .desc("admin port number")
                .required()
                .type(Number.class)
                .build());

        options.addOption(Option.builder("c")
                .longOpt("cert")
                .hasArg()
                .argName("CA certificate file path")
                .desc("CA certificate file path")
                .required()
                .type(String.class)
                .build());

        options.addOption(Option.builder("k")
                .longOpt("key")
                .hasArg()
                .argName("CA private key file path")
                .desc("CA private key file path")
                .required()
                .type(String.class)
                .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        int proxyPort;
        int adminPort;
        try {
            cmd = parser.parse(options, args);
            proxyPort = ((Long)cmd.getParsedOptionValue("pp")).intValue();
            adminPort = ((Long)cmd.getParsedOptionValue("ap")).intValue();
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("[opts]", options);
            return;
        }

        RecordMockProxy server = new RecordMockProxy(new InetSocketAddress("", proxyPort),
                cmd.getOptionValue("c"),
                cmd.getOptionValue("k"),
                adminPort);
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
        admin.stop();
    }

    protected void submitWorker(RecordMockProxyWorker worker) {
        worker.init(this, http, recordStorage, recordDir, mockStorage, mockDir);
        execService.submit(worker);
    }
}
