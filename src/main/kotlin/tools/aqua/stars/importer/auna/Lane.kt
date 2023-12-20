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

package tools.aqua.stars.importer.auna

import kotlin.math.sqrt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lane(
    @SerialName("id") val id: Int,
    @SerialName("midpoints") val midpoints: List<Waypoint>,
    @SerialName("length") val length: Double,
    @SerialName("width") val width: Double = 1.4
) {
  companion object {
    operator fun invoke(id: Int, waypoints: List<Waypoint>): Lane {
      waypoints.forEachIndexed { index, waypoint ->
        if (index == 0) {
          waypoint.distanceToStart = 0.0
        } else {
          val lastWaypoint = waypoints[index - 1]
          val distance = calculateDistance(lastWaypoint, waypoint)
          waypoint.distanceToStart = lastWaypoint.distanceToStart + distance
        }
      }
      return Lane(id, waypoints, waypoints.last().distanceToStart)
    }

    private fun calculateDistance(point1: Waypoint, point2: Waypoint): Double {
      val deltaX = point1.x - point2.x
      val deltaY = point1.y - point2.y
      return sqrt(deltaX * deltaX + deltaY * deltaY)
    }
  }
}
