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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceProvider
import org.apache.sling.api.resource.ResourceResolver
import org.osgi.service.component.ComponentContext

import javax.servlet.http.HttpServletRequest

/**
 * A custom resource provider that provides various Grabbit Sling resources.
 */
@Slf4j
@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
@Service(ResourceProvider)
@Component(label = 'Grabbit Resource Provider', description = 'Grabbit Resource Provider', metatype = true)
@Property(name = "service.vendor", value = "Time Warner Cable")
class GrabbitResourceProvider implements ResourceProvider {

    @Property(label = "Provider Roots", description = "Path roots of what this handles", value = ['/grabbit'])
    public static final String PROVIDER_ROOTS_KEY = 'provider.roots'
    private List<String> providerRoots


    @Activate
    void activate(ComponentContext context) {
        def roots = context.properties.get(PROVIDER_ROOTS_KEY)
        providerRoots = ((roots instanceof String) ? [roots] : roots) as List<String>
    }

    /**
     * @param resourceResolver The resource resolver to resolve this request. Passed in via Sling.
     * @param httpServletRequest The request for a resource. Passed in via Sling.
     * @param path The path for this request. This path will be interrogated in order to provide the correct resource.
     * @return The appropriate Grabbit {@link Resource}, or null if the request doesn't match any known Grabbit resource.
     */
    @Override
    Resource getResource(ResourceResolver resourceResolver, HttpServletRequest httpServletRequest, String path) {
        getResource(resourceResolver, path)
    }

    /**
     * @param resourceResolver The resource resolver to resolve this request. Passed in via Sling.
     * @param path The path for this request. This path will be interrogated in order to provide the correct resource.
     * @return The appropriate Grabbit {@link Resource}, or null if the request doesn't match any known Grabbit resource.
     */
    @Override
    Resource getResource(ResourceResolver resolver, String path) {
        switch (path) {
            case ~/^\/grabbit\/job(\/.*)?$/:
                log.debug "Resolving ${path} to JobResource"
                return new JobResource(resolver, path)
            case ~/^\/grabbit\/transaction(\/.*)?$/:
                log.debug "Resolving ${path} to TransactionResource"
                return new TransactionResource(resolver, path)
            case ~/^\/grabbit\/content(\/)?$/:
                log.debug "Resolving ${path} to ContentResource"
                return new ContentResource(resolver, path)
            default:
                //Should provide a root resource for /grabbit, along with HATEOS style for this link, and other links. https://github.com/TWCable/grabbit/issues/22
                return null
        }
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    Iterator<Resource> listChildren(Resource resource) {
        throw new UnsupportedOperationException()
    }
}
