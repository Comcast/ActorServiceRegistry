package com.comcast.csv.akka.serviceregistry

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberRemoved}
import com.comcast.csv.common.actors.InstrumentedActor
import com.comcast.csv.common.protocol.ServiceRegistryProtocol._

import scala.collection.mutable

/**
 * Companion of ServiceRegistry.
 */
object ServiceRegistry {
  def props = Props[ServiceRegistry]
}

/**
 * Service registry for Actor implemented service endpoints.
 *
 * @author dbolene
 */
class ServiceRegistry extends InstrumentedActor {

  // Map[subscriber,Set[subscribedTo]]
  val subscribers = scala.collection.mutable.HashMap.empty[ActorRef, mutable.HashSet[String]]
  // Map[published,publisher]
  val publishers = scala.collection.mutable.HashMap.empty[String, ActorRef]
  // Map[published,nodeAddress]
  val publishedVsNodeAddress = scala.collection.mutable.HashMap.empty[String, Address]

  val cluster = Cluster(context.system)
  cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberRemoved])

  log.info(s"=================== ServiceRegistry created")

  override def wrappedReceive = {

    case ps: PublishService =>
      publishers += (ps.serviceName -> ps.serviceEndpoint)
      publishedVsNodeAddress += (ps.serviceName -> ps.nodeAddress)
      subscribers.filter(p => p._2.contains(ps.serviceName))
        .foreach(p => p._1 ! ServiceAvailable(ps.serviceName, ps.serviceEndpoint))

    case ups: UnPublishService =>
      publishers -= ups.serviceName
      publishedVsNodeAddress -= ups.serviceName
      subscribers.filter(p => p._2.contains(ups.serviceName))
        .foreach(p => p._1 ! ServiceUnAvailable(ups.serviceName))

    case ss: SubscribeToService =>
      subscribers += (sender() -> subscribers.get(sender())
        .orElse(Some(new mutable.HashSet[String])).map(s => {
        s + ss.serviceName
      })
        .getOrElse(new mutable.HashSet[String]))
      publishers.filter(p => p._1 == ss.serviceName)
        .foreach(p => sender() ! ServiceAvailable(ss.serviceName, p._2))

    case us: UnSubscribeToService =>
      subscribers += (sender() -> subscribers.get(sender())
        .orElse(Some(new mutable.HashSet[String])).map(s => {
        s - us.serviceName
      })
        .getOrElse(new mutable.HashSet[String]))

    case mr: MemberRemoved =>
      publishedVsNodeAddress.filter(p1 => p1._2 == mr.member.address).foreach(p2 => {
        subscribers.filter(p3 => p3._2.contains(p2._1))
          .foreach(p4 => p4._1 ! ServiceUnAvailable(p2._1))
        publishedVsNodeAddress -= p2._1
      })

    case msg =>
      log.info(s"received unknown message: $msg")
  }
}

/**
 * Private ServiceRegistry messages.
 */
object ServiceRegistryInternalProtocol {
  case object End
}
