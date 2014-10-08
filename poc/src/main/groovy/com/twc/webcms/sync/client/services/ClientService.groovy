package com.twc.webcms.sync.client.services

interface ClientService {

    /**
     * This API will perform Content Sync for the given list of paths
     * @param whiteList : the list of paths to be synced
     */
    void doSync(Collection<String> whiteList)
}
