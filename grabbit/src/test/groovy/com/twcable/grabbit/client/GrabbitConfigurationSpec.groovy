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

package com.twcable.grabbit.client

import com.twcable.grabbit.GrabbitConfiguration
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Subject(GrabbitConfiguration)
class GrabbitConfigurationSpec extends Specification {

    def "Should return exclude paths"() {
        given:
        def input = """
        {
            "serverUsername" : "admin",
            "serverPassword" : "admin",
            "serverHost" : "localhost",
            "serverPort" : "4503",
            "deltaContent" : false,
            "pathConfigurations" :
            [
                {
                    "path" : "/content/a/b",
                    "excludePaths" : ["d", "e/ab", "q/fg"],
                    "workflowConfigIds" : []
                }
            ]
		}
        """
        def expectedOutput = ["/content/a/b/d", "/content/a/b/e/ab", "/content/a/b/q/fg"]

        when:
        def actualOutput = GrabbitConfiguration.create(input)

        then:
        actualOutput instanceof GrabbitConfiguration
        actualOutput.pathConfigurations.first().excludePaths == expectedOutput
    }

    @Unroll
    def "throws exception for non-relative exclude path: #excludePath"() {
        when:
        def input = """
        {
            "serverUsername" : "admin",
            "serverPassword" : "admin",
            "serverHost" : "localhost",
            "serverPort" : "4503",
            "deltaContent" : false,
            "pathConfigurations" :  [
            {
                "path" : "/content/a/b",
                "excludePaths" : ["${excludePath}"],
                "workflowConfigIds" : []
            }
          ]
        }
        """
        GrabbitConfiguration.create(input)

        then:
        GrabbitConfiguration.ConfigurationException exception = thrown()
        exception.errors == [excludePaths: "relative paths provided cannot begin or end with /, ./ or \\"]

        where:
        excludePath << ["/a", "\\\\b" , "./c"]
    }

    @Unroll
    def "generates correct exclusions regardless of trailing slash for path: #excludePath"() {
        given:
        def input = """
        {
            "serverUsername" : "admin",
            "serverPassword" : "admin",
            "serverHost" : "localhost",
            "serverPort" : "4503",
            "deltaContent" : false,
            "pathConfigurations" :  [
            {
                "path" : "${path}",
                "excludePaths" : ["d", "e/ab", "q/fg"],
                "workflowConfigIds" : []
            }
          ]
        }
        """
        def expectedOutput = ["/content/a/b/d", "/content/a/b/e/ab", "/content/a/b/q/fg"]

        when:
        def actualOutput = GrabbitConfiguration.create(input)

        then:
        actualOutput instanceof GrabbitConfiguration
        actualOutput.pathConfigurations.first().excludePaths == expectedOutput

        where:
        path << ["/content/a/b", "/content/a/b/"]
    }

    def "Should fail to process exclude paths"() {
        given:
        def input  = """
        {
            "serverUsername" : "admin",
            "serverPassword" : "admin",
            "serverHost" : "localhost",
            "serverPort" : "4503",
            "deltaContent" : false,
            "pathConfigurations" :  [
                {
                    "path" : "/content/a/b",
                    "excludePaths" :["b"],
                    "workflowConfigIds" : []
                },
                {
                    "path" : "/content/a/c",
                    "excludePaths" :["b"],
                    "workflowConfigIds" :
                    [
                        "/etc/workflow/launcher/config/update_asset_mod",
                        "/etc/workflow/launcher/config/update_asset_create",
                        "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                        "/etc/workflow/launcher/config/dam_xmp_writeback"
                    ]
                },
                {
                    "path" : "/content/dam/d/images",
                    "excludePaths" :["f",null],
                    "workflowConfigIds" :
                    [
                        "something"
                    ]
                }
            ]
        }
        """
        def errors =  [excludePaths: "contains null/empty"]

        when:
        GrabbitConfiguration.create(input)

        then:
        final GrabbitConfiguration.ConfigurationException exception = thrown()
        exception.errors == errors

    }

    @Unroll
    def "Should create configuration from json input"() {
        when:
        final configuration = GrabbitConfiguration.create(input)

        then:
        configuration instanceof GrabbitConfiguration
        !configuration.deltaContent
        configuration.transactionID != null


        where:
        input << [
            """
            {
                "serverUsername" : "admin",
                "serverPassword" : "admin",
                "serverHost" : "localhost",
                "serverPort" : "4503",
                "deltaContent" : false,
                "pathConfigurations" :  [
                    {
                        "path" : "/content/a/b",
                        "workflowConfigIds" : []
                    },
                    {
                        "path" : "/content/a/c",
                        "workflowConfigIds" :
                        [
                            "/etc/workflow/launcher/config/update_asset_mod",
                            "/etc/workflow/launcher/config/update_asset_create",
                            "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                            "/etc/workflow/launcher/config/dam_xmp_writeback"
                        ]
                    },
                    {
                        "path" : "/content/dam/d/images",
                        "workflowConfigIds" :
                        [
                            "something"
                        ]
                    }
                ]
            }
            """,
            """
            {
                "serverUsername" : "admin",
                "serverPassword" : "admin",
                "serverHost" : "localhost",
                "serverPort" : "4503",
                "pathConfigurations" :  [
                    {
                        "path" : "/content/a/b",
                        "workflowConfigIds" : []
                    },
                    {
                        "path" : "/content/a/c",
                        "workflowConfigIds" :
                        [
                            "/etc/workflow/launcher/config/update_asset_mod",
                            "/etc/workflow/launcher/config/update_asset_create",
                            "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                            "/etc/workflow/launcher/config/dam_xmp_writeback"
                        ]
                    },
                    {
                        "path" : "/content/dam/d/images",
                        "workflowConfigIds" :
                        [
                            "something"
                        ]
                    }
                ]
            }
            """
        ]
    }


    @Unroll
    def "Should fail to create configuration from json input"() {
        when:
        GrabbitConfiguration.create(input)

        then:
        final GrabbitConfiguration.ConfigurationException exception = thrown()
        exception.errors == errors


        where:

        input          | errors
        """
        {
            "serverPassword" : "admin",
            "serverHost" : "localhost",
            "serverPort" : "4503",
            "deltaContent" : false,
            "pathConfigurations" :  [
                {
                    "path" : "/content/a/b",
                    "workflowConfigIds" : []
                },
                {
                    "path" : "/content/a/c",
                    "workflowConfigIds" :
                    [
                        "/etc/workflow/launcher/config/update_asset_mod",
                        "/etc/workflow/launcher/config/update_asset_create",
                        "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                        "/etc/workflow/launcher/config/dam_xmp_writeback"
                    ]
                },
                {
                    "path" : "/content/dam/d/images",
                    "workflowConfigIds" :
                    [
                        "something"
                    ]
                }
            ]
        }
        """ | [serverUsername: "is missing"]
        """
        {
            "serverUsername" : "admin",
            "serverPassword" : "admin",
            "serverHost" : "localhost",
            "pathConfigurations" :  [
                {
                },
                {
                    "path" : "/content/a/c",
                    "workflowConfigIds" :
                    [
                        "/etc/workflow/launcher/config/update_asset_mod",
                        "/etc/workflow/launcher/config/update_asset_create",
                        "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                        "/etc/workflow/launcher/config/dam_xmp_writeback"
                    ]
                },
                {
                    "path" : "/content/dam/d/images",
                    "workflowConfigIds" :
                    [
                        "something"
                    ]
                }
            ]
        }
        """ | [serverPort: "is missing",
               path      : "is missing"]
    }

    @Unroll
    def "pathDeltaContent should override global deltaContent setting"() {
        when:
        def output = GrabbitConfiguration.create(input)

        then:
        output instanceof GrabbitConfiguration
        output.pathConfigurations.first().pathDeltaContent == false
        output.pathConfigurations.last().pathDeltaContent == true

        where:
        input << [
                """
            {
                "serverUsername" : "admin",
                "serverPassword" : "admin",
                "serverHost" : "localhost",
                "serverPort" : "4503",
                "deltaContent" : true,
                "pathConfigurations" :  [
                    {
                        "path" : "/content/a/b",
                        "workflowConfigIds" : [],
                        "deltaContent" : false
                    },
                    {
                        "path" : "/content/a/c",
                        "workflowConfigIds" : []
                    }
                ]
            }
            """,
                """
            {
                "serverUsername" : "admin",
                "serverPassword" : "admin",
                "serverHost" : "localhost",
                "serverPort" : "4503",
                "pathConfigurations" :  [
                    {
                        "path" : "/content/a/b",
                        "workflowConfigIds" : []
                    },
                    {
                        "path" : "/content/a/c",
                        "workflowConfigIds" : [],
                        "deltaContent" : true
                    }
                ]
            }
            """     //no global deltaContent setting, since without it, it defaults to false
        ]
    }
    def "Validate YAML input"() {
        when:
        def input =
        """
        clientNodeType : 'author'
        serverUsername : 'admin'
        serverPassword : 'admin'
        serverHost : 'localhost'
        serverPort : 4502
        deltaContent : true
        workflowConfigIds : &configs
          - '/configA'
          - '/configB'
          - '/configC'
        pathConfigurations :
          -
            path : '/a/b'
            excludePaths :
              - 'c'
              - 'd'
            workflowConfigIds : *configs
          -
            path : '/x/y'
            excludePaths :
              - 'z'
            workflowConfigIds : *configs
        """
        def output = GrabbitConfiguration.create(input)
        then:
        output instanceof GrabbitConfiguration
        output.pathConfigurations.first().workflowConfigIds.size() > 0
        output.pathConfigurations.first().excludePaths.size() > 0
        output.pathConfigurations.first().workflowConfigIds.first() == "/configA"
        output.pathConfigurations.first().excludePaths.first() == "/a/b/c"
        output.pathConfigurations.first().deleteBeforeWrite == false
        output.deltaContent instanceof Boolean
    }

    def "JSON Files with hard tabs as indentation should work"() {
        when:
        def input =
        """
        {
        \t"serverUsername" : "admin",
        \t"serverPassword" : "admin",
        \t"serverHost" : "localhost",
        \t"serverPort" : "4503",
        \t"deltaContent" : false,
        \t"pathConfigurations" :  [
        \t\t{
        \t\t\t"path" : "/content/a/b",
        \t\t\t"workflowConfigIds" : []
        \t\t}
        \t]
        }
        """
        def output = GrabbitConfiguration.create(input)
        then:
        output instanceof GrabbitConfiguration
        notThrown(Exception)
    }
}
