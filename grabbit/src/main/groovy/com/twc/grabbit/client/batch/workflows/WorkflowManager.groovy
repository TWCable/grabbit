package com.twc.grabbit.client.batch.workflows

interface WorkflowManager {

    void turnOff(Collection<String> wfConfigIds)

    void turnOn(Collection<String> wfConfigIds)

}
