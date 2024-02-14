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
import tools.aqua.stars.core.metric.utils.getCSVString
import tools.aqua.stars.core.metric.utils.getPlot
import tools.aqua.stars.core.metric.utils.plotDataAsLineChart
import tools.aqua.stars.core.metric.utils.saveAsCSVFile
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

class RobotVelocityStatisticsMetric(
    override val logger: Logger = Loggable.getLogger("robot-velocity-statistics")
) : SegmentMetricProvider<Robot, TickData, Segment>, Loggable, Plottable {
  private var segmentToRobotIdToRobotStateMap: MutableList<Pair<Segment, Map<Int, List<Robot>>>> =
      mutableListOf()

  override fun evaluate(segment: SegmentType<Robot, TickData, Segment>) {
    val robotIdToRobotStateMap = segment.tickData.map { it.entities }.flatten().groupBy { it.id }
    segmentToRobotIdToRobotStateMap += segment as Segment to robotIdToRobotStateMap

    // Average velocity for robots
    val averageRobotVelocity =
        robotIdToRobotStateMap.map { it.key to it.value.map { it.velocity ?: 0.0 }.average() }
    averageRobotVelocity.forEach {
      logFiner(
          "The average velocity of robot with id '${it.first}' in Segment `${segment.getSegmentIdentifier()}` is ${it.second}.")
    }

    // Minimum velocity for robots
    val minimumRobotVelocity =
        robotIdToRobotStateMap.map { it.key to it.value.minOf { it.velocity ?: 0.0 } }
    minimumRobotVelocity.forEach {
      logFiner(
          "The minimum velocity of robot with id '${it.first}' in Segment `${segment.getSegmentIdentifier()}` is ${it.second}.")
    }

    // Maximum velocity for robots
    val maximumRobotVelocity =
        robotIdToRobotStateMap.map { it.key to it.value.maxOf { it.velocity ?: 0.0 } }
    maximumRobotVelocity.forEach {
      logFiner(
          "The maximum velocity of robot with id '${it.first}' in Segment `${segment.getSegmentIdentifier()}` is ${it.second}.")
    }
  }

  override fun plotData() {
    segmentToRobotIdToRobotStateMap.forEach { segmentToRobotIdToRobotStateMap ->
      val robotIdToRobotStates = segmentToRobotIdToRobotStateMap.second
      val segment = segmentToRobotIdToRobotStateMap.first

      val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
      val folderName = "robot-velocity-statistics"
      val subFolderName = segment.getSegmentIdentifier()

      robotIdToRobotStates.forEach { (robotId, robotStates) ->
        val legendEntry = "Robot $robotId"
        val fileName = "${subFolderName}_robot_$robotId"
        val yValues = robotStates.map { it.velocity ?: 0.0 }
        val xValues = robotStates.map { it.tickData.currentTick }

        combinedValuesMap[legendEntry] = xValues to yValues

        plotDataAsLineChart(
            plot =
                getPlot(
                    legendEntry = legendEntry,
                    xValues = xValues,
                    yValues = yValues,
                    "tick",
                    "velocity (m/s)",
                    "Velocity for"),
            folder = folderName,
            subFolder = subFolderName,
            fileName = fileName)
      }

      plotDataAsLineChart(
          plot = getPlot(combinedValuesMap, "time", "velocity", "Velocity for"),
          folder = folderName,
          subFolder = subFolderName,
          fileName = "${subFolderName}_combined")
    }
  }

  override fun writePlotData() {
    segmentToRobotIdToRobotStateMap.forEach { segmentToRobotIdToRobotStateMap ->
      val robotIdToRobotStates = segmentToRobotIdToRobotStateMap.second
      val segment = segmentToRobotIdToRobotStateMap.first

      val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
      val folderName = "robot-velocity-statistics"
      val subFolderName = segment.getSegmentIdentifier()

      robotIdToRobotStates.forEach { (robotId, robotStates) ->
        val legendEntry = "Robot $robotId"
        val fileName = "${subFolderName}_robot_$robotId"
        val yValues = robotStates.map { it.velocity ?: 0.0 }
        val xValues = robotStates.map { it.tickData.currentTick }

        combinedValuesMap[legendEntry] = xValues to yValues

        saveAsCSVFile(
            csvString =
                getCSVString(columnEntry = legendEntry, xValues = xValues, yValues = yValues),
            folder = folderName,
            subFolder = subFolderName,
            fileName = fileName)
      }

      saveAsCSVFile(
          csvString = getCSVString(combinedValuesMap),
          folder = folderName,
          subFolder = subFolderName,
          fileName = "${subFolderName}_combined")
    }
  }
}
