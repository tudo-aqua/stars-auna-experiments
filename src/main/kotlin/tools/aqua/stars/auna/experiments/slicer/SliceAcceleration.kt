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

import tools.aqua.stars.auna.experiments.*
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

class SliceAcceleration : Slicer() {

  override fun slice(ticks: List<TickData>, egoRobot: Robot): List<Segment> {
    var wasAccelerating = false

    // Split ticks by acceleration threshold
    val currentSegmentTicks = mutableListOf<TickData>()
    val segmentTicks = mutableListOf<List<TickData>>()

    var i = 0
    val iterator = ticks.listIterator()
    while (iterator.hasNext()) {
      val tickData = iterator.next()
      i++
      val currentEgoRobot = tickData.entities.first { it.id == egoRobot.id }

      val acc = currentEgoRobot.acceleration
      if (slicePoint(currentEgoRobot.acceleration, wasAccelerating)) {
        segmentTicks += currentSegmentTicks.toList()

        println("\rFound slicing point at $i.")

        currentSegmentTicks.clear()
        wasAccelerating = !wasAccelerating

        while (iterator.hasPrevious()) {
          val previousTickData = iterator.previous()
          i--
          val previousEgoRobot = previousTickData.entities.first { it.id == egoRobot.id }
          val acc = previousEgoRobot.acceleration
          if (slicePoint(previousEgoRobot.acceleration, wasAccelerating)) {
            println("Rewinded to $i")
            break
          }
        }

        if (!iterator.hasPrevious()) {
          println("Rewinded to start")
        }
      }

      print("\rFound ${segmentTicks.size} slicing points; Currently at ${tickData.currentTick.seconds}. $wasAccelerating")
      currentSegmentTicks += tickData
    }
    segmentTicks += currentSegmentTicks.toList()

    // Create segments
    var previousSegment: Segment? = null
    val segments: MutableList<Segment> = mutableListOf()
    for (segmentTickList in segmentTicks.filter { it.size >= MIN_TICKS_PER_SEGMENT }) {
      segments +=
          Segment(
                  segmentId = segments.size,
                  ticks = segmentTickList.associateBy { it.currentTick },
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

  private fun slicePoint(acc: Double, wasAccelerating: Boolean): Boolean =
      if (wasAccelerating) {
        acc < ACCELERATION_DECELERATION_STRONG_THRESHOLD
      } else {
        acc > ACCELERATION_ACCELERATION_STRONG_THRESHOLD
      }
}
