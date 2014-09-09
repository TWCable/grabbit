package com.twc.webcms.sync.servlets

import com.twc.webcms.sync.proto.AddressBookProtos
import com.twc.webcms.sync.services.SyncService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.felix.scr.annotations.Reference
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet

import javax.servlet.ServletException

@CompileStatic
@Slf4j
@SlingServlet( methods = ['GET'], paths = ["/bin/sync/doSync"] )
class SyncServerServlet extends SlingAllMethodsServlet{

    @Reference
    SyncService helloWorldService

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException{

        AddressBookProtos.Person.Builder builder = AddressBookProtos.Person.newBuilder()
        AddressBookProtos.Person person = builder.setId(1).setEmail("AnEmail").setName("APerson").build()

        person.writeDelimitedTo(response.getOutputStream())
    }


}
