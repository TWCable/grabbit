package com.twc.grabbit.client

import com.twc.grabbit.GrabbitConfiguration
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
                        "path" : "/content/residential-admin/checkout-mocks",
                        "workflowConfigIds" : []
                    },
                    {
                        "path" : "/content/residential-admin/ProductCatalog",
                        "workflowConfigIds" :
                        [
                            "/etc/workflow/launcher/config/update_asset_mod",
                            "/etc/workflow/launcher/config/update_asset_create",
                            "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                            "/etc/workflow/launcher/config/dam_xmp_writeback"
                        ]
                    },
                    {
                        "path" : "/content/dam/business/images",
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
                        "path" : "/content/residential-admin/checkout-mocks",
                        "workflowConfigIds" : []
                    },
                    {
                        "path" : "/content/residential-admin/ProductCatalog",
                        "workflowConfigIds" :
                        [
                            "/etc/workflow/launcher/config/update_asset_mod",
                            "/etc/workflow/launcher/config/update_asset_create",
                            "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                            "/etc/workflow/launcher/config/dam_xmp_writeback"
                        ]
                    },
                    {
                        "path" : "/content/dam/business/images",
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
                    "path" : "/content/residential-admin/checkout-mocks",
                    "workflowConfigIds" : []
                },
                {
                    "path" : "/content/residential-admin/ProductCatalog",
                    "workflowConfigIds" :
                    [
                        "/etc/workflow/launcher/config/update_asset_mod",
                        "/etc/workflow/launcher/config/update_asset_create",
                        "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                        "/etc/workflow/launcher/config/dam_xmp_writeback"
                    ]
                },
                {
                    "path" : "/content/dam/business/images",
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
                    "path" : "/content/residential-admin/ProductCatalog",
                    "workflowConfigIds" :
                    [
                        "/etc/workflow/launcher/config/update_asset_mod",
                        "/etc/workflow/launcher/config/update_asset_create",
                        "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                        "/etc/workflow/launcher/config/dam_xmp_writeback"
                    ]
                },
                {
                    "path" : "/content/dam/business/images",
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
