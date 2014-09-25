package com.twc.webcms.sync.server.services.impl

import com.twc.webcms.sync.jcr.JcrUtil
import com.twc.webcms.sync.proto.NodeProtos.Node as Node
import com.twc.webcms.sync.proto.PreProcessorProtos.Preprocessors
import com.twc.webcms.sync.server.services.SyncServerService
import com.twc.webcms.sync.server.marshaller.ProtobufMarshaller
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.jackrabbit.commons.flat.TreeTraverser
import org.apache.sling.jcr.api.SlingRepository

import javax.annotation.Nonnull
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

    void getContentForRootPath(@Nonnull String rootPath, @Nonnull ServletOutputStream servletOutputStream) {
        JcrUtil.withSession(slingRepository, "admin") { Session session ->
            //Preprocessor step
            //Send all the namespaces first
            final Preprocessors preprocessors = ProtobufMarshaller.toPreprocessorsProto(session)
            preprocessors.writeDelimitedTo(servletOutputStream)
            servletOutputStream.flush()

            final JcrNode rootNode = session.getNode(rootPath)
            final Iterator<JcrNode> nodeIterator = TreeTraverser.nodeIterator(rootNode)
            while(nodeIterator.hasNext()) {
                JcrNode currentNode = nodeIterator.next()
                //TODO: Access Control Lists nodes are not supported right now. WEBCMS-14033
                if(!(currentNode.path.contains("rep:policy"))) {
                    final Node nodeProto = ProtobufMarshaller.toNodeProto(currentNode)
                    log.debug "Sending proto: ${nodeProto}"
                    nodeProto.writeDelimitedTo(servletOutputStream)
                    servletOutputStream.flush()
                }
            }
        }
    }

}
