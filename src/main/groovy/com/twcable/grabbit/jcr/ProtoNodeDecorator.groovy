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
package com.twcable.grabbit.jcr

import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.commons.JcrUtils

import javax.annotation.Nonnull
import javax.jcr.Node as JCRNode
import javax.jcr.Session

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@CompileStatic
@Slf4j
class ProtoNodeDecorator {

    @Delegate
    ProtoNode innerProtoNode

    Collection<ProtoPropertyDecorator> protoProperties


    ProtoNodeDecorator(@Nonnull ProtoNode node) {
        if(!node) throw new IllegalArgumentException("node must not be null!")
        this.innerProtoNode = node
        this.protoProperties = node.propertiesList.collect { new ProtoPropertyDecorator(it) }
    }


    String getPrimaryType() {
        protoProperties.find { it.isPrimaryType() }.value.stringValue
    }


    ProtoPropertyDecorator getMixinProperty() {
        protoProperties.find { it.isMixinType() }
    }


    Collection<ProtoPropertyDecorator> getWritableProperties() {
        protoProperties.findAll { !(it.name in [JCR_PRIMARYTYPE]) }
    }

    List<ProtoNodeDecorator> getMandatoryChildNodes() {
        return mandatoryChildNodeList.collect { new ProtoNodeDecorator(it) }
    }

    JcrNodeDecorator writeToJcr(@Nonnull Session session) {
        return JcrNodeDecorator.createFromProtoNode(this, session)
    }

}
