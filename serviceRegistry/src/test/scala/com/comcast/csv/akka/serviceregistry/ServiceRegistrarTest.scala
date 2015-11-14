package com.comcast.csv.akka.serviceregistry

import akka.actor.ActorSystem
import akka.testkit.{TestProbe, ImplicitSender, TestActorRef, TestKit}
import com.comcast.csv.akka.serviceregistry.SampleServiceProtocol.{GoOffline, SampleServiceInitialize}
import com.comcast.csv.common.ServiceRegistrar
import com.comcast.csv.common.protocol.ServiceProtocol.ServiceNotOnline
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}


class ServiceRegistrarTest extends TestKit(ActorSystem("test")) with FlatSpecLike with ImplicitSender with BeforeAndAfterAll{

  "The service registrar" must "appropriately route messages to two end points" in {
    // start the registry
    val registry = system.actorOf(ServiceRegistry.props)

    // start a sample service
    val aSampleService = TestActorRef[SampleService]
    aSampleService ! SampleServiceInitialize(serviceName = "sampleService", registry = registry)

    val aSampleService2 = TestActorRef[SampleService]
    aSampleService2 ! SampleServiceInitialize(serviceName = "sampleService", registry = registry)

    val registrar = TestActorRef(new ServiceRegistrar("sampleService", registry))

    Thread sleep 100

    registrar ! "echo"

    expectMsg("echo")


    registrar ! "echo"

    expectMsg("echo")

    assert(aSampleService.underlyingActor.routedMessageCount == 1)
    assert(aSampleService2.underlyingActor.routedMessageCount == 1)

  }

  "The service registrar" must "say the service is unavailable when services are unpublished" in {
    // start the registry
    val registry = system.actorOf(ServiceRegistry.props)

    // start a sample service
    val aSampleService = TestActorRef[SampleService]
    aSampleService ! SampleServiceInitialize(serviceName = "sampleService", registry = registry)

    val aSampleService2 = TestActorRef[SampleService]
    aSampleService2 ! SampleServiceInitialize(serviceName = "sampleService", registry = registry)

    val registrar = TestActorRef(new ServiceRegistrar("sampleService", registry))

    Thread sleep 100

    registrar ! "echo"

    expectMsg("echo")


    registrar ! "echo"

    expectMsg("echo")

    assert(aSampleService.underlyingActor.routedMessageCount == 1)
    assert(aSampleService2.underlyingActor.routedMessageCount == 1)

    aSampleService ! GoOffline
    aSampleService2 ! GoOffline

    Thread sleep 100

    registrar ! "echo"

    expectMsg(ServiceNotOnline("sampleService"))

  }

  override def afterAll() = {
    system.shutdown()
  }

}

/*

object ServiceRegistrar {
   def props(serviceName: String, serviceRegistry: ActorRef): Props =
     Props(new ServiceRegistrar(serviceName, serviceRegistry))
 }

class ServiceRegistrar(serviceName: String, serviceRegistry: ActorRef) extends Actor {

   var router = Router(RoundRobinRoutingLogic())

   serviceRegistry ! SubscribeToService(serviceName)

   def receive = {

     case serviceChanged: ServiceChanged =>
       serviceChanged.serviceEndpoint foreach {
         endpoint =>
           router = router.addRoutee(endpoint)
       }

     case x =>
       if (router.routees.size > 0) {
         router.route(x, sender())
       } else {
         sender() ! ServiceNotOnline(serviceName)
       }
   }

   override def postStop(): Unit = {
     serviceRegistry ! UnSubscribeToService(serviceName)
     super.postStop()
   }
 }
*/
