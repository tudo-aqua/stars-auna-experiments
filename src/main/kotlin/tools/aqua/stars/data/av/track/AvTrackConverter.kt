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

package tools.aqua.stars.data.av.track

import kotlin.math.pow
import kotlin.math.sqrt
import tools.aqua.stars.importer.auna.*

fun getTicksFromMessages(messages: List<Message>, waypoints: List<Waypoint>): List<TickData> {
  val ticks = mutableListOf<TickData>()
  val robotIds = mutableSetOf<Int>()
  messages.forEach { message ->
    // Create caching list for newly created Robot objects
    val robots = mutableListOf<Robot>()
    // Create empty TickData object for later reference
    val tickData =
        TickData(currentTick = message.header.timeStamp.toDoubleValue(), entities = mutableListOf())
    // Get the robot id for the current message
    val robotId = getRobotIdFromMessage(message)
    // Save robot id
    robotIds.add(robotId)
    // Get the latest information for all other robots to be copied later on
    val latestOtherRobotInformation =
        robotIds.minus(robotId).map { getLatestRobotInformation(ticks, it) }
    // Copy the latest information of all other robots with the current tickData
    val otherRobotInformationCopy =
        latestOtherRobotInformation.mapNotNull { it?.copyToNewTick(tickData) }
    // Save copy in caching list
    robots.addAll(otherRobotInformationCopy)

    // Get the latest information for the robot that sent the current message
    val latestRobotInformation = getLatestRobotInformation(ticks, robotId)
    // Get Robot information form current message
    val currentRobot =
        getRobotFromMessageAndLatestInformation(
            message, latestRobotInformation, robotId, tickData, waypoints)
    // Add current Robot information to robot cache list
    robots.add(currentRobot)

    // Update entities of tickData to the caching list of robots
    tickData.entities = robots
  }
  return ticks
}

/**  */
fun getRobotFromMessageAndLatestInformation(
    message: Message,
    latestRobot: Robot?,
    robotId: Int,
    tickData: TickData,
    waypoints: List<Waypoint>
): Robot {
  when (message) {
    is CAM -> {
      val posOnLaneAndLateralOffset =
          calculatePosOnLaneAndLateralOffset(Vector(message.x, message.y, message.z), waypoints)
      return Robot(
          id = robotId,
          tickData = tickData,
          posOnLane = latestRobot?.posOnLane,
          lateralOffset = latestRobot?.lateralOffset,
          velocity = latestRobot?.velocity,
          acceleration = latestRobot?.acceleration,
          posOnLaneCAM = posOnLaneAndLateralOffset.first.distanceToStart, // From Message
          lateralOffsetCAM = posOnLaneAndLateralOffset.second, // From Message
          velocityCAM = message.v, // From Message
          accelerationCAM = message.vDot, // From Message
          dataSource = DataSource.CAM, // From Message
          lane = posOnLaneAndLateralOffset.first.lane)
    }
    is Odometry ->
        return Robot(
            id = robotId,
            tickData = tickData,
            posOnLane = latestRobot?.posOnLane,
            lateralOffset = latestRobot?.lateralOffset,
            velocity = message.getVelocity(), // From Message
            acceleration = 0.0, // TODO
            posOnLaneCAM = latestRobot?.posOnLaneCAM,
            lateralOffsetCAM = latestRobot?.lateralOffsetCAM,
            velocityCAM = latestRobot?.velocityCAM,
            accelerationCAM = latestRobot?.accelerationCAM,
            dataSource = DataSource.ODOMETRY, // From Message
            lane = latestRobot?.lane)
    is ViconPose -> {
      val posOnLaneAndLateralOffset =
          calculatePosOnLaneAndLateralOffset(message.transform.translation, waypoints)
      return Robot(
          id = robotId,
          tickData = tickData,
          posOnLane = posOnLaneAndLateralOffset.first.distanceToStart, // From Message
          lateralOffset = posOnLaneAndLateralOffset.second, // From Message
          velocity = latestRobot?.velocity,
          acceleration = latestRobot?.acceleration,
          posOnLaneCAM = latestRobot?.posOnLaneCAM,
          lateralOffsetCAM = latestRobot?.lateralOffsetCAM,
          velocityCAM = latestRobot?.velocityCAM,
          accelerationCAM = latestRobot?.accelerationCAM,
          dataSource = DataSource.CAM,
          lane = posOnLaneAndLateralOffset.first.lane)
    }
  }
}

/**
 * Calculates the nearest [Waypoint] to the given [robotPosition]. The distance to the found
 * [Waypoint] is also the lateral offset.
 *
 * @param robotPosition The position as a [Vector] for the Robot for which the nearest [Waypoint]
 *   should be calculated.
 * @param waypoints All available [Waypoint]s.
 * @return A [Pair] holding the nearest [Waypoint] and the distance to it as [Double].
 */
fun calculatePosOnLaneAndLateralOffset(
    robotPosition: Vector,
    waypoints: List<Waypoint>
): Pair<Waypoint, Double> {
  check(waypoints.count() > 1) { "There have to be at least two waypoints provided." }
  val distances =
      waypoints
          .map { calculateDistance(robotPosition.x, robotPosition.y, it.x, it.y) to it }
          .sortedBy { it.first }
  val nearestWaypoint = distances.first()

  return nearestWaypoint.second to nearestWaypoint.first
}

fun calculateDistance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
  return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
}

/**
 * Traces back all currently available [ticks] and searches for the latest information of the
 * [Robot] with [robotId].
 *
 * @param ticks The [List] of [TickData] that is already processed and in which should be searched.
 * @param robotId The id of the [Robot] for which the information should be gathered.
 * @return The information of the [Robot] of identifier [robotId]. null when the [robotId] was not
 *   yet processed.
 */
fun getLatestRobotInformation(ticks: List<TickData>, robotId: Int): Robot? {
  return ticks
      .sortedByDescending { it.currentTick }
      .flatMap { it.entities }
      .firstOrNull { it.id == robotId }
}

/**
 * Returns the id of the [Robot] for the given [Message] as [Int].
 *
 * @param message The [Message] for which the [Robot] id should be returned.
 * @return The id of the [Robot] which sent the given [message].
 */
fun getRobotIdFromMessage(message: Message): Int {
  return when (message) {
    is CAM -> {
      message.robotName.replace("110", "").filter { it.isDigit() }.toInt()
    }
    is Odometry -> {
      message.header.frameId.replace("110", "").filter { it.isDigit() }.toInt()
    }
    is ViconPose -> {
      message.childFrameId.replace("110", "").filter { it.isDigit() }.toInt()
    }
  }
}
