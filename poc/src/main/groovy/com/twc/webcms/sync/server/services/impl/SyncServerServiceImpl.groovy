package com.twc.webcms.sync.server.services.impl

import com.twc.webcms.sync.jcr.JcrUtil
import com.twc.webcms.sync.proto.NodeProtos.Node as Node
import com.twc.webcms.sync.server.services.SyncServerService
import com.twc.webcms.sync.utils.marshaller.ProtobufMarshaller
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.jackrabbit.commons.flat.TreeTraverser
import org.apache.sling.jcr.api.SlingRepository

import javax.jcr.Node as JcrNode
import javax.jcr.Session
import javax.servlet.ServletOutputStream

@Slf4j
@CompileStatic
@Component(label = "Content Sync Server Service", description = "Content Sync Server Service", immediate = true, metatype = true, enabled = true)
@Service(SyncServerService)
@SuppressWarnings('GroovyUnusedDeclaration')
class SyncServerServiceImpl implements SyncServerService{

    @Reference(bind='setSlingRepository')
    SlingRepository slingRepository

    public void getProtosForRootPath(String rootPath, ServletOutputStream servletOutputStream) {
        JcrUtil.withSession(slingRepository, "admin") { Session session ->
            //Tried to use resourceResolver.getResource() but for some reason, it was failing and returning NPE
            //from time to time
            JcrNode rootNode = session.getNode(rootPath)
            Iterator<JcrNode> nodeIterator = TreeTraverser.nodeIterator(rootNode)
            while(nodeIterator.hasNext()) {
                Node nodeProto = ProtobufMarshaller.marshall(nodeIterator.next())
                nodeProto.writeDelimitedTo(servletOutputStream)
                servletOutputStream.flush()
            }
        }
    }
}
