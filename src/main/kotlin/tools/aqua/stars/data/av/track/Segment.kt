/*
 * Copyright 2023-2025 The STARS AuNa Experiments Authors
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
 * @property segmentId The id of this [Segment].
 * @property ticks The [TickData]s relevant for the [Segment].
 * @property previousSegment The [Segment] before this [Segment].
 * @property nextSegment The [Segment] after this [Segment].
 */
data class Segment(
    val segmentId: Int,
    override var ticks: Map<AuNaTimeUnit, TickData>,
    var previousSegment: Segment?,
    var nextSegment: Segment?
) : SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference> {

  override val segmentSource: String
    get() = ""

  /** Holds the id of the primary entity for this [Segment]. */
  override val primaryEntityId: Int
    get() {
      require(tickData.any()) {
        "There is no TickData provided! Cannot get primaryEntityId of for this Segment."
      }
      require(tickData.first().entities.any()) {
        "There is no Entity in the first TickData. Cannot get primaryEntityId for this Segment"
      }
      require(tickData.first().entities.any { it.isPrimaryEntity }) {
        "There need to be at least one 'primary' entity."
      }
      val firstEgo = tickData.first().entities.first { it.isPrimaryEntity }
      return firstEgo.id
    }

  override fun getSegmentIdentifier(): String =
      "Segment($segmentId with ticks from [${tickData.first().currentTick}..${tickData.last().currentTick}] " +
          "with primary entity id ${primaryEntityId})"

  override fun toString(): String = getSegmentIdentifier()
}
