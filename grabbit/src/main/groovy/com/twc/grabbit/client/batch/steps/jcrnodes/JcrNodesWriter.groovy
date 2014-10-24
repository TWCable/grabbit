package com.twc.grabbit.client.batch.steps.jcrnodes

import com.twc.grabbit.client.batch.ClientBatchJobContext
import com.twc.grabbit.client.unmarshaller.DateUtil
import com.twc.grabbit.proto.NodeProtos
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.commons.JcrUtils
import org.apache.jackrabbit.value.*
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter

import javax.jcr.Session
import javax.jcr.Value
import javax.jcr.nodetype.NodeType
import java.text.DecimalFormat

import static javax.jcr.PropertyType.*
import static javax.jcr.PropertyType.URI as JcrURI
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

    private static void writeToJcr(NodeProtos.Node nodeProto, Session session) {
        log.debug "Received NodeProto: ${nodeProto}"
        List<NodeProtos.Property> properties = nodeProto.properties.propertyList
        final String primaryType = properties.find { NodeProtos.Property protoProperty -> protoProperty.name == JCR_PRIMARYTYPE }.value.stringValue
        log.debug "Primary Type: ${primaryType}"

        if(primaryType == NT_FILE) {
            def temp = nodeProto.name.split("/")
            final String fileName = temp.last()
            final String parentName = nodeProto.name.replaceAll("/${fileName}", "")
            final javax.jcr.Node parentNode = JcrUtils.getOrCreateByPath(parentName, null, session)
            javax.jcr.Node fileNode = JcrUtils.getOrAddNode(parentNode, fileName, NodeType.NT_FILE)
            JcrUtils.getOrAddNode(fileNode, javax.jcr.Node.JCR_CONTENT, NodeType.NT_RESOURCE)
        }
        else {
            javax.jcr.Node currentNode = JcrUtils.getOrCreateByPath(nodeProto.name, primaryType, session)
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
        }
    }

    /**
     * If a property can be added as a mixin, adds it to the given node
     * @param property
     * @param node
     */
    private static void addMixins(NodeProtos.Property property, javax.jcr.Node node) {
        property.values.valueList.each { NodeProtos.Value value ->
            if(node.canAddMixin(value.stringValue)) {
                node.addMixin(value.stringValue)
            }
            else {
                log.info "Encountered invalid mixin type while unmarshalling for Proto values : ${property.values}"
            }
        }
    }

    /**
     * Accepts a Node Proto property and writes it to the current JCR Node
     * @param property
     * @param currentNode
     */
    private static void addProperty(NodeProtos.Property property, javax.jcr.Node currentNode) {
        final int type = property.type

        switch(type) {
            case STRING :
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, new StringValue(property.value.stringValue), STRING)
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new StringValue(value.stringValue) }
                    currentNode.setProperty(property.name, values, STRING)
                }
                break
            case BINARY :
                //no multiple values
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, new BinaryValue(property.value.bytesValue.toByteArray()), BINARY)
                }
                break
            case BOOLEAN :
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, new BooleanValue(property.value.stringValue.asBoolean()), BOOLEAN)
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new BooleanValue(value.stringValue.asBoolean()) }
                    currentNode.setProperty(property.name, values, BOOLEAN)
                }
                break
            case DATE :
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, new DateValue(DateUtil.getCalendarFromISOString(property.value.stringValue)), DATE)
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new DateValue(DateUtil.getCalendarFromISOString(property.value.stringValue)) }
                    currentNode.setProperty(property.name, values, DATE)
                }
                break
            case DECIMAL :
                DecimalFormat decimalFormat = new DecimalFormat()
                decimalFormat.setParseBigDecimal(true)
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, new DecimalValue(decimalFormat.parse(property.value.stringValue) as BigDecimal), DECIMAL)
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new DecimalValue(decimalFormat.parse(value.stringValue) as BigDecimal) }
                    currentNode.setProperty(property.name, values, DECIMAL)
                }
                break
            case DOUBLE :
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, new DoubleValue(Double.parseDouble(property.value.stringValue)), DOUBLE)
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new DoubleValue(Double.parseDouble(value.stringValue)) }
                    currentNode.setProperty(property.name, values, DOUBLE)
                }
                break
            case LONG :
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, new LongValue(Long.parseLong(property.value.stringValue)), LONG)
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new LongValue(Long.parseLong(value.stringValue)) }
                    currentNode.setProperty(property.name, values, LONG)
                }
                break
            case NAME :
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, NameValue.valueOf(property.value.stringValue), NAME)
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> NameValue.valueOf(value.stringValue) }
                    currentNode.setProperty(property.name, values, NAME)
                }
                break
            case PATH :
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, PathValue.valueOf(property.value.stringValue), PATH)
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> PathValue.valueOf(value.stringValue) }
                    currentNode.setProperty(property.name, values, PATH)
                }
                break
            case JcrURI :
                if(!property.hasValues()) {
                    currentNode.setProperty(property.name, URIValue.valueOf(property.value.stringValue), JcrURI)
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> URIValue.valueOf(value.stringValue) }
                    currentNode.setProperty(property.name, values, PATH)
                }
                break
                //TODO: Support WEAKREFERENCE
            case REFERENCE :
                //TODO: Should this be ignored ?
                break
            case WEAKREFERENCE :
                break

        }
    }

    private Session theSession() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        clientBatchJobContext.session
    }
}
