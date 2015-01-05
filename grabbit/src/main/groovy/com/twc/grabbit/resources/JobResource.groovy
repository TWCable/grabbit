package com.twc.grabbit.resources

import groovy.transform.CompileStatic
import org.apache.sling.api.resource.ResourceMetadata
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.SyntheticResource

/**
 * Simple representation of a Grabbit's Job Resource provided by {@link GrabbitResourceProvider}
 */
@CompileStatic
class JobResource extends SyntheticResource {
    public static final String JOB_EXECUTION_ID = "twc:grabbit.jobExecutionId"

    JobResource(ResourceResolver resourceResolver, ResourceMetadata rm, String resourceType) {
        super(resourceResolver, rm, resourceType)
    }
}
