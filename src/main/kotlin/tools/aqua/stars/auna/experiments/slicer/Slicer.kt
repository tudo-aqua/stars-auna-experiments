/*
 * Copyright 2024 The STARS AuNa Experiments Authors
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

package tools.aqua.stars.auna.experiments.slicer

import tools.aqua.stars.auna.experiments.MIN_TICKS_PER_SEGMENT
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

/** Abstract slicer. Provides functions for implementations of this class. */
abstract class Slicer {
  /**
   * Slices ticks into segments.
   *
   * @param ticks The ticks to slice.
   * @param entityIdFilter The entity IDs to filter for. If null, all entities are considered.
   */
  fun slice(ticks: List<TickData>, entityIdFilter: List<Int>? = null): Sequence<Segment> {
    // As the messages are not synchronized for the robots, there are some ticks, where only 1, or 2
    // robots are tracked. For the analysis we only want the ticks in which all three robots are
    // tracked.

    val cleanedTicks =
        ticks.filter { it.entities.count() == 3 && it.entities.all { t -> t.lane.laneID >= 0 } }

    check(cleanedTicks.any()) { "There is no TickData provided!" }
    check(
        cleanedTicks[0].entities[0].lane == cleanedTicks[0].entities[1].lane &&
            cleanedTicks[0].entities[1].lane == cleanedTicks[0].entities[2].lane) {
          "The entities do not start on the same lane!"
        }

    return cleanedTicks.let { ct ->
      val filter = entityIdFilter ?: ct[0].entities.map { it.id }

      ct[0]
          .entities
          .filter { filter.contains(it.id) }
          .map { robot ->
            val copiedTicks =
                ct.map { it.clone().also { t -> t.getById(robot.id).isPrimaryEntity = true } }
            slice(copiedTicks, robot)
          }
          .flatten()
          .asSequence()
    }
  }

  /** Creates segment object from [segmentTicks]. */
  fun createSegments(segmentTicks: List<List<TickData>>): List<Segment> {
    val segments: MutableList<Segment> = mutableListOf()
    var previousSegment: Segment? = null

    for (segmentTickList in segmentTicks.filter { it.size >= MIN_TICKS_PER_SEGMENT }) {
      if (segmentTickList.size < MIN_TICKS_PER_SEGMENT) continue
      segments +=
          Segment(
                  segmentId = segments.size,
                  ticks = segmentTickList.map { it.clone() }.associateBy { it.currentTick },
                  previousSegment = previousSegment,
                  nextSegment = null)
              .also { segment ->
                segment.tickData.forEach { it.segment = segment }
                previousSegment = segment
              }
    }

    segments.last().nextSegment = segments.first()
    segments.first().previousSegment = segments.last()

    return segments
  }

  /** Slices ticks into segments. */
  abstract fun slice(ticks: List<TickData>, egoRobot: Robot): List<Segment>
}
