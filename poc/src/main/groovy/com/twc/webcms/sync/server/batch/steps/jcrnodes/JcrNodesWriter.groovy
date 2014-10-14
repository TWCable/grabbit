package com.twc.webcms.sync.server.batch.steps.jcrnodes

import com.twc.webcms.sync.proto.NodeProtos
import com.twc.webcms.sync.server.batch.ServerBatchJobContext
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter

import javax.annotation.Nonnull
import javax.servlet.ServletOutputStream

/**
 * A Custom ItemWriter that will write the provided Protocol Buffer Nodes to the {@link JcrNodesWriter#theServletOutputStream()}
 * Will flush the {@link JcrNodesWriter#theServletOutputStream()} after writing provided Protocol Buffer Nodes
 * @see ItemWriteListener
 */
@Slf4j
@CompileStatic
@SuppressWarnings('GrMethodMayBeStatic')
class JcrNodesWriter implements ItemWriter<NodeProtos.Node>, ItemWriteListener {

    @Override
    void write(List<? extends NodeProtos.Node> nodeProtos) throws Exception {
        ServletOutputStream servletOutputStream = theServletOutputStream()
        if(servletOutputStream == null) throw new IllegalStateException("servletOutputStream must be set.")

        nodeProtos.each { NodeProtos.Node node ->
            log.debug "Sending NodeProto : ${node}"
            node.writeDelimitedTo(servletOutputStream)
        }
    }

    @Override
    void beforeWrite(List items) {
    }

    @Override
    void afterWrite(List items) {
        theServletOutputStream().flush()
    }

    @Override
    void onWriteError(Exception exception, List items) {
        log.error "Exception occurred while writing the current chunk", exception
    }

    private ServletOutputStream theServletOutputStream() {
        ServerBatchJobContext serverBatchJobContext = ServerBatchJobContext.THREAD_LOCAL.get()
        serverBatchJobContext.servletOutputStream
    }

}
