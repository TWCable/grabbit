package com.twc.webcms.sync.jcr;


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ResourceResolverFactory
import org.apache.sling.jcr.api.SlingRepository

import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.jcr.Node
import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.jcr.SimpleCredentials

@Slf4j
@CompileStatic
public class JcrUtil {

    @Nullable
    static String getSlingPathForNode(@Nullable Node node, @Nullable SlingHttpServletRequest request) {
        try {
            getSlingPathForNodePath(node?.path, request)
        }
        catch (RepositoryException e) {
            log.warn("Unable to retrieve node path", e)
            null
        }
    }


    @Nullable
    static String getSlingPathForNodePath(@Nullable String nodePath, @Nullable SlingHttpServletRequest request) {
        (nodePath != null) ?
            request?.resourceResolver?.resolve(nodePath)?.getResourceMetadata()?.getResolutionPath() : null
    }

    /**
     * This creates a ResourceResolver with the rights of the given user (by default
     * that is 'anonymous'). <strong>It is the caller's responsibility to close the ResourceResolver when
     * it is no longer needed.</strong> (See {@link #withResourceResolver} for automatic resource management.)
     *
     * @param resolverFactory the factory to get a ResourceResolver from
     * @param user if null, defaults to 'anonymous'
     * @return the ResourceResolver; never null
     *
     * @see #withResourceResolver
     */

    @Nonnull
    static ResourceResolver getResourceResolver(@Nonnull ResourceResolverFactory resolverFactory, String user = null) {
        if (resolverFactory == null) throw new IllegalArgumentException("resolverFactory == null")

        final authInfo = [:] as Map<String, Object>
        authInfo[ResourceResolverFactory.USER_IMPERSONATION] = user ?: 'anonymous'

        resolverFactory.getAdministrativeResourceResolver(authInfo)
    }

    /**
     * Provides an easy way to run some code with a ResourceResolver safely.
     *
     * This handles creating the ResourceResolver with the rights of the given user (by default
     * that is 'anonymous') and does the needed clean up when the code block is finished.
     * <p/>
     * <code>use(JcrUtil) {<br/>
     * &nbsp resourceResolverFactory.withResourceResolver {ResourceResolver resourceResolver -><br/>
     * &nbsp &nbsp // do something with the ResourceResolver<br/>
     * &nbsp }<br/>
     *}</code>
     * <p/>
     * <strong>By design, the ResourceResolver is closed at the end of the block. That means
     * that if you try to export anything out of the block that relies on a ResourceResolver
     * (such as Nodes) they will not work.</strong>
     *
     * @param resolverFactory the factory to get a ResourceResolver from
     * @param user if null, defaults to 'anonymous'
     * @param closure the code to run with the ResourceResolver
     */
    static <T> T withResourceResolver(@Nonnull ResourceResolverFactory resolverFactory,
                                      String user = null, @Nonnull Closure<T> closure) {
        if (resolverFactory == null) throw new IllegalArgumentException("resolverFactory == null")
        if (closure == null) throw new IllegalArgumentException("closure == null")

        final resourceResolver = getResourceResolver(resolverFactory, user)
        try {
            return closure.call(resourceResolver)
        }
        finally {
            resourceResolver.close()
        }
    }


    /**
     * Provides an easy way to run some code with a ResourceResolver safely.
     *
     * This handles creating the ResourceResolver with the rights of the given user (by default
     * that is 'anonymous') and does the needed clean up when the code block is finished.
     * <p/>
     * <code>use(JcrUtil) {<br/>
     * &nbsp resourceResolverFactory.manageResourceResolver {ResourceResolver resourceResolver -><br/>
     * &nbsp &nbsp // do something with the ResourceResolver<br/>
     * &nbsp }<br/>
     *}</code>
     * <p/>
     * <strong>By design, the ResourceResolver is closed at the end of the block. That means
     * that if you try to export anything out of the block that relies on a ResourceResolver
     * (such as Nodes) they will not work.</strong>
     *
     * @param resolverFactory the factory to get a ResourceResolver from
     * @param closure the code to run with the ResourceResolver
     * @return
     */
    static <T> T manageResourceResolver(@Nonnull ResourceResolverFactory resolverFactory, @Nonnull Closure<T> closure) {
        return withResourceResolver(resolverFactory, "admin", closure)
    }

    /**
     * This creates a JCR Session with the rights of the given user (by default
     * that is 'anonymous'). <strong>It is the caller's responsibility to logout of the Session when
     * it is no longer needed.</strong> (See {@link #withSession} for automatic resource management.)
     *
     * @param slingRepository the Repository to log into
     * @param user if null, defaults to 'anonymous'
     * @return the Session; never null
     *
     * @see #withSession
     */

    @Nonnull
    static Session getSession(@Nonnull SlingRepository slingRepository, String user = null) {
        final authInfo = [:] as Map<String, Object>
        authInfo[ResourceResolverFactory.USER_IMPERSONATION] = user ?: 'anonymous'

        final adminSession = slingRepository.loginAdministrative(null)
        try {
            return adminSession.impersonate(new SimpleCredentials(user ?: 'anonymous', ''.chars))
        }
        finally {
            adminSession.logout()
        }
    }

    /**
     * Provides an easy way to run some code with a JCR Session safely.
     *
     * This handles creating the ResourceResolver with the rights of the given user (by default
     * that is 'anonymous') and does the needed clean up when the code block is finished.
     * <p/>
     * <code>use(JcrUtil) {<br/>
     * &nbsp slingRepository.withSession{Session session -><br/>
     * &nbsp &nbsp // do something with the Session<br/>
     * &nbsp }<br/>
     *}</code>
     *
     * @param slingRepository the factory to get a Session from
     * @param user if null, defaults to 'anonymous'
     * @param closure the code to run with the Session
     */

    static void withSession(@Nonnull SlingRepository slingRepository, String user = null, @Nonnull Closure closure) {
        final session = getSession(slingRepository, user)
        try {
            closure.call(session)
        }
        finally {
            session.logout()
        }
    }

}
