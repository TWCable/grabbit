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
import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.jcr.Session

@CompileStatic
abstract class ProtoNodeDecorator {

    @Delegate
    protected ProtoNode innerProtoNode

    protected Collection<ProtoPropertyDecorator> protoProperties

    protected String nameOverride

    abstract JCRNodeDecorator writeToJcr(@Nonnull Session session)

    static ProtoNodeDecorator createFrom(@Nonnull ProtoNode node, String nameOverride = null) {
        if(!node) throw new IllegalArgumentException("node must not be null!")
        final protoProperties = node.propertiesList.collect { new ProtoPropertyDecorator(it) }
        final primaryType = protoProperties.find { it.primaryType }
        if(primaryType.isUserType() || primaryType.isGroupType()) {
            return new AuthorizableProtoNodeDecorator(node, protoProperties)
        }
        else if(primaryType.isRepAclType()) {
            return new ACLProtoNodeDecorator(node, protoProperties, nameOverride)
        }
        return new DefaultProtoNodeDecorator(node, protoProperties, nameOverride)
    }

    boolean hasProperty(String propertyName) {
        propertiesList.any{ it.name == propertyName }
    }


    protected ProtoPropertyDecorator getPrimaryType() {
        protoProperties.find { it.isPrimaryType() }
    }


    protected String getStringValueFrom(String propertyName) {
        protoProperties.find { it.name == propertyName }.stringValue
    }

    protected String getParentPath() {
        final pathTokens = getName().tokenize('/')
        //remove last index, as this is the Authorizable node name
        pathTokens.remove(pathTokens.size() - 1)
        return "/${pathTokens.join('/')}"
    }

    @Override
    String getName() {
        nameOverride ?: innerProtoNode.getName()
    }
}
