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

package tools.aqua.stars.auna.exporter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This class represents one Tick of a [Segment] holding information about each actor.
 *
 * @property tick The current tick described by this [TickData].
 * @property actors The [List] of [ActorPosition]s containing information about each actor.
 */
@Serializable
data class TickData(
    @SerialName("tick") val tick: Double,
    @SerialName("actors") val actors: List<ActorPosition>
)
