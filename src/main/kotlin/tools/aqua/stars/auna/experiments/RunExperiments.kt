/*
 * Copyright 2023-2024 The STARS AuNa Experiments Authors
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
import tools.aqua.stars.auna.experiments.slicer.SliceAcceleration
import tools.aqua.stars.auna.importer.Message
import tools.aqua.stars.auna.importer.importDrivingData
import tools.aqua.stars.auna.importer.importTrackData
import tools.aqua.stars.auna.metrics.acceleration.RobotAccelerationAverageStatisticsMetric
import tools.aqua.stars.auna.metrics.acceleration.RobotAccelerationMaxStatisticsMetric
import tools.aqua.stars.auna.metrics.acceleration.RobotAccelerationMinStatisticsMetric
import tools.aqua.stars.auna.metrics.acceleration.RobotAccelerationStatisticsMetric
import tools.aqua.stars.auna.metrics.distanceToFront.RobotDistanceToFrontAverageStatisticsMetric
import tools.aqua.stars.auna.metrics.distanceToFront.RobotDistanceToFrontMaxStatisticsMetric
import tools.aqua.stars.auna.metrics.distanceToFront.RobotDistanceToFrontMinStatisticsMetric
import tools.aqua.stars.auna.metrics.distanceToFront.RobotDistanceToFrontStatisticsMetric
import tools.aqua.stars.auna.metrics.lateral_offset.RobotLateralOffsetAverageStatisticsMetric
import tools.aqua.stars.auna.metrics.lateral_offset.RobotLateralOffsetMaxStatisticsMetric
import tools.aqua.stars.auna.metrics.lateral_offset.RobotLateralOffsetMinStatisticsMetric
import tools.aqua.stars.auna.metrics.lateral_offset.RobotLateralOffsetStatisticsMetric
import tools.aqua.stars.auna.metrics.steering_angle.RobotSteeringAngleAverageStatisticsMetric
import tools.aqua.stars.auna.metrics.steering_angle.RobotSteeringAngleMaxStatisticsMetric
import tools.aqua.stars.auna.metrics.steering_angle.RobotSteeringAngleMinStatisticsMetric
import tools.aqua.stars.auna.metrics.steering_angle.RobotSteeringAngleStatisticsMetric
import tools.aqua.stars.auna.metrics.velocity.RobotVelocityAverageStatisticsMetric
import tools.aqua.stars.auna.metrics.velocity.RobotVelocityMaxStatisticsMetric
import tools.aqua.stars.auna.metrics.velocity.RobotVelocityMinStatisticsMetric
import tools.aqua.stars.auna.metrics.velocity.RobotVelocityStatisticsMetric
import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.metric.metrics.evaluation.InvalidTSCInstancesPerProjectionMetric
import tools.aqua.stars.core.metric.metrics.evaluation.MissedTSCInstancesPerProjectionMetric
import tools.aqua.stars.core.metric.metrics.evaluation.SegmentCountMetric
import tools.aqua.stars.core.metric.metrics.evaluation.ValidTSCInstancesPerProjectionMetric
import tools.aqua.stars.core.metric.metrics.postEvaluation.FailedMonitorsMetric
import tools.aqua.stars.core.metric.metrics.postEvaluation.MissingPredicateCombinationsPerProjectionMetric
import tools.aqua.stars.data.av.track.*

/** Executes the experiments. */
fun main() {
  downloadAndUnzipExperimentsData()
  downloadWaypointsData()
  println("Finished downloading files")

  val tsc = tsc()

  println("Projections:")
  tsc.buildProjections().forEach {
    println("TSC for Projection $it:")
    println(it.tsc)
    println("All possible instances:")
    println(it.possibleTSCInstances.size)
    println()
  }
  println("-----------------")

  println("Import Track Data")
  val track = importTrackData()

  println("Convert Track Data")
  val lanes = convertTrackToLanes(track, segmentsPerLane = 3)

  println("Load Ticks")
  val ticks = loadTicks(lanes)

  println("Create Segments")
  // val slicer = NoSlicing()
  // val slicer = SliceEqualChunkSize()
  val slicer = SliceAcceleration()
  val segments = slicer.slice(ticks)

  println("Found ${segments.toList().size} segments.")
  val tscEvaluation =
      TSCEvaluation(tsc = tsc, segments = segments, projectionIgnoreList = listOf(""))

  val validTSCInstancesPerProjectionMetric =
      ValidTSCInstancesPerProjectionMetric<
          Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>()

  val plotSegments = false
  tscEvaluation.registerMetricProviders(
      // Generic metrics
      SegmentCountMetric(),
      validTSCInstancesPerProjectionMetric,
      InvalidTSCInstancesPerProjectionMetric(),
      MissedTSCInstancesPerProjectionMetric(),
      MissingPredicateCombinationsPerProjectionMetric(validTSCInstancesPerProjectionMetric),
      FailedMonitorsMetric(validTSCInstancesPerProjectionMetric),

      // Velocity
      RobotVelocityStatisticsMetric(plotSegments),
      RobotVelocityAverageStatisticsMetric(),
      RobotVelocityMinStatisticsMetric(),
      RobotVelocityMaxStatisticsMetric(),

      // Acceleration
      RobotAccelerationStatisticsMetric(plotSegments),
      RobotAccelerationAverageStatisticsMetric(),
      RobotAccelerationMinStatisticsMetric(),
      RobotAccelerationMaxStatisticsMetric(),

      // Steering angle
      RobotSteeringAngleStatisticsMetric(plotSegments),
      RobotSteeringAngleAverageStatisticsMetric(),
      RobotSteeringAngleMinStatisticsMetric(),
      RobotSteeringAngleMaxStatisticsMetric(),

      // Lateral Offset
      RobotLateralOffsetStatisticsMetric(plotSegments),
      RobotLateralOffsetAverageStatisticsMetric(),
      RobotLateralOffsetMinStatisticsMetric(),
      RobotLateralOffsetMaxStatisticsMetric(),

      // Distance to front
      RobotDistanceToFrontStatisticsMetric(plotSegments),
      RobotDistanceToFrontAverageStatisticsMetric(),
      RobotDistanceToFrontMinStatisticsMetric(),
      RobotDistanceToFrontMaxStatisticsMetric(),
  )
  println("Run Evaluation")
  tscEvaluation.runEvaluation()
}

/**
 * Loads all [TickData] based on the given [List] of [Lane]s.
 *
 * @param lanes The [List] of [Lane]s.
 * @return A [List] of [TickData].
 */
fun loadTicks(lanes: List<Lane>): List<TickData> {
  val path = File(SIMULATION_RUN_FOLDER).toPath()
  val messages =
      importDrivingData(path)
          .flatMap { (_, entries) -> entries.filterIsInstance<Message>() }
          .sortedWith(
              compareBy({ it.header.timeStamp.seconds }, { it.header.timeStamp.nanoseconds }))

  val waypoints = lanes.flatMap { it.waypoints }

  return getTicksFromMessages(messages, waypoints = waypoints)
}

/** Holds the name of the downloaded file used for this experiment setup. */
val DOWNLOAD_FILE_NAME = "$DOWNLOAD_FOLDER_NAME.zip"

/**
 * Checks if the experiments data is available. Otherwise, it is downloaded and extracted to the
 * correct folder.
 */
fun downloadAndUnzipExperimentsData() {
  if (!File(DOWNLOAD_FOLDER_NAME).exists()) {
    println("The experiments data is missing.")
    if (!File(DOWNLOAD_FILE_NAME).exists()) {
      println("The experiments data zip file is missing.")
      if (DOWNLOAD_EXPERIMENTS_DATA) {
        println("Start with downloading the experiments data. This may take a while.")
        downloadExperimentsData()
        println("Finished downloading.")
      } else {
        simulationDataMissing()
      }
    }
    if (!File(DOWNLOAD_FILE_NAME).exists()) {
      simulationDataMissing()
    }
    println("Extract experiments data from zip file.")
    extractZipFile(
        zipFile = File(DOWNLOAD_FILE_NAME), extractTo = File("./$DOWNLOAD_FOLDER_NAME"), true)
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
      "The experiments data is not available. " +
          "Either download it: $DRIVING_DATA_DOWNLOAD_URL and $TRACK_DATA_DOWNLOAD_URL or set " +
          "DOWNLOAD_EXPERIMENTS_DATA to 'true'")
}

/** Download the experiments data and saves it in the root directory of the project. */
fun downloadExperimentsData() {
  URL(DRIVING_DATA_DOWNLOAD_URL).openStream().use { Files.copy(it, Paths.get(DOWNLOAD_FILE_NAME)) }
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
 * Extract a zip file into any directory.
 *
 * @param zipFile src zip file.
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
): File? =
    try {
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
              File(outputDir, entry.name).mkdirs()
            } else {
              File(outputDir, entry.name).let {
                it.parentFile?.mkdirs()
                it.outputStream().use { output -> input.copyTo(output) }
              }
            }
          }
        }
      }
      extractTo
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
