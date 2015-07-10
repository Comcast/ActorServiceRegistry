package com.comcast.csv.akka.serviceregistry

import akka.actor.{AddressFromURIString, ActorRef, Props}
import SampleServiceProtocol._
import com.comcast.csv.common.actors.{StackableCountingActor, StackableLoggingActor, StackableTimingActor}
import com.comcast.csv.common.protocol.ServiceRegistryProtocol._


object SampleService {

  def props = Props[SampleService]

}

/**
 * Sample Service for testing ServiceRegistry.
 *
 * @author dbolene
 */
class SampleService extends StackableTimingActor with StackableCountingActor with StackableLoggingActor {

  var serviceName: Option[String] = None
  var registry: Option[ActorRef] = None
  var dependentServices = new scala.collection.mutable.HashMap[String, ActorRef]

  override def wrappedReceive = {

    case initialize: SampleServiceInitialize =>
      serviceName = Option(initialize.serviceName)
      registry = Option(initialize.registry)
      val address = AddressFromURIString("akka.tcp://actorSystemName@10.0.0.1:2552/user/actorName")
      registry.foreach(r => serviceName.foreach(name => r ! PublishService(serviceName = name, serviceEndpoint = self, nodeAddress = address)))

    case ss: SampleServiceSubscribeTo =>
      registry.foreach(r => r ! SubscribeToService(ss.serviceName))

    case sa: ServiceAvailable =>
      dependentServices += (sa.serviceName -> sa.serviceEndpoint)

    case sua: ServiceUnAvailable =>
      dependentServices -= sua.serviceName

    case GoOffline =>
      registry.foreach(r => serviceName.foreach(name => r ! UnPublishService(name)))
  }

}

object SampleServiceProtocol {
  case class SampleServiceInitialize(serviceName: String, registry: ActorRef)
  case class SampleServiceSubscribeTo(serviceName: String)
  case object GoOffline
}