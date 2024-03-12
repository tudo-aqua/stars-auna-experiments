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

import tools.aqua.stars.core.metric.providers.Plottable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.utils.getCSVString
import tools.aqua.stars.core.metric.utils.getPlot
import tools.aqua.stars.core.metric.utils.plotDataAsLineChart
import tools.aqua.stars.core.metric.utils.saveAsCSVFile
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

class RobotDistanceToFrontStatisticsMetric :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>, Plottable {
  var robotIdToDistanceAtTickMap: MutableMap<Int, List<Pair<TickData, Double>>> = mutableMapOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val primaryEntityId = segment.primaryEntityId

    if (primaryEntityId == 1) {
      return
    }

    val frontEntityId = primaryEntityId - 1

    val distanceToFrontForPrimaryEntityInSegment =
        segment.tickData.map { currentTick ->
          val primaryEntity = checkNotNull(currentTick.getEntityById(primaryEntityId))
          val frontEntity = checkNotNull(currentTick.getEntityById(frontEntityId))

          currentTick to primaryEntity.distanceToOther(frontEntity)
        }

    robotIdToDistanceAtTickMap[primaryEntityId] =
        robotIdToDistanceAtTickMap.getOrDefault(primaryEntityId, listOf()) +
            distanceToFrontForPrimaryEntityInSegment
  }

  override fun writePlots() {
    robotIdToDistanceAtTickMap.forEach { segmentToRobotIdToRobotStateMap ->
      val robotId = segmentToRobotIdToRobotStateMap.key
      val distanceToTickList = segmentToRobotIdToRobotStateMap.value

      val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
      val folderName = "robot-distance-to-front-statistics"
      val subFolderName = distanceToTickList.first().first.segment.getSegmentIdentifier()

      val legendEntry = "Robot $robotId"
      val fileName = "${subFolderName}_robot_$robotId"
      val xValues = distanceToTickList.map { it.first.currentTick.toSeconds() }
      val yValues = distanceToTickList.map { it.second }
      combinedValuesMap[legendEntry] = xValues to yValues

      plotDataAsLineChart(
          plot =
              getPlot(
                  legendEntry = legendEntry,
                  xValues = xValues,
                  yValues = yValues,
                  "tick",
                  "distance to front (m))",
                  "Distance for"),
          folder = folderName,
          subFolder = subFolderName,
          fileName = fileName)

      plotDataAsLineChart(
          plot = getPlot(combinedValuesMap, "time", "distance to front", "Distance for"),
          folder = folderName,
          subFolder = subFolderName,
          fileName = "${subFolderName}_combined")
    }
  }

  override fun writePlotDataCSV() {
    robotIdToDistanceAtTickMap.forEach { segmentToRobotIdToRobotStateMap ->
      val robotId = segmentToRobotIdToRobotStateMap.key
      val distanceToTickList = segmentToRobotIdToRobotStateMap.value

      val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
      val folderName = "robot-distance-to-front-statistics"
      val subFolderName = distanceToTickList.first().first.segment.getSegmentIdentifier()

      val legendEntry = "Robot $robotId"
      val fileName = "${subFolderName}_robot_$robotId"
      val xValues = distanceToTickList.map { it.first.currentTick.toSeconds() }
      val yValues = distanceToTickList.map { it.second }
      combinedValuesMap[legendEntry] = xValues to yValues

      saveAsCSVFile(
          csvString = getCSVString(columnEntry = legendEntry, xValues = xValues, yValues = yValues),
          folder = folderName,
          subFolder = subFolderName,
          fileName = fileName)
    }

    //          saveAsCSVFile(
    //              csvString = getCSVString(combinedValuesMap),
    //              folder = folderName,
    //              subFolder = subFolderName,
    //              fileName = "${subFolderName}_combined")
  }
}
