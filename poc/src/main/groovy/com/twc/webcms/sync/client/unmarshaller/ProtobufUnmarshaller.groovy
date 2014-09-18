package com.twc.webcms.sync.client.unmarshaller

import com.twc.webcms.sync.proto.NodeProtos
import com.twc.webcms.sync.proto.NodeProtos.Node
import com.twc.webcms.sync.proto.NodeProtos.Property
import com.twc.webcms.sync.proto.PreProcessorProtos
import com.twc.webcms.sync.proto.PreProcessorProtos.Preprocessors
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.commons.JcrUtils
import org.apache.jackrabbit.commons.NamespaceHelper
import org.apache.jackrabbit.value.*

import javax.jcr.Node as JcrNode
import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.jcr.Value
import javax.jcr.nodetype.NodeType
import java.text.DecimalFormat

import static javax.jcr.PropertyType.*
import static javax.jcr.PropertyType.URI as JcrURI
import static org.apache.jackrabbit.JcrConstants.*

@Slf4j
@CompileStatic
class ProtobufUnmarshaller {

    private long nodeCount

    public ProtobufUnmarshaller() {
        this.nodeCount = 0;
    }

    long getNodeCount() {
        nodeCount
    }

    /**
     * Un-marshals the NamespaceRegistry (as a key value pair of prefix:uri) and registers the namespaces if they don't
     * already exist
     * @param preprocessorsProto
     * @param session
     */
    static void fromPreprocessorsProto(Preprocessors preprocessorsProto, Session session) {
        try {
            log.info "Received Preprocessors Proto: ${preprocessorsProto}"
            final NamespaceHelper namespaceHelper = new NamespaceHelper(session)
            PreProcessorProtos.NamespaceRegistry namespaceRegistryProto = preprocessorsProto.namespaceRegistry
            namespaceRegistryProto.entryList.each { PreProcessorProtos.NamespaceEntry namespaceEntry ->
                namespaceHelper.registerNamespace(namespaceEntry.prefix, namespaceEntry.uri)
            }
            session.save()
        }
        catch (RepositoryException e) {
            log.error "Exception while unmarshalling Preprocessors: ${preprocessorsProto}", e
        }
    }

    /**
     * Un-marshals the Node proto message and writes the node to JCR
     * @param nodeProto
     * @param session
     */
    void fromNodeProto(Node nodeProto, Session session) {
        log.debug "Received NodeProto: ${nodeProto}"
        List<Property> properties = nodeProto.properties.propertyList
        final String primaryType = properties.find { Property protoProperty -> protoProperty.name == JCR_PRIMARYTYPE }.value.stringValue
        log.debug "Primary Type: ${primaryType}"

        if(primaryType == NT_FILE) {
            def temp = nodeProto.name.split("/")
            final String fileName = temp.last()
            final String parentName = nodeProto.name.replaceAll("/${fileName}", "")
            final JcrNode parentNode = JcrUtils.getOrCreateByPath(parentName, null, session)
            JcrNode fileNode = JcrUtils.getOrAddNode(parentNode, fileName, NodeType.NT_FILE)
            JcrUtils.getOrAddNode(fileNode, JcrNode.JCR_CONTENT, NodeType.NT_RESOURCE)
            nodeCount++
        }
        else {
            JcrNode currentNode = JcrUtils.getOrCreateByPath(nodeProto.name, primaryType, session)
            properties.each { Property protoProperty ->
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
            nodeCount++
        }
    }

    /**
     * If a property can be added as a mixin, adds it to the given node
     * @param property
     * @param node
     */
    private static void addMixins(Property property, JcrNode node) {
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
    private static void addProperty(Property property, JcrNode currentNode) {
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
}
