package com.twc.grabbit.client.batch.steps.jcrnodes

import com.twc.grabbit.client.batch.ClientBatchJobContext
import com.twc.grabbit.DateUtil
import com.twc.grabbit.proto.NodeProtos
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.commons.JcrUtils
import org.apache.jackrabbit.value.*
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter

import javax.jcr.Node as JcrNode
import javax.jcr.Property
import javax.jcr.Session
import javax.jcr.Value
import javax.jcr.ValueFormatException
import javax.jcr.nodetype.NodeType
import java.text.DecimalFormat

import static javax.jcr.Node.JCR_CONTENT
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
            final JcrNode parentNode = JcrUtils.getOrCreateByPath(parentName, null, session)
            JcrNode fileNode = JcrUtils.getOrAddNode(parentNode, fileName, NodeType.NT_FILE)
            JcrNode theNode = JcrUtils.getOrAddNode(fileNode, JCR_CONTENT, NodeType.NT_RESOURCE)

            //TODO : This is a workaround for the case where a chunk gets 'saved' in JCR and the last node was 'nt:file'
            // If jcr:data is not part of that chunk then you will get a constraint violation exception
            // To get around that, just add an empty binary jcr:data here with a "temp" value
            // This will always be overridden by the actual jcr:data value as that will be the next thing received
            theNode.setProperty(JCR_DATA, new BinaryValue("temp".bytes))
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
                log.info "Encountered invalid mixin type while unmarshalling for Proto values : ${property.values}"
            }
        }
    }

    /**
     * Accepts a Node Proto property and writes it to the current JCR Node
     * @param property
     * @param currentNode
     */
    //TODO : This method desperately needs a refactor http://jira.corp.mystrotv.com/browse/WEBCMS-14014
    private static void addProperty(NodeProtos.Property property, JcrNode currentNode) {
        final int type = property.type

        switch(type) {
            case STRING :
                if(!property.hasValues()) {
                    try {
                        currentNode.setProperty(property.name, new StringValue(property.value.stringValue), STRING)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Multivalued property can not be set to a single value")) {
                            //If this is the exception, that means that a property with the name already exists
                            final Property currentProperty = currentNode.getProperty(property.name)
                            if(currentProperty.multiple) {
                                //This is an edge case where the incoming property is single valued but the property
                                //already on JCR is a multi-valued property
                                final Value[] values = [ new StringValue(property.value.stringValue) ]
                                currentNode.setProperty(property.name, values, STRING)
                            }
                        }
                    }
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new StringValue(value.stringValue) }
                    try {
                        currentNode.setProperty(property.name, values, STRING)
                    }
                    catch (ValueFormatException e) {
                        if(e.message.contains("Single-valued property can not be set to an array of values")) {
                            currentNode.setProperty(property.name, values.first(), STRING)
                        }
                    }
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
                    try {
                        currentNode.setProperty(property.name, new BooleanValue(property.value.stringValue.asBoolean()), BOOLEAN)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Multivalued property can not be set to a single value")) {
                            //If this is the exception, that means that a property with the name already exists
                            final Property currentProperty = currentNode.getProperty(property.name)
                            if(currentProperty.multiple) {
                                //This is an edge case where the incoming property is single valued but the property
                                //already on JCR is a multi-valued property
                                final Value[] values = [ new BooleanValue(property.value.stringValue.asBoolean()) ]
                                currentNode.setProperty(property.name, values, BOOLEAN)
                            }
                        }
                    }
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new BooleanValue(value.stringValue.asBoolean()) }
                    try {
                        currentNode.setProperty(property.name, values, BOOLEAN)
                    }
                    catch (ValueFormatException e) {
                        if(e.message.contains("Single-valued property can not be set to an array of values")) {
                            currentNode.setProperty(property.name, values.first(), BOOLEAN)
                        }
                    }
                }
                break
            case DATE :
                if(!property.hasValues()) {
                    try {
                        currentNode.setProperty(property.name, new DateValue(DateUtil.getCalendarFromISOString(property.value.stringValue)), DATE)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Multivalued property can not be set to a single value")) {
                            //If this is the exception, that means that a property with the name already exists
                            final Property currentProperty = currentNode.getProperty(property.name)
                            if(currentProperty.multiple) {
                                //This is an edge case where the incoming property is single valued but the property
                                //already on JCR is a multi-valued property
                                final Value[] values = [ new DateValue(DateUtil.getCalendarFromISOString(property.value.stringValue)) ]
                                currentNode.setProperty(property.name, values, DATE)
                            }
                        }
                    }
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new DateValue(DateUtil.getCalendarFromISOString(property.value.stringValue)) }
                    try {
                        currentNode.setProperty(property.name, values, DATE)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Single-valued property can not be set to an array of values")) {
                            currentNode.setProperty(property.name, values.first(), DATE)
                        }
                    }
                }
                break
            case DECIMAL :
                DecimalFormat decimalFormat = new DecimalFormat()
                decimalFormat.setParseBigDecimal(true)
                if(!property.hasValues()) {
                    try {
                        currentNode.setProperty(property.name, new DecimalValue(decimalFormat.parse(property.value.stringValue) as BigDecimal), DECIMAL)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Multivalued property can not be set to a single value")) {
                            //If this is the exception, that means that a property with the name already exists
                            final Property currentProperty = currentNode.getProperty(property.name)
                            if(currentProperty.multiple) {
                                //This is an edge case where the incoming property is single valued but the property
                                //already on JCR is a multi-valued property
                                final Value[] values = [ new DecimalValue(decimalFormat.parse(property.value.stringValue) as BigDecimal) ]
                                currentNode.setProperty(property.name, values, DECIMAL)
                            }
                        }
                    }
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new DecimalValue(decimalFormat.parse(value.stringValue) as BigDecimal) }
                    try {
                        currentNode.setProperty(property.name, values, DECIMAL)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Single-valued property can not be set to an array of values")) {
                            currentNode.setProperty(property.name, values.first(), DECIMAL)
                        }
                    }
                }
                break
            case DOUBLE :
                if(!property.hasValues()) {
                    try {
                        currentNode.setProperty(property.name, new DoubleValue(Double.parseDouble(property.value.stringValue)), DOUBLE)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Multivalued property can not be set to a single value")) {
                            //If this is the exception, that means that a property with the name already exists
                            final Property currentProperty = currentNode.getProperty(property.name)
                            if(currentProperty.multiple) {
                                //This is an edge case where the incoming property is single valued but the property
                                //already on JCR is a multi-valued property
                                final Value[] values = [ new DoubleValue(Double.parseDouble(property.value.stringValue)) ]
                                currentNode.setProperty(property.name, values, DOUBLE)
                            }
                        }
                    }
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new DoubleValue(Double.parseDouble(value.stringValue)) }
                    currentNode.setProperty(property.name, values, DOUBLE)
                    try {
                        currentNode.setProperty(property.name, values, DOUBLE)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Single-valued property can not be set to an array of values")) {
                            currentNode.setProperty(property.name, values.first(), DOUBLE)
                        }
                    }
                }
                break
            case LONG :
                if(!property.hasValues()) {
                    try {
                        currentNode.setProperty(property.name, new LongValue(Long.parseLong(property.value.stringValue)), LONG)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Multivalued property can not be set to a single value")) {
                            //If this is the exception, that means that a property with the name already exists
                            final Property currentProperty = currentNode.getProperty(property.name)
                            if(currentProperty.multiple) {
                                //This is an edge case where the incoming property is single valued but the property
                                //already on JCR is a multi-valued property
                                final Value[] values = [ new LongValue(Long.parseLong(property.value.stringValue)) ]
                                currentNode.setProperty(property.name, values, LONG)
                            }
                        }
                    }

                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> new LongValue(Long.parseLong(value.stringValue)) }
                    try {
                        currentNode.setProperty(property.name, values, LONG)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Single-valued property can not be set to an array of values")) {
                            currentNode.setProperty(property.name, values.first(), LONG)
                        }
                    }
                }
                break
            case NAME :
                if(!property.hasValues()) {
                    try {
                        currentNode.setProperty(property.name, NameValue.valueOf(property.value.stringValue), NAME)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Multivalued property can not be set to a single value")) {
                            //If this is the exception, that means that a property with the name already exists
                            final Property currentProperty = currentNode.getProperty(property.name)
                            if(currentProperty.multiple) {
                                //This is an edge case where the incoming property is single valued but the property
                                //already on JCR is a multi-valued property
                                final Value[] values = [ NameValue.valueOf(property.value.stringValue) ]
                                currentNode.setProperty(property.name, values, NAME)
                            }
                        }
                    }
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> NameValue.valueOf(value.stringValue) }
                    try {
                        currentNode.setProperty(property.name, values, NAME)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Single-valued property can not be set to an array of values")) {
                            currentNode.setProperty(property.name, values.first(), NAME)
                        }
                    }
                }
                break
            case PATH :
                if(!property.hasValues()) {
                    try {
                        currentNode.setProperty(property.name, PathValue.valueOf(property.value.stringValue), PATH)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Multivalued property can not be set to a single value")) {
                            //If this is the exception, that means that a property with the name already exists
                            final Property currentProperty = currentNode.getProperty(property.name)
                            if(currentProperty.multiple) {
                                //This is an edge case where the incoming property is single valued but the property
                                //already on JCR is a multi-valued property
                                final Value[] values = [ PathValue.valueOf(property.value.stringValue) ]
                                currentNode.setProperty(property.name, values, PATH)
                            }
                        }
                    }
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> PathValue.valueOf(value.stringValue) }
                    try {
                        currentNode.setProperty(property.name, values, PATH)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Single-valued property can not be set to an array of values")) {
                            currentNode.setProperty(property.name, values.first(), PATH)
                        }
                    }
                }
                break
            case JcrURI :
                if(!property.hasValues()) {
                    try {
                        currentNode.setProperty(property.name, URIValue.valueOf(property.value.stringValue), JcrURI)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Multivalued property can not be set to a single value")) {
                            //If this is the exception, that means that a property with the name already exists
                            final Property currentProperty = currentNode.getProperty(property.name)
                            if(currentProperty.multiple) {
                                //This is an edge case where the incoming property is single valued but the property
                                //already on JCR is a multi-valued property
                                final Value[] values = [ URIValue.valueOf(property.value.stringValue) ]
                                currentNode.setProperty(property.name, values, JcrURI)
                            }
                        }
                    }
                }
                else {
                    Value[] values = property.values.valueList.collect{ NodeProtos.Value value -> URIValue.valueOf(value.stringValue) }
                    try {
                        currentNode.setProperty(property.name, values, JcrURI)
                    }
                    catch(ValueFormatException e) {
                        if(e.message.contains("Single-valued property can not be set to an array of values")) {
                            currentNode.setProperty(property.name, values.first(), JcrURI)
                        }
                    }
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
