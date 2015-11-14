package com.comcast.csv.common

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{RoundRobinRoutingLogic, Router}
import com.comcast.csv.common.protocol.ServiceProtocol.ServiceNotOnline
import com.comcast.csv.common.protocol.ServiceRegistryProtocol.{ServiceChanged, SubscribeToService, UnSubscribeToService}
import org.slf4j.LoggerFactory


object ServiceRegistrar {
  def props(serviceName: String, serviceRegistry: ActorRef): Props =
    Props(new ServiceRegistrar(serviceName, serviceRegistry))
}

class ServiceRegistrar(serviceName: String, serviceRegistry: ActorRef) extends Actor {

  val log = LoggerFactory.getLogger(this.getClass)

  var router = Router(RoundRobinRoutingLogic())

  serviceRegistry ! SubscribeToService(serviceName)

  def receive = {

    case serviceChanged: ServiceChanged =>
      log.info(s"Received ServiceChanged(${serviceChanged.serviceName}, ${serviceChanged.serviceEndpoint}) from $sender")

      router = serviceChanged.serviceEndpoint.foldLeft(Router(RoundRobinRoutingLogic())) {
        (newRouter, endPoint) =>
          newRouter.addRoutee(endPoint)
        }


    case x =>
      if (router.routees.size > 0) {
        router.route(x, sender())
      } else {
        sender() ! ServiceNotOnline(serviceName)
      }
  }

  override def postStop(): Unit = {
    serviceRegistry ! UnSubscribeToService(serviceName)
    super.postStop()
  }
}
