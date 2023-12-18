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
    val posOnLane: Double = 0.0,
    val lateralOffset: Double = 0.0,
    val velocity: Double = 0.0,
    val acceleration: Double = 0.0,
    val posOnLaneCAM: Double = 0.0,
    val lateralOffsetCAM: Double = 0.0,
    val velocityCAM: Double = 0.0,
    val accelerationCAM: Double = 0.0,
    val dataSource: DataSource = DataSource.NOT_SET,
    val lane: Lane
) : EntityType<Robot, TickData, Segment>
