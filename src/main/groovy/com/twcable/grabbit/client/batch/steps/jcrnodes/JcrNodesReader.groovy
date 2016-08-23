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

package com.twcable.grabbit.client.batch.steps.jcrnodes

import com.twcable.grabbit.client.batch.ClientBatchJobContext
import com.twcable.grabbit.proto.NodeProtos
import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException

/**
 * A Custom ItemReader that provides the "next" {@link NodeProtos.Node} from the {@link ClientBatchJobContext#inputStream}.
 * Returns null to indicate that all Items have been read.
 */
@Slf4j
@CompileStatic
@SuppressWarnings("GrMethodMayBeStatic")
class JcrNodesReader implements ItemReader<ProtoNode> {

    @Override
    NodeProtos.Node read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        ProtoNode nodeProto = ProtoNode.parseDelimitedFrom(theInputStream())
        if (!nodeProto) {
            log.info "Received all data from Server"
            return null
        }
        return nodeProto
    }

    private InputStream theInputStream() {
        ClientBatchJobContext.inputStream
    }

}
