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

import tools.aqua.stars.auna.experiments.ACCELERATION_ACCELERATION_WEAK_THRESHOLD
import tools.aqua.stars.auna.experiments.ACCELERATION_DECELERATION_WEAK_THRESHOLD
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

/**
 * Slicer that splits the data into segments based on acceleration thresholds
 * [ACCELERATION_ACCELERATION_WEAK_THRESHOLD] and [ACCELERATION_DECELERATION_WEAK_THRESHOLD]. Slices
 * are created overlapping such that an acceleration phase holds all values between two
 * [ACCELERATION_DECELERATION_WEAK_THRESHOLD] points and deceleration phase holds all values between
 * two [ACCELERATION_ACCELERATION_WEAK_THRESHOLD] points.
 */
class SliceAcceleration : Slicer() {

  override fun slice(ticks: List<TickData>, egoRobot: Robot): List<Segment> {
    // Determine whether to start with an acceleration or deceleration phase
    var wasAccelerating = determineFirstPhase(ticks, egoRobot.id)

    // Split ticks by acceleration threshold
    val segmentTicks = mutableListOf<List<TickData>>()
    val currentSegmentTicks = mutableListOf<TickData>()
    val iterator = ticks.listIterator()
    while (iterator.hasNext()) {
      val tickData = iterator.next()
      val currentEgoRobot = tickData.getById(egoRobot.id)

      // Find slice point
      if (slicePoint(currentEgoRobot.acceleration, wasAccelerating)) {
        // Reset tracking variables
        segmentTicks += currentSegmentTicks.toList()
        currentSegmentTicks.clear()
        wasAccelerating = !wasAccelerating

        // Rewind to next slice point
        while (iterator.hasPrevious()) {
          val previousTickData = iterator.previous()
          val previousEgoRobot = previousTickData.getById(egoRobot.id)

          if (slicePoint(previousEgoRobot.acceleration, wasAccelerating)) {
            iterator.next()
            break
          }
        }
      } else {
        currentSegmentTicks += tickData
      }
    }
    segmentTicks += currentSegmentTicks.toList()

    // Create segments
    return createSegments(segmentTicks)
  }

  private fun slicePoint(acc: Double, wasAccelerating: Boolean): Boolean =
      if (wasAccelerating) {
        acc <= ACCELERATION_DECELERATION_WEAK_THRESHOLD
      } else {
        acc >= ACCELERATION_ACCELERATION_WEAK_THRESHOLD
      }

  private fun determineFirstPhase(ticks: List<TickData>, egoId: Int): Boolean {
    for (tickData in ticks) {
      val currentEgoRobot = tickData.getById(egoId)

      if (currentEgoRobot.acceleration >= ACCELERATION_ACCELERATION_WEAK_THRESHOLD) {
        return true
      }

      if (currentEgoRobot.acceleration <= ACCELERATION_DECELERATION_WEAK_THRESHOLD) {
        return false
      }
    }
    error("No slice point found!")
  }
}
