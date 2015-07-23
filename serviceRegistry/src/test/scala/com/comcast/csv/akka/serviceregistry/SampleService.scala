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
  var dependentServices = new scala.collection.mutable.HashMap[String, ActorRef]

  override def receive = {

    case initialize: SampleServiceInitialize =>
      log.info(s"Received -> SampleServiceInitialize: $initialize")
      serviceName = Option(initialize.serviceName)
      registry = Option(initialize.registry)
      registry.foreach(r => serviceName.foreach(name => r ! PublishService(serviceName = name, serviceEndpoint = self)))

    case ss: SampleServiceSubscribeTo =>
      log.info(s"Received -> SampleServiceSubscribeTo: $ss")
      registry.foreach(r => r ! SubscribeToService(ss.serviceName))

    case sa: ServiceAvailable =>
      log.info(s"Received -> ServiceAvailable: $sa")
      dependentServices += (sa.serviceName -> sa.serviceEndpoint)

    case sua: ServiceUnAvailable =>
      log.info(s"Received -> ServiceUnAvailable: $sua")
      dependentServices -= sua.serviceName

    case GoOffline =>
      log.info(s"Received -> GoOffline")
      registry.foreach(r => serviceName.foreach(name => r ! UnPublishService(name)))

    case msg =>
      log.warning(s"Received unknown message: $msg")
  }

}

object SampleServiceProtocol {
  case class SampleServiceInitialize(serviceName: String, registry: ActorRef)
  case class SampleServiceSubscribeTo(serviceName: String)
  case object GoOffline
}