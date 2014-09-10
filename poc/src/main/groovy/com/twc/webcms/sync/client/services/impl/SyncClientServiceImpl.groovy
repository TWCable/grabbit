package com.twc.webcms.sync.client.services.impl

import com.twc.webcms.sync.client.services.SyncClientService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.resource.ResourceResolverFactory

@Slf4j
@CompileStatic
@Component(label = "Content Sync Client Service", description = "Content Sync Client Service", immediate = true, metatype = true, enabled = true)
@Service(SyncClientService)
class SyncClientServiceImpl implements SyncClientService {

    @Reference(bind = 'setResourceResolverFactory')
    ResourceResolverFactory resourceResolverFactory

    public void doSync(String rootPath) {

        //Node currentVersionNode = JcrUtils.getOrCreateByPath(pathToWrite, JcrConstants.NT_UNSTRUCTURED , session)
        //currentVersionNode.setProperty("value", currentVersion)
       // get(createHTTPBuilder(rootPath))


    }

//    private static HTTPBuilder createHTTPBuilder(String rootPath) {
//        final uri = "http://sagar.twcable.com:4503/bin/server/sync?rootPath=${rootPath}"
//        final builder = new HTTPBuilder(uri)
//        builder.auth.basic("admin", "admin")
//        return builder
//    }
//
//    private static void get(HTTPBuilder httpBuilder) {
//
//        httpBuilder.get( contentType : ContentType.BINARY ) { resp, reader ->
//            log.info "${reader}"
//
//        }
////        httpBuilder.request(Method.GET, ContentType.BINARY) {
////            response.success = { resp, reader ->
////                log.info "${reader}"
////            }
////        }
//    }


}
