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
package com.comcast.csv.common.protocol

import akka.actor.ActorRef

/**
 * Protocol for interacting with the Service Registry.
 *
 * @author dbolene
 */
object ServiceRegistryProtocol {

  /**
   * Service implementor sends to ServiceRegistry when transitions to online.
   */
  case class PublishService(serviceName: String, serviceEndpoint: ActorRef)
  /**
   * Service implementor sends to ServiceRegistry when transitions to offline.
   */
  case class UnPublishService(serviceName: String, serviceEndpoint: ActorRef)
  /**
   * Service client sends to ServiceRegistry when requiring dependent service.
   */
  case class SubscribeToService(serviceName: String)

  /**
   * Service client sends to ServiceRegistry when no longer requiring dependent service.
   */
  case class UnSubscribeToService(serviceName: String)

  /**
   * ServiceRegistry sends to publishers and subscribers when ServiceRegistry has been restarted
   *   requiring all participants to re-subscribe and re-publish.
   */
  case class RegistryHasRestarted(registry: ActorRef)

  /**
   * Message sent whenever an actor has entered or exited the service pool
   */
  case class ServiceChanged(serviceName: String, serviceEndpoint: List[ActorRef])

}
