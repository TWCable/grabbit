package com.twc.webcms.sync.server.services

import com.twc.webcms.sync.proto.NodeProtos

import javax.servlet.ServletOutputStream

interface SyncServerService {
    public void getProtosForRootPath(String rootPath, ServletOutputStream servletOutputStream)
}
