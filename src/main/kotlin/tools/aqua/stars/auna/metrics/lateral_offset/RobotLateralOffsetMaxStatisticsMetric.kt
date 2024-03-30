/*
 * Copyright 2024 The STARS AuNa Experiments Authors
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

package tools.aqua.stars.auna.metrics.lateral_offset

import java.util.logging.Logger
import tools.aqua.stars.core.metric.providers.Loggable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.providers.Stateful
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

class RobotLateralOffsetMaxStatisticsMetric(
    override val logger: Logger = Loggable.getLogger("robot-lateral-offset-maximum-statistics")
) :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>,
    Loggable,
    Stateful {

  private var currentMax: MutableMap<Int, Double> = mutableMapOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val robotIdToRobotStateMap = segment.tickData.map { it.entities }.flatten().groupBy { it.id }

    val maximumRobotLateralOffset =
        robotIdToRobotStateMap.map { it.key to it.value.maxOf { t -> t.lateralOffset } }
    maximumRobotLateralOffset.forEach {
      currentMax[it.first] =
          maxOf(currentMax.getOrDefault(it.first, Double.NEGATIVE_INFINITY), it.second)
      logFiner(
          "The maximum lateral offset of robot with id '${it.first}' in Segment `${segment.getSegmentIdentifier()}` is ${it.second}.")
    }
  }

  override fun getState(): Map<Int, Double> {
    return currentMax
  }

  override fun printState() {
    currentMax.forEach { (actorId, maxVelocity) ->
      logFine("The maximum lateral offset of robot with ID '$actorId' is '$maxVelocity'")
    }
  }
}
