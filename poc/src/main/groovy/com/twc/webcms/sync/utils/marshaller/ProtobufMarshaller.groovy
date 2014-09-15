package com.twc.webcms.sync.utils.marshaller

import com.twc.webcms.sync.proto.NodeProtos.Node
import com.twc.webcms.sync.proto.NodeProtos.Properties
import com.twc.webcms.sync.proto.NodeProtos.Property
import groovy.transform.CompileStatic

import javax.jcr.Property as JcrProperty
import javax.jcr.Node as JcrNode

@CompileStatic
class ProtobufMarshaller {

    static Node marshall(JcrNode jcrNode) {
        final List<JcrProperty> properties = jcrNode.properties.toList()
        Node.Builder nodeBuilder = Node.newBuilder()
        nodeBuilder.setName(jcrNode.path)

        Properties.Builder propertiesBuilder = Properties.newBuilder()
        properties.each { JcrProperty jcrProperty ->

            Property.Builder propertyBuilder = Property.newBuilder()
            propertyBuilder.setName(jcrProperty.name)

            //TODO: This does NOT yet account for various types of Property Types and Values
            propertyBuilder.setType(jcrProperty.type)
            if(!jcrProperty.multiple) {
                propertyBuilder.setValue(jcrProperty.value.string)
            }
            propertiesBuilder.addProperty(propertyBuilder.build())
        }
        nodeBuilder.setProperties(propertiesBuilder.build())

        nodeBuilder.build()
    }
}
