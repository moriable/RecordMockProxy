package com.moriable.recordmockproxy.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class MockInputStream extends InputStream {

    public MockInputStream(int status, String statusMessage, Map<String, String> head, FileInputStream body) {

    }

    @Override
    public int read() throws IOException {
        return 0;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return super.read(b);
    }
}
