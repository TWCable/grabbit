package com.twc.grabbit.client.services

import com.twc.grabbit.GrabbitConfiguration

interface ClientService {

    /**
     * This API will perform Content Grab for the given configuration
     * @param configuration : the {@link GrabbitConfiguration}
     * @return Collection of Job's Execution Ids
     */
    Collection<Long> initiateGrab(GrabbitConfiguration configuration)
}
