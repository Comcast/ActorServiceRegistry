package com.comcast.csv.akka.serviceregistry

import akka.actor.{PoisonPill, ActorSystem}
import akka.testkit.{DefaultTimeout, TestKit, ImplicitSender}
import com.comcast.csv.akka.serviceregistry.SampleServiceProtocol._
import com.comcast.csv.common.protocol.ServiceRegistryProtocol._
import org.scalatest._
import scala.concurrent.duration._

/**
 * Unit test the ServiceRegistry.
 *
 * @author dbolene
 */
class TestServiceRegistry extends TestKit(ActorSystem("testSystem"))
with DefaultTimeout
with WordSpecLike with Matchers
with ImplicitSender with BeforeAndAfterAll {

  "A ServiceRegistry actor" when {
    "created, a service is published, and a SubscribedTo is received" should {
      "respond with a ServiceAvailable msg" in {

        // start the registry
        val registry = system.actorOf(ServiceRegistry.props)

        // start a sample service
        val aSampleService = system.actorOf(SampleService.props)
        aSampleService ! SampleServiceInitialize(serviceName = "sampleService", registry = registry)

        // subscribe to the sample service
        registry ! SubscribeToService("sampleService")

        fishForMessage(5 seconds, "hint"){
          case ServiceAvailable("sampleService", a) if a == aSampleService => true
          case _ => false
        }

        registry ! PoisonPill
        aSampleService ! PoisonPill
      }
    }
  }

  "A ServiceRegistry actor" when {
    "created, SubscribedTo is received, and a service is published" should {
      "respond with a ServiceAvailable msg" in {

        // start the registry
        val registry = system.actorOf(ServiceRegistry.props)

        // subscribe to the sample service before it is started
        registry ! SubscribeToService("sampleService")

        // start a sample service
        val aSampleService = system.actorOf(SampleService.props)
        aSampleService ! SampleServiceInitialize(serviceName = "sampleService", registry = registry)

        fishForMessage(5 seconds, "hint"){
          case ServiceAvailable("sampleService", a) if a == aSampleService => true
          case _ => false
        }

        registry ! PoisonPill
        aSampleService ! PoisonPill
      }
    }
  }


  "A ServiceRegistry actor" when {
    "created, a service is published, and a SubscribedTo is received, and the service is taken offline" should {
      "respond with a ServiceUnAvailable msg" in {

        // start the registry
        val registry = system.actorOf(ServiceRegistry.props)

        // start a sample service
        val aSampleService = system.actorOf(SampleService.props)
        aSampleService ! SampleServiceInitialize(serviceName = "sampleService", registry = registry)

        // subscribe to the sample service
        registry ! SubscribeToService("sampleService")

        fishForMessage(5 seconds, "hint"){
          case ServiceAvailable("sampleService", a) if a == aSampleService => true
          case _ => false
        }

        // tell the sample service to go offline
        aSampleService ! GoOffline

        expectMsg(ServiceUnAvailable(serviceName = "sampleService"))

        registry ! PoisonPill
        aSampleService ! PoisonPill
      }
    }
  }

  "A ServiceRegistry actor" when {
    "created, a service is published, and a SubscribedTo is received, and the service is terminated" should {
      "respond with a ServiceUnAvailable msg" in {

        // start the registry
        val registry = system.actorOf(ServiceRegistry.props)

        // start a sample service
        val aSampleService = system.actorOf(SampleService.props)
        aSampleService ! SampleServiceInitialize(serviceName = "sampleService", registry = registry)

        // subscribe to the sample service
        registry ! SubscribeToService("sampleService")

        fishForMessage(5 seconds, "hint"){
          case ServiceAvailable("sampleService", a) if a == aSampleService => true
          case _ => false
        }

        // tell the sample service to go offline
        aSampleService ! PoisonPill

        expectMsg(ServiceUnAvailable(serviceName = "sampleService"))

        registry ! PoisonPill
      }
    }
  }

  override def afterAll(): Unit = {
    shutdown(system)
  }
}
