/*
 * Copyright 2023 The STARS AuNa Experiments Authors
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

import tools.aqua.stars.core.types.SegmentType

/**
 * This class implements the [SegmentType] and holds the sliced analysis data in semantic segments.
 *
 * @param segmentSource Specifies the file from which the data of this [Segment] comes from
 * @param tickData The [List] of [TickData]s relevant for the [Segment]
 */
data class Segment(override val segmentSource: String, override val tickData: List<TickData>) :
    SegmentType<Robot, TickData, Segment> {
  /** Holds a [Map] which maps a timestamp to all relevant [TickData]s (based on [tickData]) */
  override val ticks: Map<Double, TickData> = tickData.associateBy { it.currentTick }
  /** Holds a [List] of all available timestamps in this [Segment] (based on [tickData]) */
  override val tickIDs: List<Double> = tickData.map { it.currentTick }
  /** Holds the first timestamp for this [Segment] */
  override val firstTickId: Double = this.tickIDs.first()
  /** Holds the id of the primary entity for this [Segment] */
  override val primaryEntityId: Int
    get() {
      require(tickData.any()) {
        "There is no TickData provided! Cannot get primaryEntityId of for this Segment."
      }
      require(tickData.first().entities.any()) {
        "There is no Entity in the first TickData. Cannot get primaryEntityId for this Segment"
      }
      val firstEgo = tickData.first().entities.first()
      return firstEgo.id
    }
}