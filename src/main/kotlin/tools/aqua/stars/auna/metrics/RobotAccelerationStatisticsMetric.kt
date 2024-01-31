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
import tools.aqua.stars.core.metric.providers.Plottable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.utils.getPlot
import tools.aqua.stars.core.metric.utils.plotDataAsLineChart
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

class RobotAccelerationStatisticsMetric(
    override val logger: Logger = Loggable.getLogger("robot-acceleration-statistics")
) : SegmentMetricProvider<Robot, TickData, Segment>, Loggable, Plottable {
  var segmentToRobotIdToRobotStateMap: MutableList<Pair<Segment, Map<Int, List<Robot>>>> =
      mutableListOf()

  override fun evaluate(segment: SegmentType<Robot, TickData, Segment>) {
    val robotIdToRobotStateMap = segment.tickData.map { it.entities }.flatten().groupBy { it.id }
    segmentToRobotIdToRobotStateMap += segment as Segment to robotIdToRobotStateMap

    // Average acceleration for robots
    val averageRobotAcceleration =
        robotIdToRobotStateMap.map { it.key to it.value.mapNotNull { it.acceleration }.average() }
    averageRobotAcceleration.forEach {
      logFiner("The average acceleration of robot with id '$it.first' is ${it.second}.")
    }

    // Minimum acceleration for robots
    val minimumRobotAcceleration =
        robotIdToRobotStateMap.map { it.key to it.value.mapNotNull { it.acceleration }.min() }
    minimumRobotAcceleration.forEach {
      logFiner("The minimum acceleration of robot with id '$it.first' is ${it.second}.")
    }

    // Maximum acceleration for robots
    val maximumRobotAcceleration =
        robotIdToRobotStateMap.map { it.key to it.value.mapNotNull { it.acceleration }.max() }
    maximumRobotAcceleration.forEach {
      logFiner("The maximum acceleration of robot with id '$it.first' is ${it.second}.")
    }
  }

  override fun plotData() {
    segmentToRobotIdToRobotStateMap.forEach { segmentToRobotIdToRobotStateMap ->
      val robotIdToRobotStates = segmentToRobotIdToRobotStateMap.second
      val segment = segmentToRobotIdToRobotStateMap.first
      robotIdToRobotStates.forEach { (robotId, robotStates) ->
        val legendEntry = "Robot $robotId for Segment ${segment.getSegmentIdentifier()}"
        val folderName = "robot-acceleration-statistics_${segment.getSegmentIdentifier()}"
        val fileName = "${folderName}_robot_$robotId"
        val yValues = robotStates.mapNotNull { it.acceleration }

        val maxTimeStamp = robotStates.maxOf { it.tickData.currentTick }

        plotDataAsLineChart(
            plot = getPlot(legendEntry, yValues, "x", "y", "Acceleration for"),
            yAxisScaleMaxValue = maxTimeStamp,
            folder = folderName,
            fileName = fileName)
      }
    }
  }
}
