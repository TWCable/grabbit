package com.twc.grabbit

import com.google.common.collect.ImmutableMap
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
    boolean deltaContent
    Collection<PathConfiguration> pathConfigurations

    private GrabbitConfiguration(@Nonnull String user, @Nonnull String pass, @Nonnull String host,
                                 @Nonnull String port, boolean deltaContent, @Nonnull Collection<PathConfiguration> pathConfigs) {
        // all input is being verified by the "create" factory method
        this.serverUsername = user
        this.serverPassword = pass
        this.serverHost = host
        this.serverPort = port
        this.deltaContent = deltaContent
        this.pathConfigurations = pathConfigs
    }

    public static GrabbitConfiguration create(@Nonnull String configJson) {
        log.debug "Input: ${configJson}"

        final configMap = new JsonSlurper().parseText(configJson) as Map<String, String>

        def errorBuilder = ConfigurationException.builder()

        def serverUsername = nonEmpty(configMap, 'serverUsername', errorBuilder)
        def serverPassword = nonEmpty(configMap, 'serverPassword', errorBuilder)
        def serverHost = nonEmpty(configMap, 'serverHost', errorBuilder)
        def serverPort = nonEmpty(configMap, 'serverPort', errorBuilder)
        def deltaContent = boolVal(configMap, 'deltaContent')

        final Collection<PathConfiguration> pathConfigurations = configMap["pathConfigurations"]?.collect { Map config ->
            def path = nonEmpty(config, 'path', errorBuilder)
            def workflowConfigIds = config["workflowConfigIds"] as Collection<String> ?: [] as Collection<String>
            new PathConfiguration(path, workflowConfigIds)
        } ?: [] as Collection<PathConfiguration>

        if (pathConfigurations.isEmpty()) errorBuilder.add('pathConfigurations', 'is empty')

        if (errorBuilder.hasErrors()) throw errorBuilder.build()

        return new GrabbitConfiguration(
                serverUsername,
                serverPassword,
                serverHost,
                serverPort,
                deltaContent,
                pathConfigurations
        )
    }

    private static String nonEmpty(Map<String, String> configMap, String key,
                                   ConfigurationException.Builder errorBuilder) {
        if (configMap.containsKey(key)) {
            def val = configMap.get(key)
            if (val.isAllWhitespace())
                errorBuilder.add(key, 'is empty')
            return val
        } else {
            errorBuilder.add(key, 'is missing')
            return null
        }
    }

    private static boolean boolVal(Map<String, String> configMap, String key) {
        if(!configMap.containsKey(key)) {
            log.warn "Input doesn't contain ${key} for a boolean value. Will default to false"
        }
        def boolVal = configMap.get(key) as boolean
        return boolVal
    }

    @CompileStatic
    static class ConfigurationException extends RuntimeException {
        final Map<String, String> errors

        ConfigurationException(@Nonnull String msg, @Nonnull Map<String, String> errors) {
            super(msg)
            this.errors = errors
        }

        static Builder builder() {
            return new Builder()
        }

        @CompileStatic
        static class Builder {
            private ImmutableMap.Builder mapBuilder = ImmutableMap.builder()
            private Map map

            ConfigurationException build() {
                if (map == null) map = mapBuilder.build()
                return new ConfigurationException("Errors: ${map}", map)
            }

            Builder add(String keyName, String errorMessage) {
                if (map != null) throw new IllegalStateException("Can't continue to add after building or checking for errors")
                mapBuilder.put(keyName, errorMessage)
                return this
            }

            boolean hasErrors() {
                map = mapBuilder.build()
                return !map.isEmpty()
            }
        }
    }

    @CompileStatic
    static class PathConfiguration {
        String path
        Collection<String> workflowConfigIds

        protected PathConfiguration(@Nonnull String path, @Nonnull Collection<String> workflowConfigIds) {
            this.path = path
            this.workflowConfigIds = workflowConfigIds
        }
    }

}
