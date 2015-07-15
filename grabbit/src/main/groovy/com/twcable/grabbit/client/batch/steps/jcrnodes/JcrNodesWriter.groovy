/*
 * Copyright 2015 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twcable.grabbit.client.batch.steps.jcrnodes

import com.twcable.grabbit.client.batch.ClientBatchJobContext
import com.twcable.grabbit.jcr.JCRNodeDecorator
import com.twcable.grabbit.jcr.ProtoNodeDecorator
import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter
import org.springframework.util.StopWatch

import javax.jcr.Session

/**
 * A Custom ItemWriter that will write the provided Jcr Nodes to the {@link JcrNodesWriter#theSession()}
 * Will save() the {@link JcrNodesWriter#theSession()} after writing provided Jcr Nodes
 * @see ItemWriteListener
 */
@Slf4j
@CompileStatic
@SuppressWarnings('GrMethodMayBeStatic')
class JcrNodesWriter implements ItemWriter<ProtoNode>, ItemWriteListener {

    @Override
    void beforeWrite(List nodeProtos) {
        //no-op
    }


    @Override
    void afterWrite(List nodeProtos) {
        log.info "Saving ${nodeProtos.size()} nodes"
        theSession().save()
        withStopWatch("Refreshing session: ${theSession()}") {
            theSession().refresh(false)
        }
    }


    @Override
    void onWriteError(Exception exception, List nodeProtos) {
        log.error "Exception writing JCR Nodes to current JCR Session : ${theSession()}. ", exception
    }


    /**
     *  The JcrNodesReader that funnels proto nodes into here
     * will return null when the stream is finished to indicate completion, but Spring will pass null to this point
     */
    @Override
    void write(List<? extends ProtoNode> nodeProtos) throws Exception {
        Session session = theSession()
        for (ProtoNode nodeProto : nodeProtos) {
            writeToJcr(nodeProto, session)
        }
    }

    private static <T> T withStopWatch(String stopWatchId, Closure<T> cl) {
        StopWatch stopWatch = new StopWatch(stopWatchId)
        stopWatch.start()

        T retVal = cl.call()

        stopWatch.stop()
        log.info stopWatch.shortSummary()

        return retVal
    }

    private static void writeToJcr(ProtoNode nodeProto, Session session) {
        JCRNodeDecorator jcrNode = new ProtoNodeDecorator(nodeProto).writeToJcr(session)
        jcrNode.setLastModified()
        // This will processed all mandatory child nodes only
        if(nodeProto.mandatoryChildNodeList && nodeProto.mandatoryChildNodeList.size() > 0) {
            for(ProtoNode childNode: nodeProto.mandatoryChildNodeList) {
                writeToJcr(childNode, session)
            }
        }
    }

    private Session theSession() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        clientBatchJobContext.session
    }
}
