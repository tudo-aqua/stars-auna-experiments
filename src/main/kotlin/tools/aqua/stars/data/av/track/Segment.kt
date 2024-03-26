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

import tools.aqua.stars.core.types.SegmentType

/**
 * This class implements the [SegmentType] and holds the sliced analysis data in semantic segments.
 *
 * @param segmentId The id of this [Segment].
 * @param segmentSource Specifies the file from which the data of this [Segment] comes from.
 * @param ticks The [TickData]s relevant for the [Segment].
 * @param previousSegment The [Segment] before this [Segment].
 * @param nextSegment The [Segment] after this [Segment].
 */
data class Segment(
    val segmentId: Int,
    override val segmentSource: String,
    override val ticks: Map<AuNaTimeUnit, TickData>,
    var previousSegment: Segment?,
    var nextSegment: Segment?
) : SegmentType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference> {
  /** Holds a [Map] which maps a timestamp to all relevant [TickData]s (based on [tickData]). */

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

  //  fun getPrimaryEntityClones(): List<Segment> =
  //      tickData.first().entities.mapIndexed { index, e ->
  //        Segment(
  //                segmentId + (index + 1) * 1_000_000,
  //                segmentSource,
  //                tickData.map {
  //                  it.clone().also { t -> t.entities.first { e.id == it.id }.isPrimaryEntity =
  // true }
  //                },
  //                previousSegment,
  //                nextSegment)
  //            .also { it.tickData.forEach { t -> t.segment = it } }
  //      }
}
