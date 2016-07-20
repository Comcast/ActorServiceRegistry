/**
 * Copyright  2015  Comcast Cable Communications Management, LLC
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

  "A ServiceRegistry actor" when {
    "created, a service is published, and a RequestService is received" should {
      "respond with a RespondService msg" in {

        // start the registry
        val registry = system.actorOf(ServiceRegistry.props)

        // start a sample service
        val aSampleService = system.actorOf(SampleService.props)
        aSampleService ! SampleServiceInitialize(serviceName = "sampleService", registry = registry)

        Thread.sleep(5000)

        // request the sample service
        registry ! RequestService("sampleService")

        fishForMessage(20 seconds, "hint"){
          case RespondService("sampleService", a) if a == aSampleService => true
          case _ => false
        }

        // tell the sample service to go offline
        aSampleService ! PoisonPill

      /*  Thread.sleep(5000)

        // request the sample service
        registry ! RequestService("sampleService")

        fishForMessage(20 seconds, "hint"){
          case RespondServiceUnAvailable("sampleService") => true
          case _ => false
        } */

        registry ! PoisonPill
      }
    }
  }

  override def afterAll(): Unit = {
    shutdown(system)
  }
}
