package partion_proto

import grails.test.*

class EventIntegrationTests extends GroovyTestCase 
{
	def eventList = []
	
    protected void setUp() 
	{
        super.setUp()
		
		(92..110).each {

			println "it = ${it}"
			
			Event event = new Event(timestamp: new Date(it, 0, 1))
			event.save(failOnError: true, flush: true)
			eventList += event
		}
    }

    protected void tearDown() 
	{
        super.tearDown()
    }

    void testSomething() 
	{
		assertEquals(19, Event.count())
    }
}
