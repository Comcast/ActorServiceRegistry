package com.comcast.csv.common.metrics

import nl.grons.metrics.scala.CheckedBuilder

/**
 *
 * @author dbolene
 */
trait HealthChecked extends Instrumented with CheckedBuilder {

  val healthCheckRegistry = AppMetrics.healthCheckRegistry


}
