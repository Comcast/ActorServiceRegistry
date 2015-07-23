package com.comcast.csv.akka.serviceregistry

import akka.actor._
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotSuccess, SnapshotOffer}
import com.comcast.csv.akka.serviceregistry.ServiceRegistryInternalProtocol.End
import com.comcast.csv.common.protocol.ServiceRegistryProtocol._

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
class ServiceRegistry extends PersistentActor with ActorLogging {

  // [aSubscriberOrPublisher]
  val subscribersPublishers = scala.collection.mutable.Set.empty[ActorRef]
  // Map[subscriber,Set[subscribedTo]]
  val subscribers = scala.collection.mutable.HashMap.empty[ActorRef, mutable.HashSet[String]]
  // Map[published,publisher]
  val publishers = scala.collection.mutable.HashMap.empty[String, ActorRef]

  log.info(s"=================== ServiceRegistry created ===================")

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
        publishers.exists { case (serviceName, endPoint) => endPoint == subpub }
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

    case ps: PublishService =>
      log.info(s"Received -> PublishService: $ps")
      publishers += (ps.serviceName -> ps.serviceEndpoint)
      subscribers.filter(p => p._2.contains(ps.serviceName))
        .foreach(p => p._1 ! ServiceAvailable(ps.serviceName, ps.serviceEndpoint))
      context.watch(ps.serviceEndpoint)
      considerRememberParticipant(ps.serviceEndpoint)

    case ups: UnPublishService =>
      log.info(s"Received -> UnPublishService: $ups")
      val serviceEndpoint = publishers.get(ups.serviceName)
      publishers.remove(ups.serviceName)
      subscribers.filter(p => p._2.contains(ups.serviceName))
        .foreach(p => p._1 ! ServiceUnAvailable(ups.serviceName))
      serviceEndpoint.foreach(ep => considerForgetParticipant(ep))

    case ss: SubscribeToService =>
      log.info(s"Received -> SubscribeToService: $ss")
      subscribers += (sender() -> subscribers.get(sender())
        .orElse(Some(new mutable.HashSet[String])).map(s => {
        s + ss.serviceName
      })
        .getOrElse(new mutable.HashSet[String]))
      publishers.filter(p => p._1 == ss.serviceName)
        .foreach(p => sender() ! ServiceAvailable(ss.serviceName, p._2))
      considerRememberParticipant(sender())

    case us: UnSubscribeToService =>
      log.info(s"Received -> UnSubscribeToService: $us")
      subscribers += (sender() -> subscribers.get(sender())
        .orElse(Some(new mutable.HashSet[String])).map(s => {
        s - us.serviceName
      })
        .getOrElse(new mutable.HashSet[String]))
      considerForgetParticipant(sender())

    case terminated: Terminated =>
      log.info(s"Received -> Terminated: $terminated")
      publishers.find(p => p._2 == terminated.getActor).foreach(p2 => {
        subscribers.filter(p3 => p3._2.contains(p2._1))
          .foreach(p4 => p4._1 ! ServiceUnAvailable(p2._1))
      })

    case sss: SaveSnapshotSuccess =>
      log.info(s"Received -> SaveSnapshotSuccess: $sss")

    case End =>
      log.info(s"Received -> End")

    case msg =>
      log.warning(s"Received unknown message: $msg")
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
