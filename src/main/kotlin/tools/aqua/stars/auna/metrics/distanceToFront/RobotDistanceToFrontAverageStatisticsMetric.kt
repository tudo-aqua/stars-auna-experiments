/*
 * Copyright 2024-2025 The STARS AuNa Experiments Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tools.aqua.stars.auna.metrics.distanceToFront

import java.util.logging.Logger
import tools.aqua.stars.core.metric.providers.Loggable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.providers.Stateful
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

/** Metric to calculate the average distance to the front robot in a segment. */
class RobotDistanceToFrontAverageStatisticsMetric(
    override val loggerIdentifier: String = "robot-distance-to-front-average-statistics",
    override val logger: Logger = Loggable.getLogger(loggerIdentifier)
) :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>,
    Loggable,
    Stateful {

  private val averageDistanceToFront: MutableMap<Int, Double> = mutableMapOf()
  private val tickCount: MutableMap<Int, Int> = mutableMapOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val primaryEntityId = segment.primaryEntityId

    if (primaryEntityId == 1) {
      averageDistanceToFront[primaryEntityId] = Double.POSITIVE_INFINITY
      tickCount[primaryEntityId] = 1
      return
    }

    val frontEntityId = primaryEntityId - 1

    val sumOfDistanceToFrontForPrimaryEntityInSegment =
        segment.tickData.sumOf { currentTick ->
          val primaryEntity = checkNotNull(currentTick.getEntityById(primaryEntityId))
          val frontEntity = checkNotNull(currentTick.getEntityById(frontEntityId))

          primaryEntity.distanceToOther(frontEntity)
        }

    val averageDistanceToFrontForPrimaryEntityInSegment =
        sumOfDistanceToFrontForPrimaryEntityInSegment / segment.tickData.size

    logFiner(
        "The average distance between entity with ID '$primaryEntityId' and '$frontEntityId' in Segment $segment is $averageDistanceToFrontForPrimaryEntityInSegment")

    averageDistanceToFront[primaryEntityId] =
        averageDistanceToFront.getOrDefault(primaryEntityId, 0.0) +
            sumOfDistanceToFrontForPrimaryEntityInSegment

    tickCount[primaryEntityId] = tickCount.getOrDefault(primaryEntityId, 0) + segment.tickData.size
  }

  override fun getState(): Map<Int, Double> =
      averageDistanceToFront.map { it.key to it.value / (tickCount[it.key] ?: 0) }.toMap()

  override fun printState() {
    getState().forEach { (actorId, averageVelocity) ->
      logFine("The average acceleration of robot with ID '$actorId' is '$averageVelocity'")
    }
  }
}
