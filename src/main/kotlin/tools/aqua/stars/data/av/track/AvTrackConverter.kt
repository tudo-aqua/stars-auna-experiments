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

import kotlin.math.ceil
import tools.aqua.stars.auna.importer.Track

/**
 * Converts the given serialized [Track] into a [List] of [Lane]s. Splits each track segment into
 * [segmentsPerLane] segments.
 *
 * @param track The [Track] that should be converted.
 * @param segmentsPerLane The number of segments per track segment.
 * @return The converted [List] of [Lane]s.
 */
fun convertTrackToLanes(track: Track, segmentsPerLane: Int): List<Lane> {
  var previousLane: Lane? = null

  return track.lanes
      .mapIndexed { index, lane ->
        lane.waypoints
            .chunked(ceil(lane.waypoints.size / segmentsPerLane.toDouble()).toInt())
            .mapIndexed { index2, wp ->
              Lane(
                      laneID = index * segmentsPerLane + index2,
                      length = lane.length,
                      width = lane.width,
                      waypoints = listOf(),
                      laneCurvature =
                          when (index % 4) {
                            0 -> Lane.LaneCurvature.WIDE_CURVE
                            1 -> Lane.LaneCurvature.TOP_STRAIGHT
                            2 -> Lane.LaneCurvature.TIGHT_CURVE
                            3 -> Lane.LaneCurvature.BOTTOM_STRAIGHT
                            else -> error("")
                          },
                      laneSegment =
                          when (index2) {
                            0 -> Lane.LaneSegment.ENTERING
                            1 -> Lane.LaneSegment.MIDDLE
                            2 -> Lane.LaneSegment.LEAVING
                            else -> error("")
                          })
                  .also { l ->
                    l.waypoints =
                        wp.map { wp ->
                          Waypoint(
                              x = wp.x, y = wp.y, lane = l, distanceToStart = wp.distanceToStart)
                        }
                    previousLane?.let {
                      l.previousLane = it
                      it.nextLane = l
                    }
                    previousLane = l
                  }
            }
      }
      .flatten()
      .also {
        // Let's close the circle
        it.first().previousLane = it.last()
        it.last().nextLane = it.first()
      }
}

/**
 * Converts the given serialized [Track] into a single [Lane].
 *
 * @param track The [Track] that should be converted.
 * @return The converted [Lane].
 */
fun convertTrackToSingleLane(track: Track): Lane {
  val newLane =
      Lane(
          laneID = 0,
          laneCurvature = Lane.LaneCurvature.BOTTOM_STRAIGHT,
          laneSegment = Lane.LaneSegment.ENTERING,
          width = track.lanes[0].width,
          length = track.lanes[0].length,
          waypoints = listOf())
  newLane.waypoints =
      track.lanes
          .map { it.waypoints.map { wp -> Waypoint(wp.x, wp.y, wp.distanceToStart, newLane) } }
          .flatten()
          .toMutableList()
          .also { it.addAll(it.subList(0, 10).map { it.copy() }) }
  return newLane
}
