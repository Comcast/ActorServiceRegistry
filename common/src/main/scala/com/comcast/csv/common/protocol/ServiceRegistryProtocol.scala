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
  case class UnPublishService(serviceName: String)
  /**
   * Service client sends to ServiceRegistry when requiring dependent service.
   */
  case class SubscribeToService(serviceName: String)
  /**
   * Service client sends to ServiceRegistry when no longer requiring dependent service.
   */
  case class UnSubscribeToService(serviceName: String)
  /**
   * ServiceRegistry sends to service client when subscribed to service is now online.
   */
  case class ServiceAvailable(serviceName: String, serviceEndpoint: ActorRef)
  /**
   * ServiceRegistry sends to service client when subscribed to service is now offline.
   */
  case class ServiceUnAvailable(serviceName: String)
  /**
   * ServiceRegistry sends to publishers and subscribers when ServiceRegistry has been restarted requiring all participants to re-subscribe and re-publish.
   */
  case class RegistryHasRestarted(registry: ActorRef)

}
