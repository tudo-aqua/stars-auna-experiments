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

@file:Suppress("InjectDispatcher")

package tools.aqua.stars.auna.metrics.lateralOffset

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tools.aqua.stars.core.metric.providers.Plottable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.utils.*
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

/** Metric to calculate the lateral offset statistics of a robot. */
class RobotLateralOffsetStatisticsMetric(private val plotSegments: Boolean = true) :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>, Plottable {
  private val segmentToRobotIdToRobotStateMap: MutableList<Pair<Segment, Map<Int, List<Robot>>>> =
      mutableListOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val robotIdToRobotStateMap = segment.tickData.map { it.entities }.flatten().groupBy { it.id }
    segmentToRobotIdToRobotStateMap += segment as Segment to robotIdToRobotStateMap
  }

  @Suppress("DuplicatedCode", "StringLiteralDuplication", "LongMethod")
  override fun writePlots() {
    val folderName = "lateral-offset-statistics"
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
                val yValues = robotStates.map { it.lateralOffset }
                val xValues = robotStates.map { it.tickData.currentTick.toSeconds() }

                combinedValuesMap[legendEntry] = xValues to yValues

                synchronized(allValuesMap) {
                  allValuesMap
                      .getOrPut(legendEntry) { mutableListOf<Number>() to mutableListOf() }
                      .let {
                        it.first += xValues
                        it.second += yValues
                      }
                }

                if (plotSegments) {
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

                  plotDataAsLineChart(
                      plot =
                          getPlot(
                              combinedValuesMap.toSortedMap(),
                              "time",
                              "lateral offset",
                              "lateral offset for"),
                      folder = folderName,
                      subFolder = subFolderName,
                      fileName = "${subFolderName}_combined")

                  finished.incrementAndGet().let { i ->
                    print(
                        "\rWriting Plots for Robot lateral offset: " +
                            "$i/${segmentToRobotIdToRobotStateMap.size} " +
                            "(${i * 100 / segmentToRobotIdToRobotStateMap.size}%) " +
                            "on ${Thread.currentThread()}")
                  }
                }
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
                  yAxisName = "lateral offset",
                  legendHeader = "Lateral offset for"),
          size = 2500 to 500,
          folder = folderName,
          subFolder = "all",
          yAxisScaleMaxValue = 0.8,
          fileName = "lateral_offset_all_${legendEntry}")

      plotDataAsHistogram(
          plot =
              getPlot(
                  legendEntry = legendEntry,
                  xValues = values.second,
                  yValues = values.second,
                  xAxisName = "lateral offset",
                  yAxisName = "frequency",
                  legendHeader = "Lateral offset for"),
          folder = folderName,
          subFolder = "all",
          fileName = "lateral_offset_hist_all_lin_${legendEntry}")

      plotDataAsHistogram(
          plot =
              getPlot(
                  legendEntry = legendEntry,
                  xValues = values.second,
                  yValues = values.second,
                  xAxisName = "lateral offset",
                  yAxisName = "frequency",
                  legendHeader = "Lateral offset for"),
          logScaleY = true,
          folder = folderName,
          subFolder = "all",
          fileName = "lateral_offset_hist_all_log_${legendEntry}")
    }

    plotDataAsLineChart(
        plot = getPlot(allValuesMap.toSortedMap(), "tick", "lateral offset", "Lateral offset for"),
        size = 2500 to 500,
        folder = folderName,
        subFolder = "all",
        fileName = "lateral_offset_all_combined")

    println("\rWriting Plots for Robot lateral offset: finished")
  }

  @Suppress("DuplicatedCode")
  override fun writePlotDataCSV() {
    val finished = AtomicInteger(0)

    runBlocking(Dispatchers.Default) {
      segmentToRobotIdToRobotStateMap
          .map {
            launch {
              val segment = it.first
              val robotIdToRobotStates = it.second

              val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
              val folderName = "lateral-offset-statistics"
              val subFolderName = segment.getSegmentIdentifier()

              robotIdToRobotStates.forEach { (robotId, robotStates) ->
                val legendEntry = "Robot $robotId"
                val fileName = "${subFolderName}_robot_$robotId"
                val yValues = robotStates.map { it.lateralOffset }
                val xValues = robotStates.map { it.tickData.currentTick.toSeconds() }

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
                    "\rWriting CSV for Robot lateral offset: " +
                        "$i/${segmentToRobotIdToRobotStateMap.size} " +
                        "(${i * 100 / segmentToRobotIdToRobotStateMap.size}%) " +
                        "on ${Thread.currentThread()}")
              }
            }
          }
          .forEach { it.join() }
    }
    println(
        "\rWriting CSV for Robot lateral offset: " +
            "${segmentToRobotIdToRobotStateMap.size}/${segmentToRobotIdToRobotStateMap.size} (100%)")
  }
}
