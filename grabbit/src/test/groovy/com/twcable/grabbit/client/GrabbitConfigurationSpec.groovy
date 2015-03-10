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

    @Unroll
    def "Should create configuration from json input"() {
        when:
        def output = GrabbitConfiguration.create(input)

        then:
        output instanceof GrabbitConfiguration
        output.deltaContent == false

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
        final GrabbitConfiguration.ConfigurationException  exception = thrown()
        exception.errors == errors


        where:

        input                                                                   | errors
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
        """                                                                         | [ serverUsername : "is missing" ]
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
        """                                                                         | [ serverPort : "is missing",
                                                                                        path : "is missing" ]


    }

}
