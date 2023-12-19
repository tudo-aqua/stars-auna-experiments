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

import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.core.types.TickDataType

/**
 * This class implements the [TickDataType] and holds a specific timestamp (see [currentTick]) and
 * the states of all [EntityType]s (see [Robot]) at the timestamp.
 *
 * @param currentTick The current timestamp in seconds
 * @param entities The [List] of [Robot]s for the [currentTick]
 */
data class TickData(override val currentTick: Double, override var entities: List<Robot>) :
    TickDataType<Robot, TickData, Segment> {
  /** Holds a reference to the [Segment] in which this [TickData] is included and analyzed */
  override lateinit var segment: Segment
}