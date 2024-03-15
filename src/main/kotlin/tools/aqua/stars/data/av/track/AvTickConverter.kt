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
import kotlin.math.pow
import kotlin.math.sqrt
import tools.aqua.stars.auna.experiments.ACCELERATION_WINDOW_SIZE
import tools.aqua.stars.auna.importer.*

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

  messages
      .forEach { message ->
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
        val latestOtherRobotInformation =
            robotIds.minus(robotId).map { latestRobotInformationMap[it] }
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
      .also { println() }

  // Calculate Acceleration
  val tickArray = ticks.toTypedArray()
  val windowStep = ACCELERATION_WINDOW_SIZE / 2
  val start = tickArray[0].currentTick.toMillis()
  val end = tickArray.last().currentTick.toMillis()

  tickArray
      .forEachIndexed { index, tickData ->
        val currentTick = tickData.currentTick.toMillis()

        if (currentTick - windowStep <= start) return@forEachIndexed

        if (currentTick + windowStep >= end) return@forEachIndexed

        val startValue =
            tickArray[
                (index - 1 downTo 0).first { t ->
                  tickArray[t].currentTick.toMillis() <= currentTick - windowStep
                }]

        if (startValue.entities.size != 3) return@forEachIndexed

        val endValue =
            tickArray[
                (index + 1 ..< tickArray.size).first { t ->
                  tickArray[t].currentTick.toMillis() >= currentTick + windowStep
                }]

        tickData.entities.forEach {
          it.acceleration =
              1000 *
                  ((endValue.entities.first { r -> r.id == it.id }.velocity ?: 0.0) -
                      (startValue.entities.first { r -> r.id == it.id }.velocity ?: 0.0)) /
                  ACCELERATION_WINDOW_SIZE
        }
        print(
            "\rCalculated ${index+1}/${tickArray.size} average accelerations (${(index+1) * 100 / tickArray.size}%)")
      }
      .also { println("\rCalculated ${tickArray.size} average accelerations (100%)") }

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
private fun getRobotFromMessageAndLatestInformation(
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
      is AckermannDriveStamped ->
          getRobotFromMessageAndLatestInformationFromAckermannDriveStamped(
              message = message, latestRobot = latestRobot, robotId = robotId, tickData = tickData)
    }

/**
 * Returns the [Robot] based on the given [Message]. It takes the previous [Robot] state (namely
 * [latestRobot]) and the message type into consideration.
 *
 * @param message The [Message] from which the [Robot] state should be calculated from.
 * @param latestRobot The previous [Robot] state. Might be null.
 * @param robotId The id of the [Robot] which sent the [Message].
 * @param tickData The [TickData] to which the returned [Robot] belongs to.
 * @return The [Robot] object based on the given [Message].
 */
private fun getRobotFromMessageAndLatestInformationFromAckermannDriveStamped(
    message: AckermannDriveStamped,
    latestRobot: Robot?,
    robotId: Int,
    tickData: TickData
): Robot =
    Robot(
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
        steeringAngle =
            (message.ackermannDrive.steeringAngle * 360) / (2 * Math.PI), // From Message
        isPrimaryEntity = false,
    )

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
        acceleration = 0.0, /*
            (message.getVelocity() - (latestRobot?.velocity ?: 0.0)) /
                (tickData.currentTick - (latestRobot?.tickData?.currentTick ?: Zero))
                    .toDoubleValue(), // Calculated*/
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
      acceleration = 0.0, // latestRobot?.acceleration,
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
 * Returns the id of the [Robot] for the given [Message] as [Int].
 *
 * @param message The [Message] for which the [Robot] id should be returned.
 * @return The id of the [Robot] which sent the given [message].
 */
private fun getRobotIdFromMessage(message: Message): Int =
    when (message) {
          is CAM -> message.robotName
          is Odometry -> message.header.frameId
          is ViconPose -> message.childFrameId
          is AckermannDriveStamped -> message.header.frameId
        }
        .replace("110", "")
        .filter { it.isDigit() }
        .toInt()

/**
 * Calculates the nearest [Waypoint] to the given [robotPosition]. The lateral offset is calculated
 * as the distance between the robot position and the closest point on the line interval between the
 * nearest [Waypoint] and the second nearest [Waypoint].
 *
 * @param robotPosition The position as a [Vector] for the Robot for which the nearest [Waypoint]
 *   should be calculated.
 * @param waypoints All available [Waypoint]s.
 * @return A [Pair] holding the nearest [Waypoint] and the distance to it as [Double].
 */
private fun calculatePosOnLaneAndLateralOffset(
    robotPosition: Vector,
    waypoints: List<Waypoint>
): Pair<Waypoint, Double> {
  check(waypoints.count() > 1) { "There have to be at least two waypoints provided." }

  val nearestWaypoint = getNearestWaypoint(robotPosition, waypoints)

  val secondNearestWaypoint = getSecondNearestWaypoint(nearestWaypoint, robotPosition, waypoints)

  val nearestPointOnLineInterval =
      getNearestPointOnLineInterval(
          boundA = nearestWaypoint.x to nearestWaypoint.y,
          boundB = secondNearestWaypoint.x to secondNearestWaypoint.y,
          point = robotPosition.x to robotPosition.y)

  // Actual lateral offset is the distance between the robot position and the closest point on the
  // line interval
  val distance =
      getVectorDistance(
          nearestPointOnLineInterval.first to nearestPointOnLineInterval.second,
          robotPosition.x to robotPosition.y)

  return nearestWaypoint to distance
}

/**
 * Returns the nearest [Waypoint] to the given [referencePoint] from the provided [waypoints].
 *
 * @param referencePoint The [Vector] for which the nearest [Waypoint] should be calculated.
 * @param waypoints The [List] of [Waypoint]s from which the nearest [Waypoint] should be
 *   calculated.
 * @return The nearest [Waypoint] to the given [referencePoint].
 */
private fun getNearestWaypoint(referencePoint: Vector, waypoints: List<Waypoint>): Waypoint {

  check(waypoints.isNotEmpty()) { "There have to be at least one waypoint provided." }

  val dimensions = 2

  val searchPoints = waypoints.map { KdPoint(listOf(it.x, it.y)) }
  val tree = KdTree(dimensions, searchPoints)

  val solver = NNSolver(tree)

  val nearestPoint = solver.getClosestPoint(KdPoint(listOf(referencePoint.x, referencePoint.y)))
  val nearestWaypoint =
      waypoints.first { it.x == nearestPoint.values[0] && it.y == nearestPoint.values[1] }

  return nearestWaypoint
}

/**
 * Returns the second nearest [Waypoint] to the given [referencePoint] from the provided
 * [waypoints]. If the nearest [Waypoint] is the first or last [Waypoint] in the list, the second
 * nearest [Waypoint] is the first or last [Waypoint] respectively.
 *
 * @param nearestWaypoint The nearest [Waypoint] to the given [referencePoint]. If not available,
 *   the nearest [Waypoint] can be calculated using [getNearestWaypoint].
 * @param referencePoint The [Vector] for which the second nearest [Waypoint] should be calculated.
 * @param waypoints The [List] of [Waypoint]s from which the second nearest [Waypoint] should be
 *   calculated.
 * @return The second nearest [Waypoint] to the given [referencePoint].
 */
private fun getSecondNearestWaypoint(
    nearestWaypoint: Waypoint,
    referencePoint: Vector,
    waypoints: List<Waypoint>
): Waypoint {
  check(waypoints.count() > 1) { "There have to be at least two waypoints provided." }
  check(waypoints.contains(nearestWaypoint)) {
    "The nearest waypoint has to be part of the waypoints list."
  }

  val nearestWaypointIndex = waypoints.indexOf(nearestWaypoint)

  // Get second next waypoint (index + 1 or index - 1)
  val previousWaypoint =
      waypoints[
          (nearestWaypointIndex - 1 + waypoints.count()) %
              waypoints.count()] // Adding waypoints.count() to achieve wrap around
  val nextWaypoint = waypoints[(nearestWaypointIndex + 1 + waypoints.count()) % waypoints.count()]

  // Get distance to determine if previous or next waypoint is closer
  val previousWaypointDistance =
      getVectorDistance(
          (referencePoint.x to referencePoint.y), (previousWaypoint.x to previousWaypoint.y))
  val nextWaypointDistance =
      getVectorDistance((referencePoint.x to referencePoint.y), (nextWaypoint.x to nextWaypoint.y))

  return if (previousWaypointDistance <= nextWaypointDistance) previousWaypoint else nextWaypoint
}

/**
 * Returns the closest point on the line interval between [boundA] and [boundB] to the given
 * [point]. If the point is not on the line interval, the closest bound is returned.
 *
 * @param boundA The first bound of the line interval.
 * @param boundB The second bound of the line interval.
 * @param point The point for which the closest point on the line interval should be calculated.
 * @return The closest point on the line interval to the given [point].
 */
private fun getNearestPointOnLineInterval(
    boundA: Pair<Double, Double>,
    boundB: Pair<Double, Double>,
    point: Pair<Double, Double>
): Pair<Double, Double> {
  val lineAToB = (boundB.first - boundA.first) to (boundB.second - boundA.second)

  val lineAToP = (point.first - boundA.first) to (point.second - boundA.second)

  // calculate scalar from AB to AP
  val scalarProduct = (lineAToB.first * lineAToP.first) + (lineAToB.second * lineAToP.second)

  // calculate squared length of vector AB
  val abLengthSquared = (lineAToB.first * lineAToB.first) + (lineAToB.second * lineAToB.second)

  val lineProjection = scalarProduct / abLengthSquared

  return when {
    lineProjection < 0 ->
        boundA.first to boundA.second // nearest point of line is not on line -> take bound A
    lineProjection > 1 ->
        boundB.first to boundB.second // nearest point of line is not on line -> take bound B
    else ->
        boundA.first + lineProjection * lineAToB.first to
            boundA.second +
                lineProjection * lineAToB.second // nearest point is on line -> return it
  }
}

/**
 * Returns the Euclidean distance between the given [Vector]s.
 *
 * @param pointA The first Point as a [Pair] of [Double] values.
 * @param pointB The second Point as a [Pair] of [Double] values.
 * @return The Euclidean distance between [pointA] and [pointB].
 */
private fun getVectorDistance(pointA: Pair<Double, Double>, pointB: Pair<Double, Double>) =
    sqrt((pointB.first - pointA.first).pow(2) + (pointB.second - pointA.second).pow(2))
