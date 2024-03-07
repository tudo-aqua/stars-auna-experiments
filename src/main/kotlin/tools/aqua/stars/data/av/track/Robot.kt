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

import tools.aqua.stars.auna.importer.Quaternion
import tools.aqua.stars.auna.importer.Vector
import tools.aqua.stars.core.types.EntityType

/**
 * This class implements the [EntityType] and represents the main entity of the AVTrackDataClasses.
 *
 * @property id The id of the robot.
 * @property tickData The related [TickData] for which the values are valid.
 * @property posOnLane The position on the [Lane], which is the distance to the beginning of the
 *   [Lane] in meters.
 * @property lateralOffset The offset to the optimal line of the [lane]. A positive value means
 *   "right of the middle of the [lane] in driving direction". A negative value stands for "left of
 *   the middle of the [lane] in driving direction".
 * @property velocity The velocity of the Robot for the current [TickData].
 * @property acceleration The acceleration of the Robot for the current [TickData].
 * @property position The latest ViconPose transform position for the current [TickData].
 * @property rotation The latest ViconPose transform rotation for the current [TickData].
 * @property posOnLaneCAM The latest CAM position on the [Lane], which is the distance to the
 *   beginning of the [Lane] in meters.
 * @property lateralOffsetCAM The latest CAM offset to the optimal line of the [lane]. A positive
 *   value means "right of the middle of the [lane] in driving direction". A negative value stands
 *   for "left of the middle of the [lane] in driving direction".
 * @property velocityCAM The latest CAM velocity of the Robot for the current [TickData],
 * @property accelerationCAM The latest acceleration of the Robot for the current [TickData],
 * @property dataSource The [DataSource] which triggered the update to the values of the Robot for
 *   the current [TickData],
 * @property lane The [Lane] on which the Robot is currently driving on,
 * @property isPrimaryEntity Whether the [Robot] should be classified as the 'primary entity'.
 * @property steeringAngle The current steering angle of the [Robot] in degrees.
 */
data class Robot(
    override val id: Int,
    override val tickData: TickData,
    val posOnLane: Double? = 0.0,
    val lateralOffset: Double? = 0.0,
    val velocity: Double? = 0.0,
    val acceleration: Double? = 0.0,
    val position: Vector? = Vector(0.0, 0.0, 0.0),
    val rotation: Quaternion? = Quaternion(0.0, 0.0, 0.0, 0.0),
    val posOnLaneCAM: Double? = 0.0,
    val lateralOffsetCAM: Double? = 0.0,
    val velocityCAM: Double? = 0.0,
    val accelerationCAM: Double? = 0.0,
    val dataSource: DataSource = DataSource.NOT_SET,
    val lane: Lane?,
    var isPrimaryEntity: Boolean,
    val steeringAngle: Double? = 0.0,
) : EntityType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference> {

  /**
   * Clones the current [Robot] to the given [newTickData].
   *
   * @return The cloned [Robot] that is associated with the given [newTickData].
   */
  fun copyToNewTick(newTickData: TickData): Robot =
      Robot(
          id = this.id,
          tickData = newTickData,
          posOnLane = this.posOnLane,
          lateralOffset = this.lateralOffset,
          velocity = this.velocity,
          acceleration = this.acceleration,
          position = this.position,
          rotation = this.rotation,
          posOnLaneCAM = this.posOnLaneCAM,
          lateralOffsetCAM = this.lateralOffsetCAM,
          velocityCAM = this.velocityCAM,
          accelerationCAM = this.accelerationCAM,
          dataSource = this.dataSource,
          lane = this.lane,
          isPrimaryEntity = this.isPrimaryEntity,
          steeringAngle = this.steeringAngle)

  override fun toString(): String = "Robot(id=$id)"
}
