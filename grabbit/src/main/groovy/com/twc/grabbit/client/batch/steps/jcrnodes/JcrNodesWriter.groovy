package com.twc.grabbit.client.batch.steps.jcrnodes

import com.twc.grabbit.client.batch.ClientBatchJobContext
import com.twc.grabbit.jcr.NodeDecorator
import com.twc.grabbit.jcr.PropertyDecorator
import com.twc.grabbit.proto.NodeProtos
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
import static org.apache.jackrabbit.JcrConstants.*

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
        for(NodeProtos.Node nodeProto : nodeProtos) {
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

        if(primaryType == NT_FILE) {
            def temp = nodeProto.name.split("/")
            final String fileName = temp.last()
            final String parentName = nodeProto.name.replaceAll("/${fileName}", "")
            final JcrNode parentNode = JcrUtils.getOrCreateByPath(parentName, null, session)
            JcrNode fileNode = JcrUtils.getOrAddNode(parentNode, fileName, NodeType.NT_FILE)
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
            properties.each { NodeProtos.Property protoProperty ->
                if(  JCR_PRIMARYTYPE != protoProperty.name && (protoProperty.hasValue() || protoProperty.hasValues())) {
                    if(protoProperty.name == JCR_MIXINTYPES) {
                        addMixins(protoProperty, currentNode)
                    } else {
                        final String propertyPrefix = protoProperty.name.split(":")[0]
                        log.debug "Current node ${protoProperty.name} prefix : ${propertyPrefix}"
                        addProperty(protoProperty, currentNode)
                    }
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
            if(node.canAddMixin(value.stringValue)) {
                node.addMixin(value.stringValue)
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
            if(currentNode.primaryNodeType.canSetProperty(JCR_LASTMODIFIED, lastModified)) {
                currentNode.setProperty(JCR_LASTMODIFIED, lastModified)
            }
        } catch(RepositoryException ex) {
            log.error "Exception while setting jcr:lastModified on ${currentNode.path}.", ex
        }
    }


    private Session theSession() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        clientBatchJobContext.session
    }
}
