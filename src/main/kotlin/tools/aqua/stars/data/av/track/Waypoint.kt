/*
 * Copyright 2023-2025 The STARS AuNa Experiments Authors
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
 * This class represent simple (x,y)-coordinates.
 *
 * @property x The x value for this coordinate
 * @property y The y value for this coordinate
 * @property distanceToStart The distance to the start of the [Lane]
 * @property lane The [Lane] this waypoint belongs to
 */
data class Waypoint(
    val x: Double,
    val y: Double,
    var distanceToStart: Double = -1.0,
    var lane: Lane
) {
  override fun toString(): String =
      "Waypoint(x=$x, y=$y, distanceToStart=$distanceToStart, lane=${lane})"
}
