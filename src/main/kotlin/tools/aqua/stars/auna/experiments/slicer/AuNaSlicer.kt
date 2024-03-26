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

import tools.aqua.stars.core.evaluation.Slicer
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

abstract class AuNaSlicer : Slicer<TickData, Segment> {
  override fun slice(tickSource: String, ticks: List<TickData>): Sequence<Segment> {
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
      ct[0]
          .entities
          .map { robot ->
            val copiedTicks =
                ct.map {
                  it.clone().also { t ->
                    t.entities.first { e -> e.id == robot.id }.isPrimaryEntity = true
                  }
                }
            slice(copiedTicks, robot)
          }
          .flatten()
          .asSequence()
    }
  }

  abstract fun slice(ticks: List<TickData>, egoRobot: Robot): List<Segment>
}
