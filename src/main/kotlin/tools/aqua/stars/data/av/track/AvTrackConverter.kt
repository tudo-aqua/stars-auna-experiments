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
import tools.aqua.stars.auna.experiments.MIN_TICKS_PER_SEGMENT
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
                      isStraight = index % 2 != 0)
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
      ticks.filter { it.entities.count() == 3 && it.entities.all { t -> t.lane.laneID >= 0 } }

  check(cleanedTicks.any()) { "There is no TickData provided!" }
  check(cleanedTicks[0].entities.size == 3) {
    "The first Tick does not contain exactly 3 entities!"
  }
  check(
      cleanedTicks[0].entities[0].lane == cleanedTicks[0].entities[1].lane &&
          cleanedTicks[0].entities[1].lane == cleanedTicks[0].entities[2].lane) {
        "The entities do not start on the same lane!"
      }

  // Multiply segment for all robots as ego
  return cleanedTicks[0]
      .entities
      .map { egoRobot ->
        // Copy TickData for every robot as ego and set the isPrimaryEntity flag
        val copiedTicks =
            cleanedTicks.map {
              it.clone().also { t ->
                t.entities.first { e -> e.id == egoRobot.id }.isPrimaryEntity = true
              }
            }

        // Split ticks by lane change
        val splittedTicks = splitTicksByLineChange(copiedTicks, egoRobot)

        // Create segments from ticks
        val segments = createSegmentsFromTicks(sourceFile, splittedTicks)

        segments
      }
      .flatten()
}

/**
 * Wraps the [List] of [TickData] into [Segment]s.
 *
 * @param sourceFile The file from which the [TickData] was loaded.
 * @param ticks The [List] of [TickData].
 * @return [List] of [Segment]s based on the given [List] of [TickData].
 */
fun segmentTicksToIncludeWholeDrive(sourceFile: String, ticks: List<TickData>): List<Segment> {
  // As the messages are not synchronized for the robots, there are some ticks, where only 1, or 2
  // robots are tracked. For the analysis we only want the ticks in which all three robots are
  // tracked.

  val cleanedTicks =
      ticks.filter { it.entities.count() == 3 && it.entities.all { t -> t.lane.laneID >= 0 } }

  check(cleanedTicks.any()) { "There is no TickData provided!" }
  check(cleanedTicks[0].entities.size == 3) {
    "The first Tick does not contain exactly 3 entities!"
  }
  check(
      cleanedTicks[0].entities[0].lane == cleanedTicks[0].entities[1].lane &&
          cleanedTicks[0].entities[1].lane == cleanedTicks[0].entities[2].lane) {
        "The entities do not start on the same lane!"
      }

  // Multiply segment for all robots as ego
  return cleanedTicks[0].entities.map { egoRobot ->
    // Copy TickData for every robot as ego and set the isPrimaryEntity flag
    val copiedTicks =
        cleanedTicks.map {
          it.clone().also { t ->
            t.entities.first { e -> e.id == egoRobot.id }.isPrimaryEntity = true
          }
        }
    Segment(
        segmentId = egoRobot.id,
        segmentSource = sourceFile,
        ticks = copiedTicks.associateBy { it.currentTick },
        previousSegment = null,
        nextSegment = null,
    )
  }
}

private fun splitTicksByLineChange(
    cleanedTicks: List<TickData>,
    egoRobot: Robot
): List<List<TickData>> {
  var currentLane = egoRobot.lane
  val currentSegmentTicks = mutableListOf<TickData>()
  val segmentTicks = mutableListOf<List<TickData>>()

  // Split track on lane changes and the chunk those segments into [SEGMENTS_PER_LANE] evenly spaced
  // [Segment]s.
  for (tickData in cleanedTicks) {
    val currentEgoRobot = tickData.entities.first { it.id == egoRobot.id }
    val newLane = currentEgoRobot.lane

    // The ego robot is still on the same lane.
    if (currentLane == newLane) {
      currentSegmentTicks += tickData
      continue
    }

    // Reset tracking variables
    currentLane = newLane
    segmentTicks += currentSegmentTicks.toList()
    currentSegmentTicks.clear()
  }
  segmentTicks += currentSegmentTicks.toList()

  return segmentTicks.filter { it.size >= MIN_TICKS_PER_SEGMENT }
}

private fun createSegmentsFromTicks(
    sourceFile: String,
    chunkedTicks: List<List<TickData>>
): List<Segment> {
  val segments: MutableList<Segment> = mutableListOf()
  var previousSegment: Segment? = null
  for (segmentTickList in chunkedTicks) {
    if (segmentTickList.size < MIN_TICKS_PER_SEGMENT) continue
    segments +=
        Segment(
                segmentId = segments.size,
                segmentSource = sourceFile,
                ticks = segmentTickList.associateBy { it.currentTick },
                previousSegment = previousSegment,
                nextSegment = null)
            .also { segment ->
              segment.tickData.forEach { it.segment = segment }
              previousSegment = segment
            }
  }

  return segments
}
