package com.twc.webcms.sync.server.batch.steps.jcrnodes

import com.google.protobuf.ByteString
import com.twc.webcms.sync.client.unmarshaller.DateUtil
import com.twc.webcms.sync.proto.NodeProtos
import com.twc.webcms.sync.proto.NodeProtos.Node
import com.twc.webcms.sync.proto.NodeProtos.Properties
import com.twc.webcms.sync.proto.NodeProtos.Property
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemProcessor

import javax.jcr.Node as JcrNode
import javax.jcr.Property as JcrProperty
import javax.jcr.PropertyType
import javax.jcr.Value

import static javax.jcr.PropertyType.*
import static org.apache.jackrabbit.JcrConstants.*

/**
 * A Custom ItemProcessor that effectively acts as a Marshaller for a Jcr Node.
 */
@Slf4j
@CompileStatic
class JcrNodesProcessor implements ItemProcessor<JcrNode, Node> {

    /**
     * Converts a JCR Node to Protocol Buffer Message {@link NodeProtos.Node} object.
     */
    @Override
    Node process(JcrNode jcrNode) throws Exception {

        //TODO: Access Control Lists nodes are not supported right now. WEBCMS-14033
        if(!jcrNode || jcrNode.path.contains("rep:policy")){
            log.info "Ignoring current node ${jcrNode}"
            return null
        }

        final List<JcrProperty> properties = jcrNode.properties.toList()
        Node.Builder nodeBuilder = Node.newBuilder()
        nodeBuilder.setName(jcrNode.path)

        Properties.Builder propertiesBuilder = Properties.newBuilder()
        properties.each { JcrProperty jcrProperty ->
            //Before adding a property to the Current Node Proto message, check if the property
            //is Valid and if it should be actually sent to the client
            if(isPropertyTransferable(jcrProperty)) {
                Property property = toProperty(jcrProperty)
                propertiesBuilder.addProperty(property)
            }
        }
        nodeBuilder.setProperties(propertiesBuilder.build())

        nodeBuilder.build()
    }

    /**
     * Checks if current Jcr Property is valid to be written out to the client or not
     * @param jcrProperty
     * @return
     */
    private static boolean isPropertyTransferable(JcrProperty jcrProperty) {
        //If property is "jcr:lastModified", we don't want to send this property to the client. If we send it, and
        //the client writes it to JCR, then we can have lastModified date for a node that is older than the creation
        //date itself
        if(jcrProperty.name == JCR_LASTMODIFIED) {
            return false
        }

        if([JCR_PRIMARYTYPE, JCR_MIXINTYPES].contains(jcrProperty.name)) {
            return true
        }

        !jcrProperty.definition.isProtected()
    }

    /**
     * Accepts a Jcr Property and marshals it to a NodeProtos.Property message object
     * @param jcrProperty
     * @return
     */
    private static Property toProperty(JcrProperty jcrProperty) {
        Property.Builder propertyBuilder = Property.newBuilder()
        propertyBuilder.setName(jcrProperty.name)

        final int type = jcrProperty.type

        switch(type) {
            case STRING :
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setStringValue(value.string))
                }
                else {
                    Value[] values = jcrProperty.values
                    Collection<NodeProtos.Value> protoValues = values.collect { Value value ->
                        NodeProtos.Value.newBuilder().setStringValue(value.string).build()
                    }
                    propertyBuilder.setValues(
                            NodeProtos.Values.newBuilder().addAllValue(protoValues).build()
                    )
                }
                break
            case BINARY :
                //no multiple values
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setBytesValue(ByteString.copyFrom(value.binary.stream.bytes)))
                }
                break
            case BOOLEAN :
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setStringValue(value.boolean.toString()))
                }
                else {
                    Value[] values = jcrProperty.values
                    Collection<NodeProtos.Value> protoValues = values.collect { Value value ->
                        NodeProtos.Value.newBuilder().setStringValue(value.boolean.toString()).build()
                    }
                    propertyBuilder.setValues(
                            NodeProtos.Values.newBuilder().addAllValue(protoValues).build()
                    )
                }
                break
            case DATE :
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setStringValue(DateUtil.getISOStringFromCalendar(value.date)))
                }
                else {
                    Value[] values = jcrProperty.values
                    Collection<NodeProtos.Value> protoValues = values.collect { Value value ->
                        NodeProtos.Value.newBuilder().setStringValue(DateUtil.getISOStringFromCalendar(value.date)).build()
                    }
                    propertyBuilder.setValues(
                            NodeProtos.Values.newBuilder().addAllValue(protoValues).build()
                    )
                }
                break
            case DECIMAL :
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setStringValue(value.decimal.toString()))
                }
                else {
                    Value[] values = jcrProperty.values
                    Collection<NodeProtos.Value> protoValues = values.collect { Value value ->
                        NodeProtos.Value.newBuilder().setStringValue(value.decimal.toString()).build()
                    }
                    propertyBuilder.setValues(
                            NodeProtos.Values.newBuilder().addAllValue(protoValues).build()
                    )
                }
                break
            case DOUBLE :
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setStringValue(value.double.toString()))
                }
                else {
                    Value[] values = jcrProperty.values
                    Collection<NodeProtos.Value> protoValues = values.collect { Value value ->
                        NodeProtos.Value.newBuilder().setStringValue(value.double.toString()).build()
                    }
                    propertyBuilder.setValues(
                            NodeProtos.Values.newBuilder().addAllValue(protoValues).build()
                    )
                }
                break
            case LONG :
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setStringValue(value.long.toString()))
                }
                else {
                    Value[] values = jcrProperty.values
                    Collection<NodeProtos.Value> protoValues = values.collect { Value value ->
                        NodeProtos.Value.newBuilder().setStringValue(value.long.toString()).build()
                    }
                    propertyBuilder.setValues(
                            NodeProtos.Values.newBuilder().addAllValue(protoValues).build()
                    )
                }
                break
            case NAME :
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setStringValue(value.string))
                }
                else {
                    Value[] values = jcrProperty.values
                    Collection<NodeProtos.Value> protoValues = values.collect { Value value ->
                        NodeProtos.Value.newBuilder().setStringValue(value.string).build()
                    }
                    propertyBuilder.setValues(
                            NodeProtos.Values.newBuilder().addAllValue(protoValues).build()
                    )
                }
                break
            case PATH :
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setStringValue(value.string))
                }
                else {
                    Value[] values = jcrProperty.values
                    Collection<NodeProtos.Value> protoValues = values.collect { Value value ->
                        NodeProtos.Value.newBuilder().setStringValue(value.string).build()
                    }
                    propertyBuilder.setValues(
                            NodeProtos.Values.newBuilder().addAllValue(protoValues).build()
                    )
                }
                break
            case PropertyType.URI :
                if(!jcrProperty.multiple) {
                    Value value = jcrProperty.value
                    propertyBuilder.setValue(NodeProtos.Value.newBuilder().setStringValue(value.string))
                }
                else {
                    Value[] values = jcrProperty.values
                    Collection<NodeProtos.Value> protoValues = values.collect { Value value ->
                        NodeProtos.Value.newBuilder().setStringValue(value.string).build()
                    }
                    propertyBuilder.setValues(
                            NodeProtos.Values.newBuilder().addAllValue(protoValues).build()
                    )
                }
                break
            //TODO: Is it correct to ignore this? (as Reference value would mean different for Server and for Client
            case REFERENCE :
                break
            //TODO: Is it correct to ignore this? (seems similar to REFERENCE to me)
            case WEAKREFERENCE :
                break
        }

        propertyBuilder.setType(jcrProperty.type)
        propertyBuilder.build()
    }
}
