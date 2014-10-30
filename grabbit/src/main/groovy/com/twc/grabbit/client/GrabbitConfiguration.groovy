package com.twc.grabbit.client

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.annotation.Nonnull

@CompileStatic
@Slf4j
class GrabbitConfiguration {

    String serverUsername
    String serverPassword
    String serverHost
    String serverPort
    Collection<PathConfiguration> pathConfigurations

    private GrabbitConfiguration(@Nonnull String user, @Nonnull String pass, @Nonnull String host,
                                 @Nonnull String port, @Nonnull Collection<PathConfiguration> pathConfigs) {
        if(!user || !pass || !host || !port || !pathConfigs) {
            throw new IllegalArgumentException("One of more configurations are null or empty. Please verify your configs")
        }
        this.serverUsername = user
        this.serverPassword = pass
        this.serverHost = host
        this.serverPort = port
        this.pathConfigurations = pathConfigs
    }

    public static GrabbitConfiguration create(@Nonnull String configJson) {
        log.debug "Input: ${configJson}"

        final configMap = new JsonSlurper().parseText(configJson) as Map<String, String>

        final Collection<PathConfiguration> pathConfigurations = configMap["pathConfigurations"]?.collect { config ->
            def path = config["path"] as String ?: ""
            def workflowConfigIds = config["workflowConfigIds"] as Collection<String> ?: [] as Collection<String>
            new PathConfiguration(path, workflowConfigIds)
        } ?: [] as Collection<PathConfiguration>

        new GrabbitConfiguration(
            configMap["serverUsername"] as String ?: "",
            configMap["serverPassword"] as String ?: "",
            configMap["serverHost"] as String ?: "",
            configMap["serverPort"] as String ?: "",
            pathConfigurations
        )

    }

    @CompileStatic
    static class PathConfiguration {
        String path
        Collection<String> workflowConfigIds

        protected PathConfiguration(@Nonnull String path, @Nonnull Collection<String> workflowConfigIds) {
            if(!path) throw new IllegalArgumentException("path == null or empty")
            if(workflowConfigIds == null) throw new IllegalArgumentException("workflowConfigIds == null")
            this.path = path
            this.workflowConfigIds = workflowConfigIds
        }
    }

}
