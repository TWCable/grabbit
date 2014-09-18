package com.twc.webcms.sync.server.services

import javax.servlet.ServletOutputStream

interface SyncServerService {

    /**
     * Accepts a rootPath, retrieves content and writes it to the {@param servletOutputStream}
     * @param rootPath
     * @param servletOutputStream
     */
    void getContentForRootPath(String rootPath, ServletOutputStream servletOutputStream)
}
