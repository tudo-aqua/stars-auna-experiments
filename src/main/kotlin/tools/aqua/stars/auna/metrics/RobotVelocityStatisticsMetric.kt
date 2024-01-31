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

package tools.aqua.stars.auna.metrics

import java.util.logging.Logger
import tools.aqua.stars.core.metric.providers.Loggable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

class RobotVelocityStatisticsMetric(
    override val logger: Logger = Loggable.getLogger("robot-velocity-statistics")
) : SegmentMetricProvider<Robot, TickData, Segment>, Loggable {
  override fun evaluate(segment: SegmentType<Robot, TickData, Segment>) {
    // Average velocity for robots
    val averageRobotVelocity =
        segment.tickData
            .map { it.entities }
            .flatten()
            .groupBy { it.id }
            .map { it.key to (it.value.mapNotNull { it.velocity }).average() }
    averageRobotVelocity.forEach {
      logFiner("The average velocity of robot with id '$it.first' is ${it.second}.")
    }

    // Minimum velocity for robots
    val minimumRobotVelocity =
        segment.tickData
            .map { it.entities }
            .flatten()
            .groupBy { it.id }
            .map { it.key to (it.value.mapNotNull { it.velocity }).min() }
    minimumRobotVelocity.forEach {
      logFiner("The minimum velocity of robot with id '$it.first' is ${it.second}.")
    }

    // Maximum velocity for robots
    val maximumRobotVelocity =
        segment.tickData
            .map { it.entities }
            .flatten()
            .groupBy { it.id }
            .map { it.key to (it.value.mapNotNull { it.velocity }).max() }
    maximumRobotVelocity.forEach {
      logFiner("The maximum velocity of robot with id '$it.first' is ${it.second}.")
    }
  }
}
