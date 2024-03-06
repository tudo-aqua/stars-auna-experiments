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

package tools.aqua.stars.auna.exporter

import java.io.File
import kotlin.math.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.aqua.stars.auna.experiments.downloadAndUnzipExperimentsData
import tools.aqua.stars.auna.experiments.downloadWaypointsData
import tools.aqua.stars.auna.experiments.loadSegments
import tools.aqua.stars.data.av.track.Lane
import tools.aqua.stars.data.av.track.convertTrackToLanes
import tools.aqua.stars.importer.auna.Quaternion
import tools.aqua.stars.importer.auna.Vector
import tools.aqua.stars.auna.importer.importTrackData

const val OUTPUT_DIR = "./stars-auna-export/"
const val OUTPUT_FILE_NAME = "auna"

const val DEFAULT_ACTOR_TYPE_ID = "robot"
const val DEFAULT_LANE_ELEVATION = 0.0

/** A [List] of all [ActorType]s used in this experiment. */
val ACTOR_TYPES =
    listOf(
        ActorType(
            actorTypeId = DEFAULT_ACTOR_TYPE_ID,
            width = 0.2f,
            length = 0.4f,
            height = 0.1f // TODO: get actual height
            )
    )

/** Exports calculated [Segment]s to the import format used by the STARS-Visualizer tool. */
fun main() {
  println("Export Experiments Data")
  downloadAndUnzipExperimentsData()
  downloadWaypointsData()
  print("Finished downloading files")

  println("Import Track Data")
  val track = tools.aqua.stars.auna.importer.importTrackData()
  println("Convert Track Data")
  val lanes = convertTrackToLanes(track)

  println("Create Export Directory")
  File(OUTPUT_DIR).mkdirs()

  println("Export Static Data")
  exportStaticData(lanes)

  println("Export Dynamic Data")
  exportDynamicData(lanes)

  println("Export finished successfully")
}

/**
 * Exports static data to directory specified in [OUTPUT_DIR]
 *
 * @param lanes experiment data as [List] of [Lane]s.
 */
private fun exportStaticData(lanes: List<Lane>) {
  println("Static Data: Parse Lanes")
  val staticData =
      StaticData(
          lines =
              lanes.map { lane ->
                Line(
                    width = lane.width.toFloat(),
                    coordinates =
                        lane.waypoints.map { waypoint ->
                          Location(waypoint.x, waypoint.y, DEFAULT_LANE_ELEVATION)
                        })
              })
  println("Static Data: Export Lines")
  val staticDataJson = Json.encodeToString(staticData)
  val staticDataFilePath = "$OUTPUT_DIR${OUTPUT_FILE_NAME}_static.json"
  File(staticDataFilePath).writeText(staticDataJson)
  println("Static Data: Export to file $staticDataFilePath finished successfully!")
}

/**
 * Exports dynamic data to directory specified in [OUTPUT_DIR].
 *
 * @param lanes experiment data as [List] of [Lane]s.
 */
private fun exportDynamicData(lanes: List<Lane>) {
  println("Dynamic Data: Load Segments")
  val segments = loadSegments(lanes)
  println("Dynamic Data: Parse Segments")
  val dynamicData =
      DynamicData(
          segments =
              segments
                  .map { segment ->
                    val tickData = segment.tickData.sortedBy { it.currentTick }
                    Segment(
                        segmentSource = segment.segmentSource,
                        startTick = tickData.first().currentTick.toDoubleValue(),
                        endTick = tickData.last().currentTick.toDoubleValue(),
                        tickData =
                            tickData.map { tick ->
                              TickData(
                                  tick = tick.currentTick.toDoubleValue(),
                                  actors =
                                      tick.entities.map { entity ->
                                        ActorPosition(
                                            actorId = entity.id,
                                            actorTypeId = DEFAULT_ACTOR_TYPE_ID,
                                            location = vectorToLocation(entity.position),
                                            rotation = quaternionToEuler(entity.rotation))
                                      })
                            })
                  }
                  .toList(),
          ACTOR_TYPES)

  // Used as identifier for json file
  val segmentSources = segments.map { it.segmentSource }.toSet().joinToString("-")

  println("Dynamic Data: Export Segments")
  val dynamicDataJson = Json.encodeToString(dynamicData)
  val filePath = "$OUTPUT_DIR${OUTPUT_FILE_NAME}_${segmentSources}_dynamic.json"
  File(filePath).writeText(dynamicDataJson)
  println("Dynamic Data: Export to file $filePath finished successfully!")
}

/**
 * Returns a nullable [Rotation] in euler angles representation for a given [Quaternion].
 *
 * @param quaternion The [Quaternion] to parse into a [Rotation]. (0.0, 0.0, 0.0) is used as a
 *   fallback if the input is null.
 * @return [Rotation] in euler angles representation.
 */
private fun quaternionToEuler(quaternion: Quaternion?): Rotation {
  if (quaternion == null) {
    return Rotation(pitch = 0.0, yaw = 0.0, roll = 0.0)
  }

  // Roll axis
  val sinRollCosPitch = 2 * (quaternion.w * quaternion.x + quaternion.y * quaternion.z)
  val cosRollCosPitch = 1 - 2 * (quaternion.x * quaternion.x + quaternion.y * quaternion.y)
  val roll = atan2(sinRollCosPitch, cosRollCosPitch)

  // Pitch axis
  val sinPitch = 2 * (quaternion.w * quaternion.y - quaternion.z * quaternion.x)
  val pitch =
      if (abs(sinPitch) >= 1.0) {
        if (sinPitch > 0) {
          PI / 2
        } else {
          -PI / 2
        }
      } else {
        asin(sinPitch)
      }

  // Yaw axis
  val sinYawCosPitch = 2 * (quaternion.w * quaternion.z + quaternion.x * quaternion.y)
  val cosYawCosPitch = 1 - 2 * (quaternion.y * quaternion.y + quaternion.z * quaternion.z)
  val yaw = atan2(sinYawCosPitch, cosYawCosPitch)

  return Rotation(roll = roll, pitch = pitch, yaw = yaw)
}

/**
 * Parses a nullable [Vector] to a [Location] and uses (0.0, 0.0, 0.0) as a default if the input
 * [Vector] is null.
 *
 * @param vector The [Vector] to parse into [Location]. Fallback (0.0, 0.0, 0.0) is used if input is
 *   null.
 * @return Parsed [Location].
 */
private fun vectorToLocation(vector: Vector?) =
    Location(x = vector?.x ?: 0.0, y = vector?.x ?: 0.0, z = vector?.x ?: 0.0)
