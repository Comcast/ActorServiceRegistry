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

import akka.actor._
import SampleServiceProtocol._
import com.comcast.csv.common.protocol.ServiceRegistryProtocol._

/**
 * Companion to SampleService.
 */
object SampleService {

  def props = Props[SampleService]

}

/**
 * Sample Service for testing ServiceRegistry.
 *
 * @author dbolene
 */
class SampleService extends Actor with ActorLogging {

  var serviceName: Option[String] = None
  var registry: Option[ActorRef] = None
  var dependentServices = new scala.collection.mutable.HashMap[String, List[ActorRef]]
  var routedMessageCount = 0

  override def receive = {

    case initialize: SampleServiceInitialize =>
      log.info(s"Received -> SampleServiceInitialize: $initialize")
      serviceName = Option(initialize.serviceName)
      registry = Option(initialize.registry)
      registry.foreach(r => serviceName.foreach(name => r ! PublishService(serviceName = name, serviceEndpoint = self)))

    case ss: SampleServiceSubscribeTo =>
      log.info(s"Received -> SampleServiceSubscribeTo: $ss")
      registry.foreach(r => r ! SubscribeToService(ss.serviceName))

    case sa: ServiceChanged =>
      log.info(s"Received -> ServiceChanged: $sa")
      dependentServices += (sa.serviceName -> sa.serviceEndpoint)

    case GoOffline =>
      log.info(s"Received -> GoOffline")
      registry.foreach(r => serviceName.foreach(name => r ! UnPublishService(name, self)))

    case msg =>
      sender ! msg
      routedMessageCount += 1
      log.warning(s"Received unknown message: $msg")
  }

}

object SampleServiceProtocol {
  case class SampleServiceInitialize(serviceName: String, registry: ActorRef)
  case class SampleServiceSubscribeTo(serviceName: String)
  case object GoOffline
}