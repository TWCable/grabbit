package com.twc.webcms.sync.testutils

import javax.servlet.ServletOutputStream

class MockServletOutputStream extends ServletOutputStream {
    protected ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    public void write(int b) throws IOException {
        byteArrayOutputStream.write(b);
    }

    public String toString() {
        try {
            return byteArrayOutputStream.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}