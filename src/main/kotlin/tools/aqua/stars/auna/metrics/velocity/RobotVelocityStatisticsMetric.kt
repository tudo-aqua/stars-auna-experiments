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

package tools.aqua.stars.auna.metrics.velocity

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import tools.aqua.stars.core.metric.providers.Plottable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.utils.getCSVString
import tools.aqua.stars.core.metric.utils.getPlot
import tools.aqua.stars.core.metric.utils.plotDataAsLineChart
import tools.aqua.stars.core.metric.utils.saveAsCSVFile
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

class RobotVelocityStatisticsMetric :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>, Plottable {
  private var segmentToRobotIdToRobotStateMap: MutableList<Pair<Segment, Map<Int, List<Robot>>>> =
      mutableListOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val robotIdToRobotStateMap = segment.tickData.map { it.entities }.flatten().groupBy { it.id }
    segmentToRobotIdToRobotStateMap += segment as Segment to robotIdToRobotStateMap
  }

  override fun writePlots() {
    val folderName = "velocity-statistics"
    val allValuesMap = mutableMapOf<String, Pair<MutableList<Number>, MutableList<Number>>>()
    val finished = AtomicInteger(0)

    runBlocking(Dispatchers.Default) {
      segmentToRobotIdToRobotStateMap
          .map {
            launch {
              val segment = it.first
              val robotIdToRobotStates = it.second

              val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
              val subFolderName = segment.getSegmentIdentifier()

              robotIdToRobotStates.forEach { (robotId, robotStates) ->
                val legendEntry = "Robot $robotId"
                val fileName = "${subFolderName}_robot_$robotId"
                val yValues = robotStates.map { it.velocity ?: 0.0 }
                val xValues = robotStates.map { it.tickData.currentTick.toSeconds() }

                combinedValuesMap[legendEntry] = xValues to yValues

                synchronized(allValuesMap) {
                  allValuesMap.putIfAbsent(legendEntry, mutableListOf<Number>() to mutableListOf())
                  allValuesMap[legendEntry]!!.first += xValues
                  allValuesMap[legendEntry]!!.second += yValues
                }

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
                  plot =
                      getPlot(
                          combinedValuesMap.toSortedMap(),
                          "tick",
                          "velocity (m/s)",
                          "Velocity for"),
                  folder = folderName,
                  subFolder = subFolderName,
                  fileName = "${subFolderName}_combined")

              finished.incrementAndGet().let { i ->
                print(
                    "\rWriting Plots for Robot velocity: " +
                        "$i/${segmentToRobotIdToRobotStateMap.size} " +
                        "(${i * 100 / segmentToRobotIdToRobotStateMap.size}%) " +
                        "on ${Thread.currentThread()}")
              }
            }
          }
          .forEach { it.join() }
    }

    allValuesMap.forEach { (legendEntry, values) ->
      plotDataAsLineChart(
          plot =
              getPlot(
                  legendEntry = legendEntry,
                  xValues = values.first,
                  yValues = values.second,
                  xAxisName = "tick",
                  yAxisName = "velocity (m/s)",
                  legendHeader = "Velocity for"),
          folder = folderName,
          subFolder = "all",
          fileName = "velocity_all_${legendEntry}")
    }

    plotDataAsLineChart(
        plot = getPlot(allValuesMap.toSortedMap(), "tick", "velocity (m/s)", "Velocity for"),
        folder = folderName,
        subFolder = "all",
        fileName = "velocity_all_combined")

    println(
        "\rWriting Plots for Robot velocity: " +
            "${segmentToRobotIdToRobotStateMap.size}/${segmentToRobotIdToRobotStateMap.size} (100%)")
  }

  override fun writePlotDataCSV() {
    val finished = AtomicInteger(0)

    runBlocking(Dispatchers.Default) {
      segmentToRobotIdToRobotStateMap
          .map {
            launch {
              val segment = it.first
              val robotIdToRobotStates = it.second

              val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
              val folderName = "robot-velocity-statistics"
              val subFolderName = segment.getSegmentIdentifier()

              robotIdToRobotStates.forEach { (robotId, robotStates) ->
                val legendEntry = "Robot $robotId"
                val fileName = "${subFolderName}_robot_$robotId"
                val yValues = robotStates.map { it.velocity ?: 0.0 }
                val xValues = robotStates.map { it.tickData.currentTick.seconds }

                combinedValuesMap[legendEntry] = xValues to yValues

                saveAsCSVFile(
                    csvString =
                        getCSVString(
                            columnEntry = legendEntry, xValues = xValues, yValues = yValues),
                    folder = folderName,
                    subFolder = subFolderName,
                    fileName = fileName)
              }

              saveAsCSVFile(
                  csvString = getCSVString(combinedValuesMap),
                  folder = folderName,
                  subFolder = subFolderName,
                  fileName = "${subFolderName}_combined")

              finished.incrementAndGet().let { i ->
                print(
                    "\rWriting CSV for Robot velocity: " +
                        "$i/${segmentToRobotIdToRobotStateMap.size} " +
                        "(${i * 100 / segmentToRobotIdToRobotStateMap.size}%) " +
                        "on ${Thread.currentThread()}")
              }
            }
          }
          .forEach { it.join() }
    }
    println(
        "\rWriting CSV for Robot velocity: " +
            "${segmentToRobotIdToRobotStateMap.size}/${segmentToRobotIdToRobotStateMap.size} (100%)")
  }
}
