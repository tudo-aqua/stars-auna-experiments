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
 * @param length The length of the [Lane] in meters
 * @param width The width of the [Lane] in meters
 * @param waypoints The [List] of [Waypoint]s which represent the ideal line for this [Lane]
 */
data class Lane(val length: Double, val width: Double, var waypoints: List<Waypoint>) {
  override fun toString(): String {
    return "Lane(length=$length, width=$width, waypoints=${waypoints.count()})"
  }
}
