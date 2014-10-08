package com.twc.webcms.sync.server.services

import javax.servlet.ServletOutputStream

interface ServerService {

    /**
     * Accepts a rootPath, retrieves content and writes it to the {@param servletOutputStream}
     * @param path
     * @param servletOutputStream
     */
    void getContentForRootPath(String path, ServletOutputStream servletOutputStream)
}
