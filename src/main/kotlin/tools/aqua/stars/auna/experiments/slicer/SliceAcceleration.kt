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

class SliceAcceleration : AuNaSlicer() {

  /**
   * Threshold for acceleration phases. Transition from deceleration to acceleration is detected,
   * when acceleration value climbs above [ACC_THRESHOLD].
   */
  private val ACC_THRESHOLD = 2
  /**
   * Threshold for deceleration phases. Transition from acceleration to deceleration is detected,
   * when acceleration value drops below [DEC_THRESHOLD].
   */
  private val DEC_THRESHOLD = -2

  override fun slice(ticks: List<TickData>, egoRobot: Robot): List<Segment> {
    val segments: MutableList<Segment> = mutableListOf()
    var previousSegment: Segment? = null
    var wasAccelerating = true

    // Split ticks by acceleration threshold
    val currentSegmentTicks = mutableListOf<TickData>()
    val segmentTicks = mutableListOf<List<TickData>>()
    for (tickData in ticks) {
      val currentEgoRobot = tickData.entities.first { it.id == egoRobot.id }

      if (slicePoint(currentEgoRobot.acceleration, wasAccelerating)) {
        segmentTicks += currentSegmentTicks.toList()
        currentSegmentTicks.clear()
        wasAccelerating = !wasAccelerating
      }

      currentSegmentTicks += tickData
    }
    segmentTicks += currentSegmentTicks.toList()

    // Create segments
    for (segmentTickList in segmentTicks.filter { it.size >= MIN_TICKS_PER_SEGMENT }) {
      if (segmentTickList.size < MIN_TICKS_PER_SEGMENT) continue
      segments +=
          Segment(
                  segmentId = segments.size,
                  segmentSource = "",
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

  private fun slicePoint(acc: Double, wasAccelerating: Boolean): Boolean =
      if (wasAccelerating) {
        acc < DEC_THRESHOLD
      } else {
        acc > ACC_THRESHOLD
      }
}
