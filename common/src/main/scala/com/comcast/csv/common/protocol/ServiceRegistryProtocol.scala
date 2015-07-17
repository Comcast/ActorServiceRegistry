package com.comcast.csv.common.protocol

import akka.actor.{Address, ActorRef}

/**
 * Protocol for interacting with the Service Registry.
 *
 * @author dbolene
 */
object ServiceRegistryProtocol {

  case class PublishService(serviceName: String, serviceEndpoint: ActorRef, nodeAddress: Address)
  case class UnPublishService(serviceName: String)

  case class SubscribeToService(serviceName: String)
  case class UnSubscribeToService(serviceName: String)

  case class ServiceAvailable(serviceName: String, serviceEndpoint: ActorRef)
  case class ServiceUnAvailable(serviceName: String)

  case class RegistryHasRestarted(registry: ActorRef)

}
