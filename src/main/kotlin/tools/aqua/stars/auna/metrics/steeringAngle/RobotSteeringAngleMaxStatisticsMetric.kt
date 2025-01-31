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

package tools.aqua.stars.auna.metrics.steeringAngle

import java.util.logging.Logger
import tools.aqua.stars.core.metric.providers.Loggable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.providers.Stateful
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

/** Metric to calculate the maximum steering angle of a robot. */
class RobotSteeringAngleMaxStatisticsMetric(
    override val loggerIdentifier: String = "robot-steering-angle-maximum-statistics",
    override val logger: Logger = Loggable.getLogger(loggerIdentifier)
) :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>,
    Loggable,
    Stateful {

  private val currentMax: MutableMap<Int, Double> = mutableMapOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val robotIdToRobotStateMap = segment.tickData.map { it.entities }.flatten().groupBy { it.id }

    // Maximum steering angle for robots
    val maximumRobotSteeringAngle =
        robotIdToRobotStateMap.map { it.key to it.value.maxOf { t -> t.steeringAngle } }
    maximumRobotSteeringAngle.forEach {
      currentMax[it.first] =
          maxOf(currentMax.getOrDefault(it.first, Double.NEGATIVE_INFINITY), it.second)
      logFiner(
          "The maximum steering angle of robot with ID '${it.first}' in Segment `${segment.getSegmentIdentifier()}` is ${it.second}.")
    }
  }

  override fun getState(): Map<Int, Double> = currentMax

  override fun printState() {
    currentMax.forEach { (actorId, maxSteeringAngle) ->
      logFine("The maximum steering angle of robot with ID '$actorId' is '$maxSteeringAngle'")
    }
  }
}
