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

/** Metric to calculate the minimum distance to the front robot in a segment. */
class RobotDistanceToFrontMinStatisticsMetric(
    override val logger: Logger = Loggable.getLogger("robot-distance-to-front-minimum-statistics")
) :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>,
    Loggable,
    Stateful {

  private val currentMin: MutableMap<Int, Double> = mutableMapOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val primaryEntityId = segment.primaryEntityId

    if (primaryEntityId == 1) {
      currentMin[primaryEntityId] = Double.POSITIVE_INFINITY
      return
    }

    val frontEntityId = primaryEntityId - 1

    val minOfDistanceToFrontForPrimaryEntityInSegment =
        segment.tickData.minOf { currentTick ->
          val primaryEntity = checkNotNull(currentTick.getEntityById(primaryEntityId))
          val frontEntity = checkNotNull(currentTick.getEntityById(frontEntityId))

          primaryEntity.distanceToOther(frontEntity)
        }

    logFiner(
        "The minimum distance between entity $primaryEntityId and $frontEntityId in Segment $segment is $minOfDistanceToFrontForPrimaryEntityInSegment")

    currentMin[primaryEntityId] =
        minOf(
            currentMin.getOrDefault(primaryEntityId, 0.0),
            minOfDistanceToFrontForPrimaryEntityInSegment)
  }

  override fun getState(): Map<Int, Double> = currentMin

  override fun printState() {
    currentMin.forEach { (actorId, minDistanceToFront) ->
      logFine(
          "The minimum distance of robot with ID '$actorId' to its front robot is '$minDistanceToFront'")
    }
  }
}
