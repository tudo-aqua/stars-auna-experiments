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

/**
 * This class represents the [Lane] on which the [Robot]s are driving on.
 *
 * @param laneID The id of this [Lane].
 * @param length The length of the [Lane] in meters.
 * @param width The width of the [Lane] in meters.
 * @param waypoints The [List] of [Waypoint]s which represent the ideal line for this [Lane].
 * @param laneCurvature The curvature of this [Lane].
 * @param laneSegment The segment of this [Lane].
 */
data class Lane(
    val laneID: Int,
    val length: Double,
    val width: Double,
    var waypoints: List<Waypoint>,
    val laneCurvature: LaneCurvature,
    val laneSegment: LaneSegment,
) {
  lateinit var previousLane: Lane
  lateinit var nextLane: Lane

  override fun toString(): String {
    return "Lane(id=$laneID, length=$length, width=$width, waypoints=${waypoints.count()})"
  }

  enum class LaneCurvature {
    WIDE_CURVE,
    TOP_STRAIGHT,
    TIGHT_CURVE,
    BOTTOM_STRAIGHT
  }

  enum class LaneSegment {
    ENTERING,
    MIDDLE,
    LEAVING
  }
}
