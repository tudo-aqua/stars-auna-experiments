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

@file:Suppress("unused")

package tools.aqua.stars.auna.importer

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/** Quaternion. */
data class Quaternion(
    @SerialName("x")
    /** The x value. */
    val x: Double,
    @SerialName("y")
    /** The y value. */
    val y: Double,
    @SerialName("z")
    /** The z value. */
    val z: Double,
    @SerialName("w")
    /** The w value. */
    val w: Double
) {

  /** Calculate the roll value for this quaternion. */
  val roll: Double
    get() {
      val sinrCosp = 2 * (w * x + y * z)
      val cosrCosp = 1 - 2 * (x * x + y * y)
      return atan2(sinrCosp, cosrCosp)
    }

  /** Calculate the pitch value for this quaternion. */
  val pitch: Double
    get() {
      val sinp = sqrt(1 + 2 * (w * y - x * z))
      val cosp = sqrt(1 - 2 * (w * y - x * z))
      return 2 * atan2(sinp, cosp) - PI / 2
    }

  /** Calculate the yaw value for this quaternion. */
  val yaw: Double
    get() {
      val sinyCosp = 2 * (w * z + x * y)
      val cosyCosp = 1 - 2 * (y * y + z * z)
      return atan2(sinyCosp, cosyCosp)
    }

  /** The zero quaternion. */
  companion object {
    /** The zero quaternion. */
    val zero: Quaternion = Quaternion(0.0, 0.0, 0.0, 0.0)
  }
}
