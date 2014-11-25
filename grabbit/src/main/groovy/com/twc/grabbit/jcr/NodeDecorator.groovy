package com.twc.grabbit.jcr

import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.Value
import javax.jcr.ValueFormatException

@CompileStatic
class NodeDecorator {

    @Delegate
    private Node innerNode

    private NodeDecorator(Node node) {
        this.innerNode = node
    }

    static NodeDecorator from(@Nonnull Node node) {
        if(node == null) throw new IllegalArgumentException("Node can not be null!")
        return new NodeDecorator(node)
    }


    void setProperty(PropertyDecorator propertyDecorator) {
        try {
            if(!propertyDecorator.hasValues()) {
                setProperty(propertyDecorator.name, propertyDecorator.getPropertyValue(), propertyDecorator.type)
            }
            else {
                Value[] values = propertyDecorator.getPropertyValues()
                setProperty(propertyDecorator.name, values, propertyDecorator.type)
            }
        }
        catch(ValueFormatException ex) {
            if(ex.message.contains("Multivalued property can not be set to a single value")) {
                //If this is the exception, that means that a property with the name already exists
                final Property currentProperty = getProperty(propertyDecorator.name)
                if(currentProperty.multiple) {
                    final Value[] values = [ propertyDecorator.getPropertyValue() ]
                    setProperty(propertyDecorator.name, values, propertyDecorator.type)
                }
            }
            else if(ex.message.contains("Single-valued property can not be set to an array of values")) {
                setProperty(propertyDecorator.name, propertyDecorator.getPropertyValues().first(), propertyDecorator.type)
            }
        }
    }


}
