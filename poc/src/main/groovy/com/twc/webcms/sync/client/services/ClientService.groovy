package com.twc.webcms.sync.client.services

interface ClientService {

    /**
     * This API will perform Content Grab for the given list of paths
     * @param whiteList : the list of paths to be synced
     * @return Collection of Job's Execution Ids
     */
    Collection<Long> initiateGrab(Collection<String> whiteList)
}
