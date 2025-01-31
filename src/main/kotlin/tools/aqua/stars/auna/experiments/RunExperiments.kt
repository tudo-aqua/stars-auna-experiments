/*
 * Copyright 2023-2025 The STARS AuNa Experiments Authors
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
import tools.aqua.stars.auna.metrics.heading.RobotHeadingStatisticsMetric
import tools.aqua.stars.auna.metrics.lateralOffset.RobotLateralOffsetAverageStatisticsMetric
import tools.aqua.stars.auna.metrics.lateralOffset.RobotLateralOffsetMaxStatisticsMetric
import tools.aqua.stars.auna.metrics.lateralOffset.RobotLateralOffsetMinStatisticsMetric
import tools.aqua.stars.auna.metrics.lateralOffset.RobotLateralOffsetStatisticsMetric
import tools.aqua.stars.auna.metrics.steeringAngle.RobotSteeringAngleAverageStatisticsMetric
import tools.aqua.stars.auna.metrics.steeringAngle.RobotSteeringAngleMaxStatisticsMetric
import tools.aqua.stars.auna.metrics.steeringAngle.RobotSteeringAngleMinStatisticsMetric
import tools.aqua.stars.auna.metrics.steeringAngle.RobotSteeringAngleStatisticsMetric
import tools.aqua.stars.auna.metrics.velocity.RobotVelocityAverageStatisticsMetric
import tools.aqua.stars.auna.metrics.velocity.RobotVelocityMaxStatisticsMetric
import tools.aqua.stars.auna.metrics.velocity.RobotVelocityMinStatisticsMetric
import tools.aqua.stars.auna.metrics.velocity.RobotVelocityStatisticsMetric
import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.metric.metrics.evaluation.InvalidTSCInstancesPerTSCMetric
import tools.aqua.stars.core.metric.metrics.evaluation.MissedTSCInstancesPerTSCMetric
import tools.aqua.stars.core.metric.metrics.evaluation.SegmentCountMetric
import tools.aqua.stars.core.metric.metrics.evaluation.ValidTSCInstancesPerTSCMetric
import tools.aqua.stars.core.metric.metrics.postEvaluation.FailedMonitorsGroupedByTSCInstanceMetric
import tools.aqua.stars.core.metric.metrics.postEvaluation.FailedMonitorsGroupedByTSCNodeMetric
import tools.aqua.stars.core.metric.metrics.postEvaluation.FailedMonitorsMetric
import tools.aqua.stars.core.metric.metrics.postEvaluation.MissedPredicateCombinationsPerTSCMetric
import tools.aqua.stars.data.av.track.*

/** Executes the experiments. */
fun main() {
  val tsc = tsc()

  println("Import Track Data")
  val track = importTrackData()

  println("Convert Track Data")
  val lanes = convertTrackToLanes(track, segmentsPerLane = 3)

  println("Load Ticks")
  val ticks = loadTicks(lanes)

  println("Create Segments")
  val segments = SliceAcceleration().slice(ticks, listOf(2, 3))

  println("Found ${segments.toList().size} segments.")

  val tscEvaluation =
      TSCEvaluation(tscList = tsc.buildProjections(), writePlots = false, writePlotDataCSV = false)

  val validTSCInstancesPerTSCMetric =
      ValidTSCInstancesPerTSCMetric<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference>()

  val plotSegments = true
  tscEvaluation.registerMetricProviders(
      // Generic metrics
      SegmentCountMetric(),
      validTSCInstancesPerTSCMetric,
      InvalidTSCInstancesPerTSCMetric(),
      MissedTSCInstancesPerTSCMetric(),
      MissedPredicateCombinationsPerTSCMetric(validTSCInstancesPerTSCMetric),
      FailedMonitorsMetric(validTSCInstancesPerTSCMetric),
      FailedMonitorsGroupedByTSCInstanceMetric(validTSCInstancesPerTSCMetric),
      FailedMonitorsGroupedByTSCNodeMetric(validTSCInstancesPerTSCMetric, onlyLeafNodes = true),

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

      // Heading
      RobotHeadingStatisticsMetric(plotSegments),
  )
  println("Run Evaluation")
  tscEvaluation.runEvaluation(segments = segments)
}

/**
 * Loads all [TickData] based on the given [List] of [Lane]s.
 *
 * @param lanes The [List] of [Lane]s.
 * @return A [List] of [TickData].
 */
fun loadTicks(lanes: List<Lane>): List<TickData> {
  val path = File(DYNAMIC_DATA_DIRECTORY).toPath()
  val messages =
      importDrivingData(path)
          .flatMap { (_, entries) -> entries.filterIsInstance<Message>() }
          .sortedWith(
              compareBy({ it.header.timeStamp.seconds }, { it.header.timeStamp.nanoseconds }))

  val waypoints = lanes.flatMap { it.waypoints }

  return getTicksFromMessages(messages, waypoints = waypoints)
}
