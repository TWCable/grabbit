package com.twc.webcms.sync.services

import com.twc.webcms.sync.services.impl.HelloWorldServiceImpl
import org.osgi.service.component.ComponentContext
import spock.lang.Specification
import spock.lang.Subject

@Subject(HelloWorldService)
class HelloWorldServiceSpec extends Specification {

	def "Test helloWorldService.getMessage()"() {
		setup:
		final helloWorldService = new HelloWorldServiceImpl()
		final mockContext = Mock(ComponentContext)
        final properties = new Hashtable()
        properties['message'] = "Test Value"
		mockContext.getProperties() >> properties
		helloWorldService.activate(mockContext)

		when:
		final actual = helloWorldService.getMessage()

		then:
		actual == "Test Value"
	}
}

