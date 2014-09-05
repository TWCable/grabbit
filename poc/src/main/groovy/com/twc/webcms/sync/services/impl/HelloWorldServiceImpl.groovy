package com.twc.webcms.sync.services.impl

import com.twc.webcms.sync.services.HelloWorldService
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Properties
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Service
import org.apache.sling.commons.osgi.PropertiesUtil
import org.osgi.service.component.ComponentContext

@Component(label = "Hello World OSGi Service", description = "Hello World Service that returns a String", immediate = true, metatype = true, enabled = true)
@Service(HelloWorldService)
@Properties([
    @Property(label = "Message", name = "message", value = "Hello World!")
])
class HelloWorldServiceImpl implements HelloWorldService{

    private String message

    @Activate
    protected void activate(ComponentContext ctx){
        message = PropertiesUtil.toString(ctx.properties["message"], "Hello World!")
    }

    String getMessage() {
        return message
    }

}
