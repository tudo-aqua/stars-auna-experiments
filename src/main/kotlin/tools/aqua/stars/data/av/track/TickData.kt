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

import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.core.types.TickDataType

/**
 * This class implements the [TickDataType] and holds a specific timestamp (see [currentTick]) and
 * the states of all [EntityType]s (see [Robot]) at the timestamp.
 *
 * @property currentTick The current timestamp in seconds.
 * @property entities The [List] of [Robot]s for the [currentTick].
 * @property id The id of this [TickData].
 */
data class TickData(
    override val currentTick: AuNaTimeUnit,
    override var entities: List<Robot>,
    var id: Int = -1
) : TickDataType<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference> {
  /** Holds a reference to the [Segment] in which this [TickData] is included and analyzed. */
  override lateinit var segment: Segment

  /**
   * Returns Robot with id [robotId].
   *
   * @throws NoSuchElementException if no Robot with id [robotId] is found.
   */
  fun getById(robotId: Int): Robot = entities.first { it.id == robotId }

  /**
   * Clones this [TickData] object.
   *
   * @return The cloned [TickData] object.
   */
  fun clone(): TickData {
    val newTick = TickData(currentTick.clone(), emptyList(), -1)
    val entityCopies = entities.map { it.copyToNewTick(newTick) }
    newTick.entities = entityCopies
    newTick.id = id

    if (this::segment.isInitialized) newTick.segment = segment

    return newTick
  }

  override fun equals(other: Any?): Boolean =
      if (other is TickData) this.currentTick == other.currentTick else super.equals(other)

  override fun hashCode(): Int = currentTick.hashCode()
}
