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

import tools.aqua.stars.core.metric.providers.Plottable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.utils.getCSVString
import tools.aqua.stars.core.metric.utils.getPlot
import tools.aqua.stars.core.metric.utils.plotDataAsLineChart
import tools.aqua.stars.core.metric.utils.saveAsCSVFile
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

class RobotLateralOffsetStatisticsMetric :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>, Plottable {
  private var segmentToRobotIdToRobotStateMap: MutableList<Pair<Segment, Map<Int, List<Robot>>>> =
      mutableListOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val robotIdToRobotStateMap = segment.tickData.map { it.entities }.flatten().groupBy { it.id }
    segmentToRobotIdToRobotStateMap += segment as Segment to robotIdToRobotStateMap
  }

  override fun plotData() {
    segmentToRobotIdToRobotStateMap.forEachIndexed { index, segmentToRobotIdToRobotState ->
      print(
          "\rWriting Plots for Robot lateral offset: ${index+1}/${segmentToRobotIdToRobotStateMap.size} (${(index+1) * 100 / segmentToRobotIdToRobotStateMap.size}%)")

      val robotIdToRobotStates = segmentToRobotIdToRobotState.second
      val segment = segmentToRobotIdToRobotState.first

      val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
      val folderName = "lateral-offset-statistics"
      val subFolderName = segment.getSegmentIdentifier()

      robotIdToRobotStates.forEach { (robotId, robotStates) ->
        val legendEntry = "Robot $robotId"
        val fileName = "${subFolderName}_robot_$robotId"
        val yValues = robotStates.map { it.lateralOffset ?: 0.0 }
        val xValues = robotStates.map { it.tickData.currentTick.toSeconds() }

        combinedValuesMap[legendEntry] = xValues to yValues

        plotDataAsLineChart(
            plot =
                getPlot(
                    legendEntry = legendEntry,
                    xValues = xValues,
                    yValues = yValues,
                    "tick",
                    "lateral offset",
                    "lateral offset for"),
            folder = folderName,
            subFolder = subFolderName,
            fileName = fileName)
      }

      plotDataAsLineChart(
          plot = getPlot(combinedValuesMap, "time", "lateral offset", "lateral offset for"),
          folder = folderName,
          subFolder = subFolderName,
          fileName = "${subFolderName}_combined")
    }
    println()
  }

  override fun writePlotData() {
    segmentToRobotIdToRobotStateMap.forEachIndexed { index, segmentToRobotIdToRobotState ->
      print(
          "\rWriting CSV for Robot lateral offset: ${index+1}/${segmentToRobotIdToRobotStateMap.size} (${(index+1) * 100 / segmentToRobotIdToRobotStateMap.size}%)")

      val robotIdToRobotStates = segmentToRobotIdToRobotState.second
      val segment = segmentToRobotIdToRobotState.first

      val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
      val folderName = "lateral-offset-statistics"
      val subFolderName = segment.getSegmentIdentifier()

      robotIdToRobotStates.forEach { (robotId, robotStates) ->
        val legendEntry = "Robot $robotId"
        val fileName = "${subFolderName}_robot_$robotId"
        val yValues = robotStates.map { it.lateralOffset ?: 0.0 }
        val xValues = robotStates.map { it.tickData.currentTick.toSeconds() }

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
    println()
  }
}
