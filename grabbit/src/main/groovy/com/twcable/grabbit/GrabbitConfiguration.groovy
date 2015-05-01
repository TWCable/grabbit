/*
 * Copyright 2015 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twcable.grabbit

import com.google.common.collect.ImmutableMap
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.annotation.Nonnull
import javax.annotation.Nullable
import java.util.regex.Pattern

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
                                 @Nonnull String port, boolean deltaContent,
                                 @Nonnull Collection<PathConfiguration> pathConfigs) {
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
            def excludePaths = nonEmptyCollection(config["excludePaths"] as Collection<String>, errorBuilder)
            def workflowConfigIds = config["workflowConfigIds"] as Collection<String> ?: [] as Collection<String>
            new PathConfiguration(path, excludePaths, workflowConfigIds)
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

    private static final Pattern prePattern = Pattern.compile(/^(\/|\.\/|\\).*$/)
    private static final Pattern postPattern = Pattern.compile(/^.*(\/|\.\/|\\)$/)

    private
    static Collection<String> nonEmptyCollection(Collection<String> excludePaths, ConfigurationException.Builder errorBuilder) {
        if (excludePaths == null || excludePaths.isEmpty())
            return Collections.EMPTY_LIST
        if (excludePaths.any({ it == null || it.isEmpty() })) {
            errorBuilder.add('excludePaths', 'contains null/empty')
            return excludePaths
        } else if (excludePaths.any { it.matches(prePattern) || it.matches(postPattern) } ) {
            errorBuilder.add('excludePaths', 'relative paths provided cannot begin or end with /, ./ or \\')
            return excludePaths
        } else {
            return excludePaths
        }
    }


    private static String nonEmpty(Map<String, String> configMap, String key,
                                   ConfigurationException.Builder errorBuilder) {
        if (configMap.containsKey(key)) {
            def val = configMap.get(key)
            if (val.isAllWhitespace())
                errorBuilder.add(key, 'is empty')
            return val
        }
        else {
            errorBuilder.add(key, 'is missing')
            return null
        }
    }


    private static boolean boolVal(Map<String, String> configMap, String key) {
        if (!configMap.containsKey(key)) {
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
        Collection<String> excludePaths
        Collection<String> workflowConfigIds

        void setPath(@Nullable String path) {
            this.path = ( path!=null && path.endsWith("/") ) ? path[0..-2] : path
        }

        Collection<String> getAbsolutePaths(@Nonnull Collection<String> excludePaths) {
            if(this.path!=null)
                return excludePaths.collect { "${this.path}/${it}" }
            else
                return excludePaths
        }

        protected PathConfiguration(@Nonnull String path, @Nonnull Collection<String> excludePaths, @Nonnull Collection<String> workflowConfigIds) {
            setPath(path)
            this.excludePaths = getAbsolutePaths(excludePaths)
            this.workflowConfigIds = workflowConfigIds
        }
    }

}
