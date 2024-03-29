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

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tools.aqua.stars.core.metric.providers.Plottable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.utils.*
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.track.*

class RobotDistanceToFrontStatisticsMetric(private val plotSegments: Boolean = true) :
    SegmentMetricProvider<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>, Plottable {
  private var robotIdToDistanceAtTickMap: MutableMap<Int, List<Pair<TickData, Double>>> =
      mutableMapOf()

  override fun evaluate(
      segment: SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>
  ) {
    val primaryEntityId = segment.primaryEntityId

    if (primaryEntityId == 1) return

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

  @Suppress("DuplicatedCode")
  override fun writePlots() {
    val folderName = "robot-distance-to-front-statistics"
    val allValuesMap = mutableMapOf<String, Pair<MutableList<Number>, MutableList<Number>>>()
    val finished = AtomicInteger(0)

    runBlocking(Dispatchers.Default) {
      robotIdToDistanceAtTickMap
          .map {
            launch {
              val robotId = it.key
              val distanceToTickList = it.value

              val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
              val subFolderName = distanceToTickList.first().first.segment.getSegmentIdentifier()

              val legendEntry = "Robot $robotId"
              val fileName = "${subFolderName}_robot_$robotId"
              val xValues = distanceToTickList.map { it.first.currentTick.toSeconds() }
              val yValues = distanceToTickList.map { it.second }

              combinedValuesMap[legendEntry] = xValues to yValues

              synchronized(allValuesMap) {
                allValuesMap.putIfAbsent(legendEntry, mutableListOf<Number>() to mutableListOf())
                allValuesMap[legendEntry]!!.first += xValues
                allValuesMap[legendEntry]!!.second += yValues
              }

              if (plotSegments) {
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

                finished.incrementAndGet().let { i ->
                  print(
                      "\rWriting Plots for Robot distance to front: " +
                          "$i/${robotIdToDistanceAtTickMap.size} " +
                          "(${i * 100 / robotIdToDistanceAtTickMap.size}%) " +
                          "on ${Thread.currentThread()}")
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
                  yAxisName = "distance to front",
                  legendHeader = "Distance for"),
          size = 2500 to 500,
          folder = folderName,
          subFolder = "all",
          fileName = "distance_to_front_all_${legendEntry}")

      plotDataAsHistogram(
          plot =
              getPlot(
                  legendEntry = legendEntry,
                  xValues = values.second,
                  yValues = values.second,
                  xAxisName = "distance to front",
                  yAxisName = "frequency",
                  legendHeader = "Distance for"),
          folder = folderName,
          subFolder = "all",
          fileName = "distance_to_front_hist_all_lin_${legendEntry}")

      plotDataAsHistogram(
          plot =
              getPlot(
                  legendEntry = legendEntry,
                  xValues = values.second,
                  yValues = values.second,
                  xAxisName = "distance to front",
                  yAxisName = "frequency",
                  legendHeader = "Distance for"),
          logScaleY = true,
          folder = folderName,
          subFolder = "all",
          fileName = "distance_to_front_hist_all_log_${legendEntry}")
    }

    println("\rWriting Plots for Robot distance to front: finished")
  }

  @Suppress("DuplicatedCode")
  override fun writePlotDataCSV() {
    val finished = AtomicInteger(0)

    runBlocking(Dispatchers.Default) {
      robotIdToDistanceAtTickMap
          .map {
            launch {
              val robotId = it.key
              val distanceToTickList = it.value

              val combinedValuesMap = mutableMapOf<String, Pair<List<Number>, List<Number>>>()
              val folderName = "robot-distance-to-front-statistics"
              val subFolderName = distanceToTickList.first().first.segment.getSegmentIdentifier()

              val legendEntry = "Robot $robotId"
              val fileName = "${subFolderName}_robot_$robotId"
              val xValues = distanceToTickList.map { it.first.currentTick.toSeconds() }
              val yValues = distanceToTickList.map { it.second }
              combinedValuesMap[legendEntry] = xValues to yValues

              saveAsCSVFile(
                  csvString =
                      getCSVString(columnEntry = legendEntry, xValues = xValues, yValues = yValues),
                  folder = folderName,
                  subFolder = subFolderName,
                  fileName = fileName)

              //          saveAsCSVFile(
              //              csvString = getCSVString(combinedValuesMap),
              //              folder = folderName,
              //              subFolder = subFolderName,
              //              fileName = "${subFolderName}_combined")

              finished.incrementAndGet().let { i ->
                print(
                    "\rWriting CSV for Robot distance to front: " +
                        "$i/${robotIdToDistanceAtTickMap.size} " +
                        "(${i * 100 / robotIdToDistanceAtTickMap.size}%) " +
                        "on ${Thread.currentThread()}")
              }
            }
          }
          .forEach { it.join() }
    }
    println(
        "\rWriting CSV for Robot distance to front: " +
            "${robotIdToDistanceAtTickMap.size}/${robotIdToDistanceAtTickMap.size} (100%)")
  }
}
