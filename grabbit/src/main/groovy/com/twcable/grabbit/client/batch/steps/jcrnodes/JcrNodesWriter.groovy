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
import org.apache.jackrabbit.commons.JcrUtils
import org.apache.jackrabbit.value.BinaryValue
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter
import org.springframework.util.StopWatch

import javax.jcr.Node
import javax.jcr.Session
import javax.jcr.nodetype.NodeType

import static javax.jcr.Node.JCR_CONTENT
import static org.apache.jackrabbit.JcrConstants.JCR_DATA
import static org.apache.jackrabbit.JcrConstants.NT_FILE

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


    @Override
    /*
     *  The JcrNodesReader that funnels proto nodes into here
      * will return null when the stream is finished to indicate completion, but Spring will pass null to this point
     */
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

        //If the primary type is NT_FILE, we have to do some goofiness to make sure a jcr:content node is written with the nt_file node
        if (jcrNode.getPrimaryType() == NT_FILE) {
            //Now we have to add the jcr:content node to enforce the nt:hierarchy requirements
            JCRNodeDecorator jcrContentNode = new JCRNodeDecorator(JcrUtils.getOrAddNode(jcrNode as Node, JCR_CONTENT, NodeType.NT_RESOURCE))
            /*
            * TODO : This is a workaround for the case where a chunk gets 'saved' in JCR and the last node was 'nt:file'
            * If jcr:data is not part of that chunk then you will get a constraint violation exception
            * To get around that, just add an empty binary jcr:data here with a "temp" value
            * This will always be overridden by the actual jcr:data value as that will be the next thing received
            */
            jcrContentNode.setProperty(JCR_DATA, new BinaryValue("temp".bytes))
            jcrContentNode.setLastModified()
        }
        jcrNode.setLastModified()
    }


    private Session theSession() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        clientBatchJobContext.session
    }
}
