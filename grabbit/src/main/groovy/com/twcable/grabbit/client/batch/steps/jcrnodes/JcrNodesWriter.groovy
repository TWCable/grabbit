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

import com.twcable.grabbit.jcr.NodeDecorator
import com.twcable.grabbit.jcr.PropertyDecorator
import com.twcable.grabbit.client.batch.ClientBatchJobContext
import com.twcable.grabbit.proto.NodeProtos
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.commons.JcrUtils
import org.apache.jackrabbit.value.BinaryValue
import org.apache.jackrabbit.value.DateValue
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter
import org.springframework.util.StopWatch

import javax.jcr.Node as JcrNode
import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.jcr.nodetype.NodeType

import static javax.jcr.Node.JCR_CONTENT
import static org.apache.jackrabbit.JcrConstants.JCR_DATA
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import static org.apache.jackrabbit.JcrConstants.NT_FILE

/**
 * A Custom ItemWriter that will write the provided Jcr Nodes to the {@link JcrNodesWriter#theSession()}
 * Will save() the {@link JcrNodesWriter#theSession()} after writing provided Jcr Nodes
 * @see ItemWriteListener
 */
@Slf4j
@CompileStatic
@SuppressWarnings('GrMethodMayBeStatic')
class JcrNodesWriter implements ItemWriter<NodeProtos.Node>, ItemWriteListener {

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
    void write(List<? extends NodeProtos.Node> nodeProtos) throws Exception {
        Session session = theSession()
        for (NodeProtos.Node nodeProto : nodeProtos) {
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


    private static void writeToJcr(NodeProtos.Node nodeProto, Session session) {
        log.debug "Received NodeProto: ${nodeProto}"
        List<NodeProtos.Property> properties = nodeProto.properties.propertyList
        final String primaryType = properties.find { NodeProtos.Property protoProperty -> protoProperty.name == JCR_PRIMARYTYPE }.value.stringValue
        log.debug "Primary Type: ${primaryType}"

        if (primaryType == NT_FILE) {
            def currentNameArray = nodeProto.name.split("/")
            //currentNameArray[-1] finds the member of the last index
            final String currentFileName = currentNameArray[-1]
            //The path leading up to the second to last index.  e.g /content/foo/bar of /content/foo/bar/file
            final String immediateParentName = currentNameArray[0..-2].join("/")
            final JcrNode parentNode = JcrUtils.getOrCreateByPath(immediateParentName, null, session)
            JcrNode fileNode = JcrUtils.getOrAddNode(parentNode, currentFileName, NodeType.NT_FILE)
            JcrNode theNode = JcrUtils.getOrAddNode(fileNode, JCR_CONTENT, NodeType.NT_RESOURCE)

            //TODO : This is a workaround for the case where a chunk gets 'saved' in JCR and the last node was 'nt:file'
            // If jcr:data is not part of that chunk then you will get a constraint violation exception
            // To get around that, just add an empty binary jcr:data here with a "temp" value
            // This will always be overridden by the actual jcr:data value as that will be the next thing received
            theNode.setProperty(JCR_DATA, new BinaryValue("temp".bytes))
            addLastModifiedProperty(theNode)
        }
        else {
            JcrNode currentNode = JcrUtils.getOrCreateByPath(nodeProto.name, primaryType, session)
            final NodeProtos.Property mixinProperty = properties.find {NodeProtos.Property property -> property.name == JCR_MIXINTYPES }
            if(mixinProperty) {
                addMixins(mixinProperty, currentNode)
            }
            properties.each { NodeProtos.Property protoProperty ->
                if( !(protoProperty.name in [JCR_PRIMARYTYPE, JCR_MIXINTYPES]) && (protoProperty.hasValue() || protoProperty.hasValues())) {
                    final String propertyPrefix = protoProperty.name.split(":")[0]
                    log.debug "Current node ${protoProperty.name} prefix : ${propertyPrefix}"
                    addProperty(protoProperty, currentNode)
                }
            }
            addLastModifiedProperty(currentNode)
        }
    }

    /**
     * If a property can be added as a mixin, adds it to the given node
     * @param property
     * @param node
     */
    private static void addMixins(NodeProtos.Property property, JcrNode node) {
        property.values.valueList.each { NodeProtos.Value value ->
            if (node.canAddMixin(value.stringValue)) {
                node.addMixin(value.stringValue)
                log.info "Added mixin ${value.stringValue} for : ${node.name}."
            }
            else {
                log.warn "Encountered invalid mixin type while unmarshalling for Proto value : ${value}"
            }
        }
    }

    /**
     * Accepts a Node Proto property and writes it to the current JCR Node
     * @param property
     * @param currentNode
     */
    private static void addProperty(NodeProtos.Property property, JcrNode currentNode) {
        PropertyDecorator propertyDecorator = PropertyDecorator.from(property)
        NodeDecorator nodeDecorator = NodeDecorator.from(currentNode)
        nodeDecorator.setProperty(propertyDecorator)
    }

    /**
     * Checks if the given LastModified property can be added to the current node
     * If so, adds it to the given "currentNode"
     */
    private static void addLastModifiedProperty(JcrNode currentNode) {
        final lastModified = new DateValue(Calendar.instance)
        try {
            //Need to check if jcr:lastModified can be added to the current node via its NodeType definition
            //as it cannot be added to all the nodes
            if (currentNode.primaryNodeType.canSetProperty(JCR_LASTMODIFIED, lastModified)) {
                currentNode.setProperty(JCR_LASTMODIFIED, lastModified)
            }
        }
        catch (RepositoryException ex) {
            log.error "Exception while setting jcr:lastModified on ${currentNode.path}.", ex
        }
    }


    private Session theSession() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        clientBatchJobContext.session
    }
}
