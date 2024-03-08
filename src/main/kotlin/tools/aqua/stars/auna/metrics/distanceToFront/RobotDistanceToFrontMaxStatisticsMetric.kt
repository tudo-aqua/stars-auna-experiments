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

package tools.aqua.stars.auna.metrics.distanceToFront

import java.util.logging.Logger
import tools.aqua.stars.core.metric.providers.Loggable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.providers.Stateful
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

class RobotDistanceToFrontMaxStatisticsMetric(
    override val logger: Logger = Loggable.getLogger("robot-distance-to-front-maximum-statistics")
) :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>,
    Loggable,
    Stateful {

  private var currentMax: MutableMap<Int, Double> = mutableMapOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val primaryEntityId = segment.primaryEntityId

    if (primaryEntityId == 1) {
      currentMax[primaryEntityId] = Double.POSITIVE_INFINITY
      return
    }

    val frontEntityId = primaryEntityId - 1

    val maxOfDistanceToFrontForPrimaryEntityInSegment =
        segment.tickData.maxOf { currentTick ->
          val primaryEntity = checkNotNull(currentTick.getEntityById(primaryEntityId))
          val frontEntity = checkNotNull(currentTick.getEntityById(frontEntityId))

          primaryEntity.distanceToOther(frontEntity)
        }

    logFiner(
        "The maximum distance between entity $primaryEntityId and $frontEntityId in Segment $segment is $maxOfDistanceToFrontForPrimaryEntityInSegment")

    currentMax[primaryEntityId] =
        maxOf(
            currentMax.getOrDefault(primaryEntityId, 0.0),
            maxOfDistanceToFrontForPrimaryEntityInSegment)
  }

  override fun getState(): Map<Int, Double> = currentMax

  override fun printState() {
    currentMax.forEach { (actorId, maxDistanceToFront) ->
      logFine(
          "The maximum distance of robot with ID '$actorId' to its front robot is '$maxDistanceToFront'")
    }
  }
}
