package com.comcast.csv.common.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.Slf4jReporter
import com.comcast.csv.common.logging.Logging

/**
 *
 * @author dbolene
 */
/*
 * encapsulation of metrics
 */
object AppMetrics extends Logging {

  val metricRegistry = new com.codahale.metrics.MetricRegistry()
  val healthCheckRegistry = new com.codahale.metrics.health.HealthCheckRegistry()


  val slf4jMetricsReporter = Slf4jReporter.forRegistry(metricRegistry).convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS).outputTo(loggr)
    .build()

 // val slf4jHealthReporter = Slf4jReporter.forRegistry(healthCheckRegistry).convertRatesTo(TimeUnit.SECONDS)
   // .convertDurationsTo(TimeUnit.MILLISECONDS).outputTo(loggr)
    //.build()

  slf4jMetricsReporter.start(5, TimeUnit.MINUTES)
}
