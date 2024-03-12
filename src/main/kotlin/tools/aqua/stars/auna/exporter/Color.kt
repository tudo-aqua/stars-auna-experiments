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

@file:Suppress("unused")

package tools.aqua.stars.auna.exporter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This enum class represents color values which can be used in the STARS-Visualizer tool for
 * coloring objects.
 */
@Serializable
enum class Color {
  @SerialName("black") BLACK,
  @SerialName("darkGray") DARK_GRAY,
  @SerialName("slateGray") SLATE_GRAY,
  @SerialName("gray") GRAY,
  @SerialName("green") GREEN,
  @SerialName("blue") BLUE,
  @SerialName("aqua") AQUA,
  @SerialName("violet") VIOLET,
  @SerialName("crimson") CRIMSON,
  @SerialName("firebrick") FIREBRICK,
  @SerialName("darkRed") DARK_RED,
  @SerialName("orange") ORANGE,
  @SerialName("amber") AMBER,
  @SerialName("yellow") YELLOW,
  @SerialName("white") WHITE
}
