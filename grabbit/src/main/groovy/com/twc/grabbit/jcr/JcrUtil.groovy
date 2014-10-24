package com.twc.grabbit.jcr;


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

/**
 * Utility method to work with JCR Sessions
 * Ported from omega project
 */
@Slf4j
@CompileStatic
public class JcrUtil {

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
