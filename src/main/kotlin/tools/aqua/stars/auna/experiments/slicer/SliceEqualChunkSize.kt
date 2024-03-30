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

import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

class SliceEqualChunkSize : Slicer() {
  override fun slice(ticks: List<TickData>, egoRobot: Robot): List<Segment> {
    // Split ticks by line change
    var currentLane = egoRobot.lane
    val currentSegmentTicks = mutableListOf<TickData>()
    val segmentTicks = mutableListOf<List<TickData>>()

    // Split track on lane changes and the chunk those segments into [SEGMENTS_PER_LANE] evenly
    // spaced Segments.
    for (tickData in ticks) {
      val currentEgoRobot = tickData.getById(egoRobot.id)
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

    return createSegments(segmentTicks)
  }
}
