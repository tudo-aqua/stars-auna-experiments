/*
 * Copyright 2023 The STARS AuNa Experiments Authors
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

package tools.aqua.stars.auna.experiments

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.io.path.name
import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.metric.metrics.evaluation.*
import tools.aqua.stars.core.metric.metrics.postEvaluation.FailedMonitorsMetric
import tools.aqua.stars.core.metric.metrics.postEvaluation.MissingPredicateCombinationsPerProjectionMetric
import tools.aqua.stars.data.av.track.*
import tools.aqua.stars.data.av.track.TickData
import tools.aqua.stars.importer.auna.Message
import tools.aqua.stars.importer.auna.Time
import tools.aqua.stars.importer.auna.importDrivingData
import tools.aqua.stars.importer.auna.importTrackData

fun main() {
  downloadAndUnzipExperimentsData()
  downloadWaypointsData()
  println("Finished downloading files")

  val tsc = tsc()
  println("Import Track Data")
  val track = importTrackData()
  println("Convert Track Data")
  val lanes = convertTrackToLanes(track)
  println("Load Segments")
  val segments = loadSegments(lanes)

  val tscEvaluation =
      TSCEvaluation(tsc = tsc, segments = segments, projectionIgnoreList = listOf(""))

  tscEvaluation.registerMetricProvider(SegmentCountMetric())
  tscEvaluation.registerMetricProvider(SegmentDurationPerIdentifierMetric())
  tscEvaluation.registerMetricProvider(TotalSegmentTimeLengthMetric())
  val validTSCInstancesPerProjectionMetric =
      ValidTSCInstancesPerProjectionMetric<Robot, TickData, Segment>()
  tscEvaluation.registerMetricProvider(InvalidTSCInstancesPerProjectionMetric())
  tscEvaluation.registerMetricProvider(MissedTSCInstancesPerProjectionMetric())
  tscEvaluation.registerMetricProvider(
      MissingPredicateCombinationsPerProjectionMetric(validTSCInstancesPerProjectionMetric))
  tscEvaluation.registerMetricProvider(FailedMonitorsMetric(validTSCInstancesPerProjectionMetric))

  println("Run Evaluation")
  tscEvaluation.runEvaluation()
}

fun loadSegments(lanes: List<Lane>): Sequence<Segment> {
  val path = File(SIMULATION_RUN_FOLDER).toPath()
  val sourcesToContentMap = importDrivingData(path)
  val messages = sortMessagesBySentTime(sourcesToContentMap)
  val waypoints = lanes.flatMap { it.waypoints }
  println("Calculate ticks")
  val ticks = getTicksFromMessages(messages, waypoints = waypoints)
  println("Slice Ticks into Segments")
  val segments = segmentTicksIntoSegments(path.name, ticks)
  return segments.asSequence()
}

/**
 * Creates a sorted [List] of all [Message]s. Sorted by [Time.seconds] and [Time.nanoseconds].
 *
 * @param messageSourceToContentMap A [Map] which maps a [DataSource] to all its [Message]s.
 * @return A sorted [List] of [Message]s.
 */
fun sortMessagesBySentTime(messageSourceToContentMap: Map<DataSource, List<Any>>): List<Message> {
  return messageSourceToContentMap
      .flatMap { (_, entries) -> entries.filterIsInstance<Message>() }
      .sortedWith(compareBy({ it.header.timeStamp.seconds }, { it.header.timeStamp.nanoseconds }))
}

/**
 * Checks if the experiments data is available. Otherwise, it is downloaded and extracted to the
 * correct folder.
 */
fun downloadAndUnzipExperimentsData() {
  if (!File(DOWNLOAD_FOLDER_NAME).exists()) {
    println("The experiments data is missing.")
    if (!File("$DOWNLOAD_FOLDER_NAME.zip").exists()) {
      println("The experiments data zip file is missing.")
      if (DOWNLOAD_EXPERIMENTS_DATA) {
        println("Start with downloading the experiments data. This may take a while.")
        downloadExperimentsData()
        println("Finished downloading.")
      } else {
        simulationDataMissing()
      }
    }
    if (!File("$DOWNLOAD_FOLDER_NAME.zip").exists()) {
      simulationDataMissing()
    }
    println("Extract experiments data from zip file.")
    extractZipFile(
        zipFile = File("$DOWNLOAD_FOLDER_NAME.zip"),
        extractTo = File("./$DOWNLOAD_FOLDER_NAME"),
        true)
  }
  if (!File(DOWNLOAD_FOLDER_NAME).exists()) {
    simulationDataMissing()
  }
}

/**
 * Throws an exception when the experiments data is not available and when the
 * [DOWNLOAD_EXPERIMENTS_DATA] is set to false.
 */
fun simulationDataMissing() {
  error(
      "The experiments data is not available. Either download it: $DRIVING_DATA_DOWNLOAD_URL and $TRACK_DATA_DOWNLOAD_URL or set " +
          "DOWNLOAD_EXPERIMENTS_DATA to 'true'")
}

/** Download the experiments data and saves it in the root directory of the project. */
fun downloadExperimentsData() {
  URL(DRIVING_DATA_DOWNLOAD_URL).openStream().use {
    Files.copy(it, Paths.get("$DOWNLOAD_FOLDER_NAME.zip"))
  }
}

/** Download the waypoint data and saves it in the root directory of the project. */
fun downloadWaypointsData() {
  val waypointsFileName = WAYPOINTS_FILE_NAME
  if (!File(waypointsFileName).exists()) {
    println("The waypoints data is missing.")
    if (DOWNLOAD_EXPERIMENTS_DATA) {
      println("Start with downloading the waypoints data.")
      URL(TRACK_DATA_DOWNLOAD_URL).openStream().use {
        Files.copy(it, Paths.get(WAYPOINTS_FILE_NAME))
      }
      println("Finished downloading.")
    } else {
      simulationDataMissing()
    }
  }
  if (!File(WAYPOINTS_FILE_NAME).exists()) {
    simulationDataMissing()
  }
}

/**
 * Extract a zip file into any directory
 *
 * @param zipFile src zip file
 * @param extractTo directory to extract into. There will be new folder with the zip's name inside
 *   [extractTo] directory.
 * @param extractHere no extra folder will be created and will be extracted directly inside
 *   [extractTo] folder.
 * @return the extracted directory i.e, [extractTo] folder if [extractHere] is `true` and
 *   [extractTo]\zipFile\ folder otherwise.
 */
private fun extractZipFile(
    zipFile: File,
    extractTo: File,
    extractHere: Boolean = false,
): File? {
  return try {
    val outputDir =
        if (extractHere) {
          extractTo
        } else {
          File(extractTo, zipFile.nameWithoutExtension)
        }

    ZipFile(zipFile).use { zip ->
      zip.entries().asSequence().forEach { entry ->
        zip.getInputStream(entry).use { input ->
          if (entry.isDirectory) {
            val d = File(outputDir, entry.name)
            if (!d.exists()) d.mkdirs()
          } else {
            val f = File(outputDir, entry.name)
            if (f.parentFile?.exists() != true) f.parentFile?.mkdirs()

            f.outputStream().use { output -> input.copyTo(output) }
          }
        }
      }
    }
    extractTo
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}
