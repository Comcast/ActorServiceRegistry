package com.comcast.csv.common.metrics

import nl.grons.metrics.scala.{ReceiveCounterActor, ReceiveExceptionMeterActor, ReceiveTimerActor}

/**
 *
 * @author dbolene
 */
trait InstrumentedActorMetrics extends Instrumented  with ReceiveCounterActor
  with ReceiveTimerActor with ReceiveExceptionMeterActor{

}
