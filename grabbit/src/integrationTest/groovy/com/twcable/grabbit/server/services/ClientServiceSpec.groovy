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

package com.twcable.grabbit.server.services

import com.twcable.grabbit.GrabbitConfiguration
import com.twcable.grabbit.client.services.ClientService
import com.twcable.grabbit.client.services.impl.DefaultClientService
import com.twcable.grabbit.jcr.AbstractJcrSpec
import com.twcable.jackalope.impl.sling.SimpleResourceResolverFactory
import com.twcable.jackalope.impl.sling.SlingRepositoryImpl
import org.apache.sling.jcr.api.SlingRepository
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.context.support.StaticApplicationContext
import spock.lang.Subject

@Subject(ServerService)
class ClientServiceSpec extends AbstractJcrSpec {

    SlingRepository slingRepository

    ConfigurableApplicationContext appCtx

    ClientService syncClientService


    def setup() {
        slingRepository = new SlingRepositoryImpl()

        ApplicationContext parentAppCtx = new StaticApplicationContext()
        parentAppCtx.beanFactory.registerSingleton("slingRepository", slingRepository)
        parentAppCtx.beanFactory.registerSingleton("resourceResolverFactory", new SimpleResourceResolverFactory(slingRepository))
        parentAppCtx.refresh()
        appCtx = new ClassPathXmlApplicationContext(["META-INF/spring/client-batch-job.xml", "META-INF/spring/client-workflow-on-step.xml", "META-INF/spring/client-workflow-off-step.xml"] as String[], parentAppCtx)

        syncClientService = new DefaultClientService(slingRepository: slingRepository,
                applicationContext: appCtx)
    }


    def "Service initiate a grab and return jobs"() {
        when:
        def jobIds = syncClientService.initiateGrab(new GrabbitConfiguration("admin", "adminPass", "testbox", "4502", false, [new GrabbitConfiguration.PathConfiguration("/content/test", [], [], false)]), "admin")

        then:
        jobIds.size() == 1
    }

}
