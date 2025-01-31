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

package tools.aqua.stars.auna.metrics.acceleration

import java.util.logging.Logger
import tools.aqua.stars.core.metric.providers.Loggable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.providers.Stateful
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

/** Metric to calculate the average acceleration of a robot in a segment. */
class RobotAccelerationAverageStatisticsMetric(
    override val loggerIdentifier: String = "robot-acceleration-average-statistics",
    override val logger: Logger = Loggable.getLogger(loggerIdentifier)
) :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>,
    Loggable,
    Stateful {

  private val averageAcceleration: MutableMap<Int, Double> = mutableMapOf()
  private val tickCount: MutableMap<Int, Int> = mutableMapOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val robotIdToRobotStateMap = segment.tickData.map { it.entities }.flatten().groupBy { it.id }

    val robotIdToSumOfAcceleration =
        robotIdToRobotStateMap.map { it.key to it.value.sumOf { t -> t.acceleration } }

    robotIdToSumOfAcceleration.forEach {
      logFiner(
          "The average acceleration of robot with ID '${it.first}' in Segment $segment is ${it.second / segment.tickData.size}")

      averageAcceleration[it.first] = averageAcceleration.getOrDefault(it.first, 0.0) + it.second
      tickCount[it.first] = tickCount.getOrDefault(it.first, 0) + segment.tickData.size
    }
  }

  override fun getState(): Map<Int, Double> =
      averageAcceleration.map { it.key to it.value / (tickCount[it.key] ?: 0) }.toMap()

  override fun printState() {
    getState().forEach { (actorId, averageVelocity) ->
      logFine("The average acceleration of robot with ID '$actorId' is '$averageVelocity'")
    }
  }
}
