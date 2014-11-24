package com.twc.grabbit.spring.batch.repository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * A simple base class that is extended by all the DAOs
 */
@CompileStatic
@Slf4j
abstract class AbstractJcrDao {

    public static final String ROOT_RESOURCE_NAME = "/var/grabbit/job/repository"

    protected abstract void ensureRootResource()

    /**
     * Uses {@link UUID#randomUUID()} to get a Unique ID assigned to "new instances" of
     * {@link JcrExecutionContextDao}, {@link JcrJobExecutionDao}, {@link JcrStepExecutionDao}
     * @return the nextId
     */
    protected static Long generateNextId() {
        final uuid = UUID.randomUUID()
        long nextId = uuid.mostSignificantBits ^ uuid.leastSignificantBits
        //Making sure that the nextId is not negative.
        nextId >>>= 1
        Long.valueOf(nextId)
    }
}
