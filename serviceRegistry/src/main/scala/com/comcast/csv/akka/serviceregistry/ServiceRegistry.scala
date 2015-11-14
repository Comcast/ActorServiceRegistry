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
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotSuccess, SnapshotOffer}
import com.comcast.csv.akka.serviceregistry.ServiceRegistryInternalProtocol.End
import com.comcast.csv.common.protocol.ServiceRegistryProtocol._
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * Companion of ServiceRegistry.
 */
object ServiceRegistry {

  def props = Props[ServiceRegistry]

  val identity = "serviceRegistry"
}

/**
 * Service registry for Actor implemented service endpoints.
 *
 * @author dbolene
 */
class ServiceRegistry extends PersistentActor {

  val log = LoggerFactory.getLogger(this.getClass)

  // [aSubscriberOrPublisher]
  val subscribersPublishers = mutable.Set.empty[ActorRef]
  // Map[subscriber,Set[subscribedTo]]
  val subscribers = mutable.HashMap.empty[ActorRef, mutable.HashSet[String]]
  // Map[published,publisher]
  val publishers = mutable.HashMap.empty[String, List[ActorRef]]


  override val persistenceId: String = ServiceRegistry.identity

  def recordSubscriberPublisher(subpub: AddSubscriberPublisher): Unit = {
    subscribersPublishers += subpub.subscriberPublisher
  }

  def considerRememberParticipant(participant: ActorRef): Unit = {
    if (!subscribersPublishers.contains(participant)) {
      val add = AddSubscriberPublisher(participant)
      persist(add)(recordSubscriberPublisher)
    }
  }

  def unrecordSubscriberPublisher(subpub: RemoveSubscriberPublisher): Unit = {
    subscribersPublishers -= subpub.subscriberPublisher
  }

  def considerForgetParticipant(participant: ActorRef): Unit = {

    def isSubscriberPublisherStillInUse(subpub: ActorRef): Boolean = {
      subscribers.contains(subpub) ||
        publishers.exists { case (serviceName, endPoint) => endPoint.contains(subpub) }
    }

    if (subscribersPublishers.contains(participant) && !isSubscriberPublisherStillInUse(participant)) {
      val remove = RemoveSubscriberPublisher(participant)
      persist(remove)(unrecordSubscriberPublisher)
    }
  }

  override def receiveRecover: Receive = {
    case add: AddSubscriberPublisher =>
      log.info(s"Received -> AddSubscriberPublisher: $add")
      recordSubscriberPublisher(add)

    case remove: RemoveSubscriberPublisher =>
      log.info(s"Received -> RemoveSubscriberPublisher: $remove")
      unrecordSubscriberPublisher(remove)

    case SnapshotOffer(_, snapshot: SnapshotAfterRecover) =>
      log.info(s"Received -> SnapshotOffer")
    // do nothing

    case RecoveryCompleted =>
      log.info(s"Received -> RecoveryCompleted")
      val registryHasRestarted = RegistryHasRestarted(self)
      subscribersPublishers.foreach(sp => sp ! registryHasRestarted)
      subscribersPublishers.clear()
      saveSnapshot(SnapshotAfterRecover())
  }

  override def receiveCommand: Receive = {


    case PublishService(serviceName, serviceEndpoint) =>
      log.info(s"Received -> PublishService($serviceName, ${serviceEndpoint.path}) from ${sender().path}")

      // Update the list of endpoints at this serviceName
      val newPublishers = serviceEndpoint :: publishers.getOrElse(serviceName, List())
      publishers += (serviceName -> newPublishers)

      // Tell all the subscribers with this serviceName that it changed
      subscribers.filter(p => p._2.contains(serviceName))
        .foreach(p => p._1 ! ServiceChanged(serviceName, newPublishers))

      // Watch the service so that we can react to failures
      context.watch(serviceEndpoint)

      // persist the endpoint so that we can recover
      considerRememberParticipant(serviceEndpoint)


    case UnPublishService(serviceName, serviceEndpoint) =>
      log.info(s"Received -> UnPublishService($serviceName, ${serviceEndpoint.path}) from ${sender().path}")

      val existingEndpoints = publishers.get(serviceName)

      existingEndpoints foreach {
        endpoints =>

          log.debug(s"Found endpoints=$endpoints")
          // Make out the new endpoints and verify that we have this publisher
          val newEndpoints: List[ActorRef] = endpoints.filterNot(actor => actor == serviceEndpoint)
          val publisherToRemove: Option[ActorRef] = endpoints.find(actor => actor == serviceEndpoint)

          if (newEndpoints != endpoints) {

            log.debug(s"New endpoints=$newEndpoints")

            publisherToRemove foreach {
              actor =>

                // Tell the subscribers with this service that the service changed
                for (subscriber <- subscribers) {
                  if (subscriber._2.contains(serviceName)) {
                    log.debug(s"notifying subscriber=${subscriber._1.path} of endpoint changes.")
                    subscriber._1 ! ServiceChanged(serviceName, newEndpoints)
                  }
                }

            }

            // Update the endpoints
            publishers += (serviceName -> newEndpoints)

          }
      }

      // Forget this publisher since it's not in service any more
      considerForgetParticipant(serviceEndpoint)


    case ss: SubscribeToService =>
      log.info(s"Received -> SubscribeToService(${ss.serviceName}) from ${sender().path}")

      // Add this guy to the subscribers
      subscribers += (sender() -> subscribers.get(sender())
        .orElse(Some(new mutable.HashSet[String])).map(s => {
        s + ss.serviceName
      })
        .getOrElse(new mutable.HashSet[String]))

      // Find all the publishers of this service
      // Then tell the subscribers about the service endpoints
      publishers.filter(p => p._1 == ss.serviceName)
        .foreach(p => sender() ! ServiceChanged(ss.serviceName, p._2))

      // Persist this subscriber so that we can recover
      considerRememberParticipant(sender())


    case us: UnSubscribeToService =>
      log.info(s"Received -> UnSubscribeToService(${us.serviceName}) from ${sender().path}")

      // Remove this subscriber
      subscribers += (sender() -> subscribers.get(sender())
        .orElse(Some(new mutable.HashSet[String])).map(s => {
        s - us.serviceName
      })
        .getOrElse(new mutable.HashSet[String]))

      // persist the change
      considerForgetParticipant(sender())

    case terminated: Terminated =>
      log.info(s"Received -> Terminated(${terminated.getActor.path}) from ${sender().path}")

      val terminatedActor = terminated.getActor()


      // Un-publish the terminated service
      for(publisher <- publishers
          if publisher._2.contains(terminatedActor)) {
            self forward UnPublishService(publisher._1, terminatedActor)
          }

    case sss: SaveSnapshotSuccess =>
      log.info(s"Received -> SaveSnapshotSuccess: $sss")

    case End =>
      log.info(s"Received -> End")

    case msg =>
      log.warn(s"Received unknown message: $msg")
  }
}

/**
 * Private ServiceRegistry messages.
 */
object ServiceRegistryInternalProtocol {
  case object End
}

case class AddSubscriberPublisher(subscriberPublisher: ActorRef)
case class RemoveSubscriberPublisher(subscriberPublisher: ActorRef)
case class SnapshotAfterRecover()
