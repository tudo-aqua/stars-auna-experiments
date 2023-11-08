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

import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.core.types.TickDataType

/**
 * This class implements the [EntityType] and represents the main entity of the AVTrackDataClasses.
 *
 * @param id The id of the robot
 * @param tickData The related [TickData] for which the values are valid
 * @param posOnLane The position on the [Lane], which is the distance to the beginning of the [Lane]
 *   in meters
 * @param lateralOffset The offset to the optimal line of the [lane]. A positive value means "right
 *   of the middle of the [lane] in driving direction". A negative value stands for "left of the
 *   middle of the [lane] in driving direction".
 * @param velocity The velocity of the Robot for the current [TickData]
 * @param acceleration The acceleration of the Robot for the current [TickData]
 * @param posOnLaneCAM The latest CAM position on the [Lane], which is the distance to the beginning
 *   of the [Lane] in meters
 * @param lateralOffsetCAM The latest CAM offset to the optimal line of the [lane]. A positive value
 *   means "right of the middle of the [lane] in driving direction". A negative value stands for
 *   "left of the middle of the [lane] in driving direction".
 * @param velocityCAM The latest CAM velocity of the Robot for the current [TickData]
 * @param accelerationCAM The latest acceleration of the Robot for the current [TickData]
 * @param dataSource The [DataSource] which triggered the update to the values of the Robot for the
 *   current [TickData]
 * @param lane The [Lane] on which the Robot is currently driving on
 */
data class Robot(
    override val id: Int,
    override val tickData: TickData,
    val posOnLane: Double,
    val lateralOffset: Double,
    val velocity: Double,
    val acceleration: Double,
    val posOnLaneCAM: Double,
    val lateralOffsetCAM: Double,
    val velocityCAM: Double,
    val accelerationCAM: Double,
    val dataSource: DataSource,
    val lane: Lane
) : EntityType<Robot, TickData, Segment>

/**
 * This class implements the [TickDataType] and holds a specific timestamp (see [currentTick]) and
 * the states of all [EntityType]s (see [Robot]) at the timestamp.
 *
 * @param currentTick The current timestamp in seconds
 * @param entities The [List] of [Robot]s for the [currentTick]
 */
data class TickData(override val currentTick: Double, override var entities: List<Robot>) :
    TickDataType<Robot, TickData, Segment> {
  /** Holds a reference to the [Segment] in which this [TickData] is included and analyzed */
  override lateinit var segment: Segment
}

/**
 * This class implements the [SegmentType] and holds the sliced analysis data in semantic segments.
 *
 * @param segmentSource Specifies the file from which the data of this [Segment] comes from
 * @param tickData The [List] of [TickData]s relevant for the [Segment]
 */
data class Segment(override val segmentSource: String, override val tickData: List<TickData>) :
    SegmentType<Robot, TickData, Segment> {
  /** Holds a [Map] which maps a timestamp to all relevant [TickData]s (based on [tickData]) */
  override val ticks: Map<Double, TickData> = tickData.associateBy { it.currentTick }
  /** Holds a [List] of all available timestamps in this [Segment] (based on [tickData]) */
  override val tickIDs: List<Double> = tickData.map { it.currentTick }
  /** Holds the first timestamp for this [Segment] */
  override val firstTickId: Double = this.tickIDs.first()
  /** Holds the id of the primary entity for this [Segment] */
  override val primaryEntityId: Int
    get() {
      require(tickData.any()) {
        "There is no TickData provided! Cannot get primaryEntityId of for this Segment."
      }
      require(tickData.first().entities.any()) {
        "There is no Entity in the first TickData. Cannot get primaryEntityId for this Segment"
      }
      val firstEgo = tickData.first().entities.first()
      return firstEgo.id
    }
}

/**
 * This class represents the [Lane] on which the [Robot]s are driving on.
 *
 * @param length The length of the [Lane] in meters
 * @param width The width of the [Lane] in meters
 * @param waypoints The [List] of [Waypoint]s which represent the ideal line for this [Lane]
 */
data class Lane(val length: Double, val width: Double, val waypoints: List<Waypoint>)

/**
 * This class represent simple (x,y)-coordinates
 *
 * @param x The x value for this coordinate
 * @param y The y value for this coordinate
 */
data class Waypoint(val x: Double, val y: Double)

/** This enum holds all possible triggers for new value updates of the [Robot]s. */
enum class DataSource() {
  VICON_POSE,
  ODOMETRY,
  CAM
}
