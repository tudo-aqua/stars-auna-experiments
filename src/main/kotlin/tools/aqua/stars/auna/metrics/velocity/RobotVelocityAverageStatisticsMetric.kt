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

package tools.aqua.stars.auna.metrics.velocity

import java.util.logging.Logger
import tools.aqua.stars.core.metric.providers.Loggable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.providers.Stateful
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

/** Metric to calculate the average velocity of a robot. */
class RobotVelocityAverageStatisticsMetric(
    override val loggerIdentifier: String = "robot-velocity-average-statistics",
    override val logger: Logger = Loggable.getLogger(loggerIdentifier)
) :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>,
    Loggable,
    Stateful {

  private val averageVelocity: MutableMap<Int, Double> = mutableMapOf()
  private val tickCount: MutableMap<Int, Int> = mutableMapOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val robotIdToRobotStateMap = segment.tickData.map { it.entities }.flatten().groupBy { it.id }

    val robotIdToSumOfVelocity =
        robotIdToRobotStateMap.map { it.key to it.value.sumOf { t -> t.velocity } }

    robotIdToSumOfVelocity.forEach {
      logFiner(
          "The average velocity of robot with ID '${it.first}' in Segment $segment is ${it.second / segment.tickData.size}")

      averageVelocity[it.first] = averageVelocity.getOrDefault(it.first, 0.0) + it.second
      tickCount[it.first] = tickCount.getOrDefault(it.first, 0) + segment.tickData.size
    }
  }

  override fun getState(): Map<Int, Double> =
      averageVelocity.map { it.key to it.value / (tickCount[it.key] ?: 0) }.toMap()

  override fun printState() {
    getState().forEach { (actorId, averageVelocity) ->
      logFine("The average velocity of robot with ID '$actorId' is '$averageVelocity'")
    }
  }
}
