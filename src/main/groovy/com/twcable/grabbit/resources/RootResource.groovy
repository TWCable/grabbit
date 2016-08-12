/**
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
 *
 * Representation of a Grabbit's Root Resource provided by {@link GrabbitResourceProvider}.
 * Queried from {@link com.twcable.grabbit.client.servlets.GrabbitRootServlet}
 */

package com.twcable.grabbit.resources

import groovy.transform.CompileStatic
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.SyntheticResource

import javax.annotation.Nonnull

@CompileStatic
class RootResource extends SyntheticResource  {

    static final String ROOT_RESOURCE_TYPE = "twcable:grabbit"

    RootResource(@Nonnull final ResourceResolver resourceResolver,@Nonnull final String resolutionPath,
                 String resourceType = ROOT_RESOURCE_TYPE) {
        super(resourceResolver, resolutionPath, resourceType)
    }

    @Override
    String getResourceType() {
        return ROOT_RESOURCE_TYPE
    }
}
