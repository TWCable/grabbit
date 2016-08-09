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
package com.twcable.grabbit.resources

import org.apache.sling.api.resource.ResourceResolver
import spock.lang.Specification
import spock.lang.Unroll

import static com.twcable.grabbit.resources.TransactionResource.TRANSACTION_ID_KEY
import static com.twcable.grabbit.resources.TransactionResource.TRANSACTION_RESOURCE_TYPE

class TransactionResourceSpec extends Specification {

    @Unroll
    def "TransactionResource creation works as we expect with valid input path: #path"() {
        given:
        final transactionResource = new TransactionResource(Mock(ResourceResolver), path)

        expect:
        transactionResource.resourceMetadata[TRANSACTION_ID_KEY] == "transactionID"
        transactionResource.getPath() == path
        transactionResource.getResourceType() == TRANSACTION_RESOURCE_TYPE

        where:
        path  << [
            "/grabbit/transaction/transactionID.json",
            "/grabbit/transaction/transactionID.html",
            "/grabbit/transaction/transactionID.doesntmatter",
            "/grabbit/transaction/transactionID"
        ]
    }

    def "TransactionResource handles resource creation gracefully if no transactionID was provided"() {
        given:
        final transactionResource = new TransactionResource(Mock(ResourceResolver), path)

        expect:
        transactionResource.resourceMetadata[TRANSACTION_ID_KEY] == ""
        transactionResource.getPath() == path
        transactionResource.getResourceType() == TRANSACTION_RESOURCE_TYPE

        where:
        path << ["/grabbit/transaction/", "/grabbit/transaction"]
    }

}
