package com.twc.webcms.sync.server.batch.steps.jcrnodes

import com.twc.webcms.sync.proto.NodeProtos
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter

import javax.annotation.Nonnull
import javax.servlet.ServletOutputStream

/**
 * A Custom ItemWriter that will write the provided Protocol Buffer Nodes to the {@link JcrNodesWriter#servletOutputStream}
 * Will flush the {@link JcrNodesWriter#servletOutputStream} after writing provided Protocol Buffer Nodes
 * @see ItemWriteListener
 */
@Slf4j
@CompileStatic
class JcrNodesWriter implements ItemWriter<NodeProtos.Node>, ItemWriteListener {

    private ServletOutputStream servletOutputStream

    /**
     * {@link JcrNodesWriter#servletOutputStream} must be set before using JcrNodesWriter using this method
     * @param the servletOutputStream for current execution
     */
    public void setServletOutputStream(@Nonnull ServletOutputStream servletOutputStream) {
        if(servletOutputStream == null) throw new IllegalArgumentException("servletOutputStream == null")
        this.servletOutputStream = servletOutputStream
    }

    @Override
    void write(List<? extends NodeProtos.Node> nodeProtos) throws Exception {
        if(servletOutputStream == null) throw new IllegalStateException("servletOutputStream must be set.")

        nodeProtos.each { NodeProtos.Node node ->
            log.debug "Sending NodeProto : ${node}"
            node.writeDelimitedTo(servletOutputStream)
        }
    }

    @Override
    void beforeWrite(List items) {
        //no-op
    }

    @Override
    void afterWrite(List items) {
        servletOutputStream.flush()
    }

    @Override
    void onWriteError(Exception exception, List items) {
        log.error "Exception occurred while writing the current chunk", exception
    }
}
