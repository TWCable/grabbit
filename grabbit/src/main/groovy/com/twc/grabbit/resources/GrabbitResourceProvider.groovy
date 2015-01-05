package com.twc.grabbit.resources

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceMetadata
import org.apache.sling.api.resource.ResourceProvider
import org.apache.sling.api.resource.ResourceResolver
import org.osgi.service.component.ComponentContext

import javax.servlet.http.HttpServletRequest
import java.util.regex.Matcher

/**
 * A custom resource provider that provides a {@link JobResource} for paths like
 * /grabbit/job : Initiate a new request
 * /grabbit/job/all.(html|json) : Status of All Jobs
 * /grabbit/job/<id>.(html|json) : Status of A Job
 * @see com.twc.grabbit.servlets.GrabbitServlet
 */
@Slf4j
@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
@Service(ResourceProvider)
@Component(label = 'TWC - Grabbit Resource Provider', description = 'Grabbit Resource Provider', metatype = true)
@Property(name = "service.vendor", value = "Time Warner Cable")
class GrabbitResourceProvider implements ResourceProvider {

    @Property(label = "Provider Roots", description = "Path roots of what this handles", value = ['/grabbit'])
    public static final String PROVIDER_ROOTS = 'provider.roots'
    private List<String> providerRoots

    public static final String GRABBIT_ROOT = "/grabbit"

    public static final String GRABBIT_JOB = "${GRABBIT_ROOT}/job"
    public static final String GRABBIT_JOB_RESOURCE_TYPE = "twc:grabbit/job"

    @Activate
    void activate(ComponentContext context) {
        def roots = context.properties.get(PROVIDER_ROOTS)
        providerRoots = ((roots instanceof String) ? [roots] : roots) as List<String>
    }

    @Override
    Resource getResource(ResourceResolver resourceResolver, HttpServletRequest httpServletRequest, String path) {
        getResource(resourceResolver, path)
    }

    @Override
    Resource getResource(ResourceResolver resolver, String path) {
        switch(path) {
            case ~/^${GRABBIT_JOB}$/:
                log.debug "Called for path : ${path}"
                return getJobResource(resolver, path, GRABBIT_JOB_RESOURCE_TYPE)
            case ~/^${GRABBIT_JOB}\/(.+).(html|json)$/:
                log.debug "Called for path : ${path}"
                return getJobResource(resolver, path, GRABBIT_JOB_RESOURCE_TYPE)
            default:
                log.warn "Unable to find resource for path: ${path}."
                return null
        }
    }

    @Override
    Iterator<Resource> listChildren(Resource resource) {
        log.debug "List Children called for ${resource}"
        return null
    }

    private static Resource getJobResource(ResourceResolver resourceResolver, String path, String resourceType) {
        final metadata = new ResourceMetadata(resolutionPath: path)

        final jobId = getJobIdFromPath(path)
        if(jobId) { metadata.put(JobResource.JOB_EXECUTION_ID, jobId) }

        new JobResource(resourceResolver, metadata, resourceType)
    }

    protected static String getJobIdFromPath(String path) {
        Matcher m = path =~ /\/job\/(.+).(html|json)$/
        if (m.size() > 0) {
            final matchedList = m[0] as List<String>
            return matchedList[1]
        }
        else {
            return null
        }
    }
}
