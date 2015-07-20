package com.comcast.csv.common.actors

import akka.actor.{ActorLogging, Actor, ActorRef}
import com.comcast.csv.common.metrics.Instrumented


trait StackableActor extends Actor {

  /** Actor classes should implement this partialFunction for standard
    *  actor message handling
    */
  def wrappedReceive: Receive

  /** Stackable traits should override and call super.receive(x) for
    *  stacking functionality
    */
  def receive: Receive = {
    case x => if (wrappedReceive.isDefinedAt(x)) wrappedReceive(x) else unhandled(x)
  }

}

trait StackableLoggingActor extends StackableActor with ActorLogging {
  override def receive: Receive = {
    case x =>
      log.info(s"====================== received: $x from $sender")
      super.receive(x)
      log.debug(s"processed: $x")
  }
}

trait StackableTimingActor extends StackableActor with Instrumented {
  override def receive: Receive = {
    case x =>
      val name = InstrumentUtil.name(this, x, "timer");
      metrics.timer(name).time {super.receive(x)}
  }
}

trait StackableCountingActor extends StackableActor with Instrumented {
  override def receive: Receive = {
    case x =>
      val name = InstrumentUtil.name(this, x, "counter");
      metrics.counter(name).inc(1)
      super.receive(x)
  }
}


object CollectorProtocol {
  case class CollectFlow(message: Any, parent: ActorRef, node: Actor)
}

trait InstrumentedActor extends StackableCountingActor with StackableTimingActor with StackableLoggingActor {

}

object InstrumentUtil {

  def name(source: Any, message: Any, variance: String) = {
    s"${source.getClass.getSimpleName}-${message.getClass.getSimpleName}-$variance"
  }

}


