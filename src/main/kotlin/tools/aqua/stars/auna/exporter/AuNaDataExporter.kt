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
import java.io.FileOutputStream
import kotlin.math.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import tools.aqua.stars.auna.experiments.*
import tools.aqua.stars.auna.experiments.slicer.SliceAcceleration
import tools.aqua.stars.auna.experiments.slicer.Slicer
import tools.aqua.stars.auna.importer.Quaternion
import tools.aqua.stars.auna.importer.Vector
import tools.aqua.stars.auna.importer.importTrackData
import tools.aqua.stars.data.av.track.Lane
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.convertTrackToSingleLane

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
            ))

/** The [Json] instance used for serialization with domain specific configuration. */
val json = Json { encodeDefaults = true }

/** Exports calculated [Segment]s to the import format used by the STARS-Visualizer tool. */
fun main() {
  println("Export Experiments Data")

  println("Import Track Data")
  val track = importTrackData()
  println("Convert Track Data")
  val lanes = listOf(convertTrackToSingleLane(track))

  println("Create Export Directory")
  File(OUTPUT_DIR).mkdirs()

  println("Export Static Data")
  exportStaticData(lanes)

  println("Export Dynamic Data")
  exportDynamicData(lanes, SliceAcceleration())

  println("Export finished successfully")
}

/**
 * Exports static data to directory specified in [OUTPUT_DIR].
 *
 * @param lanes experiment data as [List] of [Lane]s.
 */
@OptIn(ExperimentalSerializationApi::class)
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
  val staticDataFilePath = "$OUTPUT_DIR${OUTPUT_FILE_NAME}_static.json"
  FileOutputStream(staticDataFilePath).use { fos ->
    json.encodeToStream(StaticData.serializer(), staticData, fos)
  }

  println("Static Data: Export to file $staticDataFilePath finished successfully!")
}

/**
 * Exports dynamic data to directory specified in [OUTPUT_DIR].
 *
 * @param lanes experiment data as [List] of [Lane]s.
 */
@OptIn(ExperimentalSerializationApi::class)
private fun exportDynamicData(lanes: List<Lane>, slicer: Slicer) {
  println("Load Ticks")
  val ticks = loadTicks(lanes)

  println("Dynamic Data: Load Segments")
  val segments = slicer.slice(ticks)

  println("Dynamic Data: Parse Segments")
  val primaryEntityIds = segments.groupBy { it.primaryEntityId }.map { it.key }.sorted()

  // Used as identifier for json file
  val segmentSources = segments.map { it.segmentSource }.toSet().joinToString("-")

  for (primaryEntityId in primaryEntityIds) {
    val dynamicData =
        DynamicData(
            segments =
                segments
                    .filter { it.primaryEntityId == primaryEntityId }
                    .map { segment ->
                      val tickData = segment.tickData.sortedBy { it.currentTick }
                      Segment(
                          segmentSource = segment.segmentSource,
                          startTick = tickData.first().currentTick.toSeconds(),
                          endTick = tickData.last().currentTick.toSeconds(),
                          primaryActorId = segment.primaryEntityId,
                          tickData =
                              tickData.map { tick ->
                                TickData(
                                    tick = tick.currentTick.toSeconds(),
                                    actors =
                                        tick.entities.map { entity ->
                                          ActorPosition(
                                              actorId = entity.id,
                                              actorTypeId = DEFAULT_ACTOR_TYPE_ID,
                                              location = vectorToLocation(entity.position),
                                              rotation = quaternionToEuler(entity.rotation),
                                              description =
                                                  "${entity.velocity} m/s\n${entity.acceleration} m/s²\n${entity.lateralOffset} m lat off",
                                              trajectoryColors =
                                                  thresholdTrajectories(entity, tick))
                                        })
                              })
                    }
                    .toList(),
            ACTOR_TYPES)
    val filePath =
        "$OUTPUT_DIR${OUTPUT_FILE_NAME}_${segmentSources}_ego${primaryEntityId}_dynamic.json"
    print(
        "\rDynamic Data: Exporting ${dynamicData.segments.count()} Segments for ego vehicle $primaryEntityId at $filePath...")
    FileOutputStream(filePath).use { fos ->
      json.encodeToStream(DynamicData.serializer(), dynamicData, fos)
    }
  }
  println("\rDynamic Data: Exported dynamic data of ${primaryEntityIds.count()} ego vehicles!")
}

private fun thresholdTrajectories(robot: Robot, tick: tools.aqua.stars.data.av.track.TickData) =
    listOf(
        // Acceleration
        gradientColorValue(
            robot.acceleration,
            valueColors =
                listOf(
                    ACCELERATION_DECELERATION_STRONG_THRESHOLD to "#0010ff",
                    ACCELERATION_DECELERATION_STRONG_THRESHOLD + 0.000001 to "#006bff",
                    ACCELERATION_DECELERATION_WEAK_THRESHOLD to "#006bff",
                    ACCELERATION_DECELERATION_WEAK_THRESHOLD + 0.0000001 to "#1de100",
                    ACCELERATION_ACCELERATION_WEAK_THRESHOLD - 0.0000001 to "#1de100",
                    ACCELERATION_ACCELERATION_WEAK_THRESHOLD to "#ff8000",
                    ACCELERATION_ACCELERATION_STRONG_THRESHOLD - 0.000001 to "#ff8000",
                    ACCELERATION_ACCELERATION_STRONG_THRESHOLD to "#FF0000")),
        // Velocity
        gradientColorValue(
            value = robot.velocity,
            valueColors =
                listOf(
                    0.0 to "#0000FF",
                    VELOCITY_HIGH - 0.000001 to "#0000FF",
                    VELOCITY_HIGH to "#FFFF00",
                    VELOCITY_MAX - 0.000001 to "#FFFF00",
                    VELOCITY_MAX to "#FF0000")),
        // Lateral Offset
        gradientColorValue(
            value = robot.lateralOffset,
            valueColors =
                listOf(
                    0.0 to "#00FF00",
                    MAX_LATERAL_OFFSET - 0.00000001 to "#00FF00",
                    MAX_LATERAL_OFFSET to "#FF0000")),
        // Steering Angle
        gradientColorValue(
            value = abs(robot.steeringAngle),
            valueColors =
                listOf(
                    0.0 to "#000000",
                    STEERING_ANGLE_LOW - 0.000001 to "#000000",
                    STEERING_ANGLE_LOW to "#E8766c",
                    STEERING_ANGLE_HARD - 0.000001 to "#E8766c",
                    STEERING_ANGLE_HARD to "#e80500")),
        gradientColorValue(
            // Distance to Front
            value = distanceToFront(robot, tick),
            valueColors =
                listOf(
                    -1.0 to "#333333", // just applies to front robot
                    0.0 to "#333333",
                    0.000000001 to "#e80500",
                    DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW - 0.0000001 to "#e80500",
                    DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW to "#0b9c03",
                    DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH - 0.0000001 to "#0b9c03",
                    DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH to "#e3db00",
                )))

@Suppress("unused")
private fun gradientTrajectories(robot: Robot, tick: tools.aqua.stars.data.av.track.TickData) =
    listOf(
        gradientColorValue(
            robot.acceleration,
            valueColors =
                listOf(
                    -15.329432162982227 to "#0000FF",
                    -2.0 to "#0000F0",
                    0.0 to "#333333",
                    2.0 to "#F00000",
                    10.747724317295187 to "#FF0000")),
        gradientColorValue(
            value = robot.velocity,
            valueColors =
                listOf(
                    0.0 to "#0000FF",
                    2.0 to "#F5E43D",
                    3.0 to "#F52620",
                    3.724100563502384 to "#523FA1")),
        gradientColorValue(
            value = robot.lateralOffset,
            valueColors = listOf(0.0 to "#00FF00", 0.499999999 to "#00FF00", 0.5 to "#FF0000")),
        gradientColorValue(
            value = robot.steeringAngle,
            valueColors = listOf(-20.0 to "FF0000", 0.0 to "#000000", 20.0 to "#00FF00")),
        gradientColorValue(
            value = distanceToFront(robot, tick),
            valueColors =
                listOf(
                    -1.0 to "#333333",
                    0.0 to "#333333",
                    0.000000001 to "#FF0000",
                    0.5 to "#FF0000",
                    0.500000001 to "#00FF00",
                    3.0 to "#00FF00",
                    3.000000001 to "#F57E3D")))

/**
 * Returns the distance to the front robot.
 *
 * @param robot The robot to calculate the distance for.
 * @param tick The tick data to calculate the distance for.
 * @return The distance to the front robot. -1.0 if the front robot is not found (Robot is leading).
 */
private fun distanceToFront(robot: Robot, tick: tools.aqua.stars.data.av.track.TickData): Double {
  val frontRobot = tick.getEntityById(robot.id - 1) ?: return -1.0
  return robot.distanceToOther(frontRobot)
}

/**
 * Returns a hex color value based on a given value and a list of valueColors.
 *
 * @param value The value to calculate the color for.
 * @param valueColors A list of valueColors. Each valueColor is a pair of a value and a color in hex
 *   notation. Trajectory colors are interpolated between the valueColors. If the value is smaller
 *   or greater than the range of valueColors, the first or last valueColor is used.
 * @return The hex color value for the given value.
 */
private fun gradientColorValue(value: Double, valueColors: List<Pair<Double, String>>): String {
  require(valueColors.count() > 1) { "At least two valueColors are required" }

  val valueColorsSorted = valueColors.sortedBy { it.first }

  // If the value is smaller than the smallest valueColor, return the color of the smallest
  // valueColor
  if (value <= valueColorsSorted.first().first) {
    return valueColorsSorted.first().second
  }

  // If the value is greater than the greatest valueColor, return the color of the greatest
  // valueColor
  if (value >= valueColorsSorted.last().first) {
    return valueColorsSorted.last().second
  }

  valueColorsSorted.forEachIndexed { index, valueColor ->
    if (value < valueColor.first) {
      val lowerBound = if (index == 0) valueColor else valueColorsSorted[index - 1]
      val ratio = (value - lowerBound.first) / (valueColor.first - lowerBound.first)
      return interpolateHexColor(lowerBound.second, valueColor.second, ratio)
    }
  }
  return valueColorsSorted.last().second
}

/**
 * Interpolates between two hex colors with alpha values (RGBA format).
 *
 * @param color1 The first color in hex notation (with optional alpha at the end (AA = 255
 *   otherwise)).
 * @param color2 The second color in hex notation (with optional alpha at the end (AA = 255
 *   otherwise)).
 * @param ratio The ratio to interpolate between the two colors.
 * @return The interpolated color in hex notation with optional alpha (AA = 255 otherwise).
 */
private fun interpolateHexColor(color1: String, color2: String, ratio: Double): String {
  val hex1 = color1.substring(1)
  val hex2 = color2.substring(1)

  val alpha1 = hex1.length == 8
  val alpha2 = hex2.length == 8

  val r1 = hex1.substring(0, 2).toInt(16)
  val g1 = hex1.substring(2, 4).toInt(16)
  val b1 = hex1.substring(4, 6).toInt(16)
  val a1 = if (alpha1) hex1.substring(6, 8).toInt(16) else 255

  val r2 = hex2.substring(0, 2).toInt(16)
  val g2 = hex2.substring(2, 4).toInt(16)
  val b2 = hex2.substring(4, 6).toInt(16)
  val a2 = if (alpha2) hex2.substring(6, 8).toInt(16) else 255

  val r = (r1 + (r2 - r1) * ratio).toInt()
  val g = (g1 + (g2 - g1) * ratio).toInt()
  val b = (b1 + (b2 - b1) * ratio).toInt()
  val a = (a1 + (a2 - a1) * ratio).toInt()

  // convert to hex
  val interpolatedColorHex = String.format("#%02X%02X%02X%02X", r, g, b, a)
  return interpolatedColorHex
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
    Location(x = vector?.x ?: 0.0, y = vector?.y ?: 0.0, z = vector?.z ?: 0.0)
