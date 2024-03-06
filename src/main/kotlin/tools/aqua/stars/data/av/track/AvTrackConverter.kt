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

package tools.aqua.stars.data.av.track

import de.sciss.kdtree.KdPoint
import de.sciss.kdtree.KdTree
import de.sciss.kdtree.NNSolver
import tools.aqua.stars.data.av.track.AuNaTimeUnit.Companion.Zero
import tools.aqua.stars.importer.auna.*

/**
 * Converts the given serialized [Track] into a [List] of [Lane]s.
 *
 * @param track The [Track] that should be converted.
 * @return The converted [List] of [Lane]s.
 */
fun convertTrackToLanes(track: Track): List<Lane> {
  val lanes = mutableListOf<Lane>()
  track.lanes.forEachIndexed { index, lane ->
    val newLane =
        Lane(
            length = lane.length,
            width = lane.width,
            waypoints = listOf(),
            isStraight = index % 2 == 0)
    val waypoints =
        lane.waypoints.map { wp ->
          Waypoint(x = wp.x, y = wp.y, lane = newLane, distanceToStart = wp.distanceToStart)
        }
    newLane.waypoints = waypoints
    lanes += newLane
  }
  return lanes
}

/**
 * Slices the [List] of [TickData] into [Segment]s based on the [Lane] of the leading [Robot].
 *
 * @param sourceFile The file from which the [TickData] was loaded.
 * @param ticks The [List] of [TickData].
 * @return [List] of [Segment]s based on the given [List] of [TickData].
 */
fun segmentTicksIntoSegments(sourceFile: String, ticks: List<TickData>): List<Segment> {
  // As the messages are not synchronized for the robots, there are some ticks, where only 1, or 2
  // robots are tracked. For the analysis we only want the ticks in which all three robots are
  // tracked.
  val cleanedTicks =
      ticks.filter { it.entities.count() == 3 && it.entities.all { t -> t.lane != null } }
  check(cleanedTicks.any()) { "There is no TickData provided!" }
  check(cleanedTicks[0].entities.size == 3) {
    "The first Tick does not contain exactly 3 entities!"
  }
  check(
      cleanedTicks[0].entities[0].lane == cleanedTicks[0].entities[1].lane &&
          cleanedTicks[0].entities[1].lane == cleanedTicks[0].entities[2].lane) {
        "The entities do not start on the same lane!"
      }
  // Calculate the leading robot by getting the maximum posOnLane property (meaning, the robot is
  // furthest ahead)
  val leadingRobot = cleanedTicks[0].entities.maxBy { it.posOnLane ?: -1.0 }

  var currentLane = leadingRobot.lane
  val currentSegmentTicks = mutableListOf<TickData>()
  val segments = mutableListOf<Segment>()
  cleanedTicks.forEach { tickData ->
    val currentLeadingRobot = tickData.entities.first { it.id == leadingRobot.id }
    // The leading robot is still on the same lane.
    if (currentLeadingRobot.lane == currentLane) {
      currentSegmentTicks += tickData
    } else {
      if (currentSegmentTicks.size > 0) {
        // The leading robot switched lanes. Add previous ticks as segment to list.
        val newSegment =
            Segment(segments.size, sourceFile, currentSegmentTicks.toList()).also { segment ->
              segment.tickData.forEach { it.segment = segment }
            }
        segments += newSegment
      }
      // Reset tracking variables
      currentLane = currentLeadingRobot.lane
      currentSegmentTicks.clear()
      currentSegmentTicks += tickData
    }
  }
  // Add remaining ticks to segments.
  val newSegment =
      Segment(segments.size, sourceFile, currentSegmentTicks.toList()).also { segment ->
        segment.tickData.forEach { it.segment = segment }
      }
  segments += newSegment

  return segments.filter { it.tickData.size > 10 }.map { it.getPrimaryEntityClones() }.flatten()
}

/**
 * Returns a [List] of [TickData] based on the given [List] of [Message]s.
 *
 * @param messages The [List] of [Message]s. Each [Message] results in a new [TickData].
 * @param waypoints A [List] of all available [Waypoint]s. It is used internally to calculate the
 *   related [Lane] and nearest [Waypoint] for each [Robot].
 */
fun getTicksFromMessages(messages: List<Message>, waypoints: List<Waypoint>): List<TickData> {
  val ticks = mutableListOf<TickData>()
  val robotIds = mutableSetOf<Int>()
  val latestRobotInformationMap: MutableMap<Int, Robot?> = mutableMapOf()
  messages.forEach { message ->
    // Create caching list for newly created Robot objects
    val robots = mutableListOf<Robot>()
    // Create empty TickData object for later reference
    val tickData =
        TickData(
            currentTick = message.header.timeStamp.toAuNaTimeUnit(), entities = mutableListOf())
    // Get the robot id for the current message
    val robotId = getRobotIdFromMessage(message)
    // Save robot id
    robotIds.add(robotId)
    // Get the latest information for all other robots to be copied later on
    val latestOtherRobotInformation = robotIds.minus(robotId).map { latestRobotInformationMap[it] }
    // Copy the latest information of all other robots with the current tickData
    val otherRobotInformationCopy =
        latestOtherRobotInformation.mapNotNull { it?.copyToNewTick(tickData) }
    // Save copy in caching list
    robots.addAll(otherRobotInformationCopy)

    // Get the latest information for the robot that sent the current message
    val latestRobotInformation = latestRobotInformationMap[robotId]
    // Get Robot information form current message
    val currentRobot =
        getRobotFromMessageAndLatestInformation(
            message, latestRobotInformation, robotId, tickData, waypoints)
    latestRobotInformationMap[robotId] = currentRobot
    // Add current Robot information to robot cache list
    robots.add(currentRobot)

    // Update entities of tickData to the caching list of robots
    tickData.entities = robots
    ticks += tickData
    print(
        "\rCalculated ${ticks.count()}/${messages.count()} ticks (${(ticks.count() * 100) / messages.count()}%)")
  }
  println()
  return ticks
}

/**
 * Returns the [Robot] based on the given [Message]. It takes the previous [Robot] state (namely
 * [latestRobot]) and the message type into consideration.
 *
 * @param message The [Message] from which the [Robot] state should be calculated from.
 * @param latestRobot The previous [Robot] state. Might be null.
 * @param robotId The id of the [Robot] which sent the [Message].
 * @param tickData The [TickData] to which the returned [Robot] belongs to.
 * @param waypoints A [List] of [Waypoint]s. It is used to calculate the related [Lane] and nearest
 *   [Waypoint].
 * @return The [Robot] object based on the given [Message].
 */
fun getRobotFromMessageAndLatestInformation(
    message: Message,
    latestRobot: Robot?,
    robotId: Int,
    tickData: TickData,
    waypoints: List<Waypoint>
): Robot =
    when (message) {
      is CAM ->
          getRobotFromMessageAndLatestInformationFromCAM(
              message = message,
              latestRobot = latestRobot,
              robotId = robotId,
              tickData = tickData,
              waypoints = waypoints)
      is Odometry ->
          getRobotFromMessageAndLatestInformationFromOdometry(
              message = message, latestRobot = latestRobot, robotId = robotId, tickData = tickData)
      is ViconPose ->
          getRobotFromMessageAndLatestInformationFromViconPose(
              message = message,
              latestRobot = latestRobot,
              robotId = robotId,
              tickData = tickData,
              waypoints = waypoints)
      is tools.aqua.stars.auna.importer.AckermannDriveStamped ->
          getRobotFromMessageAndLatestInformationFromAckermannDriveStamped(
              message = message, latestRobot = latestRobot, robotId = robotId, tickData = tickData)
    }

fun getRobotFromMessageAndLatestInformationFromAckermannDriveStamped(
  message: tools.aqua.stars.auna.importer.AckermannDriveStamped,
  latestRobot: Robot?,
  robotId: Int,
  tickData: TickData
): Robot {
  return Robot(
      id = robotId,
      tickData = tickData,
      posOnLane = latestRobot?.posOnLane,
      lateralOffset = latestRobot?.lateralOffset,
      velocity = latestRobot?.velocity,
      acceleration = latestRobot?.acceleration,
      position = latestRobot?.position,
      rotation = latestRobot?.rotation,
      posOnLaneCAM = latestRobot?.posOnLaneCAM,
      lateralOffsetCAM = latestRobot?.lateralOffsetCAM,
      velocityCAM = latestRobot?.velocityCAM,
      accelerationCAM = latestRobot?.accelerationCAM, // From Message? //TODO Check values
      dataSource = DataSource.ACKERMANN_CMD, // From Message
      lane = latestRobot?.lane,
      steeringAngle = (message.ackermannDrive.steeringAngle * 360) / (2 * Math.PI), // From Message
      isPrimaryEntity = false,
  )
}

/**
 * Returns the [Robot] based on the given [CAM] [Message]. It takes the previous [Robot] state
 * (namely [latestRobot]) and the message type into consideration.
 *
 * @param message The [Message] from which the [Robot] state should be calculated from.
 * @param latestRobot The previous [Robot] state. Might be null.
 * @param robotId The id of the [Robot] which sent the [Message].
 * @param tickData The [TickData] to which the returned [Robot] belongs to.
 * @param waypoints A [List] of [Waypoint]s. It is used to calculate the related [Lane] and nearest
 *   [Waypoint].
 * @return The [Robot] object based on the given [CAM] [Message].
 */
private fun getRobotFromMessageAndLatestInformationFromCAM(
    message: CAM,
    latestRobot: Robot?,
    robotId: Int,
    tickData: TickData,
    waypoints: List<Waypoint>
): Robot {
  val posOnLaneAndLateralOffset =
      calculatePosOnLaneAndLateralOffset(Vector(message.x, message.y, message.z), waypoints)
  return Robot(
      id = robotId,
      tickData = tickData,
      posOnLane = latestRobot?.posOnLane,
      lateralOffset = latestRobot?.lateralOffset,
      velocity = latestRobot?.velocity,
      acceleration = latestRobot?.acceleration,
      position = latestRobot?.position,
      rotation = latestRobot?.rotation,
      posOnLaneCAM = posOnLaneAndLateralOffset.first.distanceToStart, // From Message
      lateralOffsetCAM = posOnLaneAndLateralOffset.second, // From Message
      velocityCAM = message.v, // From Message
      accelerationCAM = message.vDot, // From Message
      dataSource = DataSource.CAM, // From Message
      lane = posOnLaneAndLateralOffset.first.lane,
      steeringAngle = latestRobot?.steeringAngle,
      isPrimaryEntity = false)
}

/**
 * Returns the [Robot] based on the given [Odometry] [Message]. It takes the previous [Robot] state
 * (namely [latestRobot]) and the message type into consideration.
 *
 * @param message The [Message] from which the [Robot] state should be calculated from.
 * @param latestRobot The previous [Robot] state. Might be null.
 * @param robotId The id of the [Robot] which sent the [Message].
 * @param tickData The [TickData] to which the returned [Robot] belongs to.
 * @return The [Robot] object based on the given [Odometry] [Message].
 */
private fun getRobotFromMessageAndLatestInformationFromOdometry(
    message: Odometry,
    latestRobot: Robot?,
    robotId: Int,
    tickData: TickData
): Robot =
    Robot(
        id = robotId,
        tickData = tickData,
        posOnLane = latestRobot?.posOnLane,
        lateralOffset = latestRobot?.lateralOffset,
        velocity = message.getVelocity(), // From Message
        acceleration =
            (message.getVelocity() - (latestRobot?.velocity ?: 0.0)) /
                (tickData.currentTick - (latestRobot?.tickData?.currentTick ?: Zero))
                    .toDoubleValue(), // Calculated
        position = latestRobot?.position,
        rotation = latestRobot?.rotation,
        posOnLaneCAM = latestRobot?.posOnLaneCAM,
        lateralOffsetCAM = latestRobot?.lateralOffsetCAM,
        velocityCAM = latestRobot?.velocityCAM,
        accelerationCAM = latestRobot?.accelerationCAM,
        dataSource = DataSource.ODOMETRY, // From Message
        lane = latestRobot?.lane,
        steeringAngle = latestRobot?.steeringAngle,
        isPrimaryEntity = false)

/**
 * Returns the [Robot] based on the given [ViconPose] [Message]. It takes the previous [Robot] state
 * (namely [latestRobot]) and the message type into consideration.
 *
 * @param message The [Message] from which the [Robot] state should be calculated from.
 * @param latestRobot The previous [Robot] state. Might be null.
 * @param robotId The id of the [Robot] which sent the [Message].
 * @param tickData The [TickData] to which the returned [Robot] belongs to.
 * @param waypoints A [List] of [Waypoint]s. It is used to calculate the related [Lane] and nearest
 *   [Waypoint].
 * @return The [Robot] object based on the given [ViconPose] [Message].
 */
private fun getRobotFromMessageAndLatestInformationFromViconPose(
    message: ViconPose,
    latestRobot: Robot?,
    robotId: Int,
    tickData: TickData,
    waypoints: List<Waypoint>
): Robot {
  val posOnLaneAndLateralOffset =
      calculatePosOnLaneAndLateralOffset(message.transform.translation, waypoints)
  return Robot(
      id = robotId,
      tickData = tickData,
      posOnLane = posOnLaneAndLateralOffset.first.distanceToStart, // From Message
      lateralOffset = posOnLaneAndLateralOffset.second, // From Message
      velocity = latestRobot?.velocity,
      acceleration = latestRobot?.acceleration,
      position = message.transform.translation, // From Message
      rotation = message.transform.rotation, // From Message
      posOnLaneCAM = latestRobot?.posOnLaneCAM,
      lateralOffsetCAM = latestRobot?.lateralOffsetCAM,
      velocityCAM = latestRobot?.velocityCAM,
      accelerationCAM = latestRobot?.accelerationCAM,
      dataSource = DataSource.VICON_POSE,
      lane = posOnLaneAndLateralOffset.first.lane, // From Message
      steeringAngle = latestRobot?.steeringAngle,
      isPrimaryEntity = false)
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
  val dimensions = 2

  val searchPoints = waypoints.map { KdPoint(listOf(it.x, it.y)) }
  val tree = KdTree(dimensions, searchPoints)

  val solver = NNSolver(tree)

  val nearestPoint = solver.getClosestPoint(KdPoint(listOf(robotPosition.x, robotPosition.y)))
  val nearestWaypoint =
      waypoints.first { it.x == nearestPoint.values[0] && it.y == nearestPoint.values[1] }

  return nearestWaypoint to
      nearestPoint.getDistanceSquared(KdPoint(listOf(robotPosition.x, robotPosition.y)))
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
    is tools.aqua.stars.auna.importer.AckermannDriveStamped -> {
      message.header.frameId.replace("110", "").filter { it.isDigit() }.toInt()
    }
  }
}
