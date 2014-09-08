package com.twc.webcms.sync.services.impl

import com.twc.webcms.sync.services.SyncService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Properties
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Service
import org.apache.sling.commons.osgi.PropertiesUtil
import org.osgi.service.component.ComponentContext

@Slf4j
@CompileStatic
@Component(label = "Hello World OSGi Service", description = "Hello World Service that returns a String", immediate = true, metatype = true, enabled = true)
@Service(SyncService)
class SyncServiceImpl implements SyncService{

    @Property(label="Message", description="Message")
    public static final String MESSAGE = "message"

    private String message

    @Activate
    protected void activate(ComponentContext ctx){
        message = PropertiesUtil.toString(ctx.properties[MESSAGE], "Hello World!")
    }

    String getMessage() {
        return message
    }

}
