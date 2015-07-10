package com.comcast.csv.common.metrics

import nl.grons.metrics.scala.InstrumentedBuilder

/**
 *
 * @author dbolene
 */
/*
 * Extend this trait to have metricRegisty brought into scope
 * like this:
 * import _root_.metrics.Instrumented
 */
trait Instrumented extends InstrumentedBuilder{

  val metricRegistry = AppMetrics.metricRegistry

}
