package com.twc.grabbit.client

import com.twc.grabbit.GrabbitConfiguration
import spock.lang.Specification
import spock.lang.Subject

@Subject(GrabbitConfiguration)
class GrabbitConfigurationSpec extends Specification {

    def "Should create configuration from json input"() {
        given:
        def input = """
        {
            "serverUsername" : "admin",
            "serverPassword" : "admin",
            "serverHost" : "localhost",
            "serverPort" : "4503",
            "deltaContent" : true,
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
        when:
        def output = GrabbitConfiguration.create(input)

        then:
        output instanceof GrabbitConfiguration
        output.deltaContent == true

    }

}
