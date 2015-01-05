package com.twc.grabbit.resources

import org.apache.sling.api.resource.ResourceResolver
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Subject(GrabbitResourceProvider)
class GrabbitResourceProviderSpec extends Specification {

    @Unroll
    def "Resourceprovider returns #resourceType for #path"() {
        given:
        def provider = new GrabbitResourceProvider()

        when:
        final resource = provider.getResource(Mock(ResourceResolver), path)

        then:
        resource.path == path
        resource.resourceType == resourceType
        resource.resourceMetadata[JobResource.JOB_EXECUTION_ID] == jobExecutionId

        where:
        path                            | resourceType                                          | jobExecutionId
        '/grabbit/job'                  | GrabbitResourceProvider.GRABBIT_JOB_RESOURCE_TYPE     | null
        '/grabbit/job/all.json'         | GrabbitResourceProvider.GRABBIT_JOB_RESOURCE_TYPE     | "all"
        '/grabbit/job/1.json'           | GrabbitResourceProvider.GRABBIT_JOB_RESOURCE_TYPE     | "1"
        '/grabbit/job/1.html'           | GrabbitResourceProvider.GRABBIT_JOB_RESOURCE_TYPE     | "1"
    }
}
