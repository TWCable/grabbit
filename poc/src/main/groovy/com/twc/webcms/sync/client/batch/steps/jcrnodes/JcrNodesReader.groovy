package com.twc.webcms.sync.client.batch.steps.jcrnodes

import com.twc.webcms.sync.client.batch.ClientBatchJobContext
import com.twc.webcms.sync.proto.NodeProtos
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException

/**
 * A Custom ItemReader that provides the "next" {@link NodeProtos.Node} from the {@link ClientBatchJobContext#inputStream}.
 * Returns null to indicate that all Items have been read.
 */
@Slf4j
@CompileStatic
@SuppressWarnings("GrMethodMayBeStatic")
class JcrNodesReader implements ItemReader<NodeProtos.Node> {

    @Override
    NodeProtos.Node read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        NodeProtos.Node nodeProto = NodeProtos.Node.parseDelimitedFrom(theInputStream())
        if(!nodeProto) {
            log.info "Received all data from Server"
            return null
        }
        return nodeProto
    }

    private InputStream theInputStream() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        clientBatchJobContext.inputStream
    }
}
