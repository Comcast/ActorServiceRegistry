package com.comcast.csv.akka.serviceregistry

import akka.actor._
import SampleServiceProtocol._
import com.comcast.csv.common.protocol.ServiceRegistryProtocol._


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
  var dependentServices = new scala.collection.mutable.HashMap[String, ActorRef]

  override def receive = {

    case initialize: SampleServiceInitialize =>
      serviceName = Option(initialize.serviceName)
      registry = Option(initialize.registry)
      registry.foreach(r => serviceName.foreach(name => r ! PublishService(serviceName = name, serviceEndpoint = self)))

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