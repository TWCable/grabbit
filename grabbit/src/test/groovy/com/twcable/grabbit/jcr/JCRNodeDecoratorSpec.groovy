package com.twcable.grabbit.jcr

import com.day.cq.commons.jcr.JcrConstants
import spock.lang.Specification

import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.RepositoryException
import javax.jcr.nodetype.NodeType

import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@SuppressWarnings("GroovyAssignabilityCheck")
class JCRNodeDecoratorSpec extends Specification {

    def "null nodes are not allowed for JCRNodeDecorator construction"() {
        when:
        new JCRNodeDecorator(null)

        then:
        thrown(IllegalArgumentException)
    }


    def "setLastModified() when last modified can be set"() {
        given:
        Node node = Mock(Node) {
            getPrimaryNodeType() >> Mock(NodeType) {
                canSetProperty(JCR_LASTMODIFIED, _) >> { true }
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)
        nodeDecorator.setLastModified()

        then:
        1 * node.setProperty(JCR_LASTMODIFIED, _)
        notThrown(RepositoryException)
    }


    def "setLastModified() when last modified can not be set"() {
        given:
        Node node = Mock(Node) {
            getPrimaryNodeType() >> Mock(NodeType) {
                canSetProperty(JCR_LASTMODIFIED, _) >> { false }
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)
        nodeDecorator.setLastModified()

        then:
        0 * node.setProperty(JCR_LASTMODIFIED, _)
        notThrown(RepositoryException)
    }


    def "During setLastModified() when something goes wrong with getPrimaryNodeType() we handle this case gracefully"() {
        given:
        Node node = Mock(Node) {
            getPrimaryNodeType() >> { throw new RepositoryException() }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)
        nodeDecorator.setLastModified()

        then:
        notThrown(RepositoryException)
    }


    def "getPrimaryType()"() {
        given:
        Node node = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> { JcrConstants.NT_FILE }
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)

        then:
        nodeDecorator.getPrimaryType() == JcrConstants.NT_FILE
    }


    def "Can adapt the decorator back to the wrapped node"() {
        given:
        final node = Mock(Node)
        final nodeDecorator = new JCRNodeDecorator(node)

        expect:
        (nodeDecorator as Node) == node
    }
}
