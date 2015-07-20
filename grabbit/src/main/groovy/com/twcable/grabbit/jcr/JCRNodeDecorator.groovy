package com.twcable.grabbit.jcr

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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.value.DateValue

import javax.annotation.Nonnull
import javax.jcr.Node as JCRNode
import javax.jcr.RepositoryException

import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@CompileStatic
@Slf4j
class JCRNodeDecorator {

    @Delegate
    JCRNode innerNode


    JCRNodeDecorator(@Nonnull JCRNode node) {
        if(!node) throw new IllegalArgumentException("node must not be null!")
        this.innerNode = node
    }


    void setLastModified() {
        final lastModified = new DateValue(Calendar.instance)
        try {
            //Need to check if jcr:lastModified can be added to the current node via its NodeType definition
            //as it cannot be added to all the nodes
            if (innerNode.primaryNodeType.canSetProperty(JCR_LASTMODIFIED, lastModified)) {
                innerNode.setProperty(JCR_LASTMODIFIED, lastModified)
            }
        }
        catch (RepositoryException ex) {
            log.error "Exception while setting jcr:lastModified on ${innerNode.path}.", ex
        }
    }


    String getPrimaryType() {
        innerNode.getProperty(JCR_PRIMARYTYPE).string
    }


    Object asType(Class clazz) {
        if(clazz == JCRNode) {
            return innerNode
        }
        else {
            super.asType(clazz)
        }
    }
}
