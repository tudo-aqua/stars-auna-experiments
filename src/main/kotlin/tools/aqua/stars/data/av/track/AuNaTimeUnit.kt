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

package tools.aqua.stars.data.av.track

import tools.aqua.stars.core.types.TickUnit

class AuNaTimeUnit(val seconds: Double, val nanoseconds: Double) :
    TickUnit<AuNaTimeUnit, AuNaTimeDifference> {
  override fun compareTo(other: AuNaTimeUnit): Int {
    return if (seconds != other.seconds) {
      seconds.compareTo(other.seconds)
    } else {
      nanoseconds.compareTo(other.nanoseconds)
    }
  }

  override fun minus(other: AuNaTimeDifference): AuNaTimeUnit =
      AuNaTimeUnit(seconds - other.secondsDifference, nanoseconds - other.nanosecondsDifference)

  override fun minus(other: AuNaTimeUnit): AuNaTimeDifference =
      AuNaTimeDifference(seconds - other.seconds, nanoseconds - other.nanoseconds)

  override fun plus(other: AuNaTimeDifference): AuNaTimeUnit =
      AuNaTimeUnit(seconds + other.secondsDifference, nanoseconds + other.nanosecondsDifference)

  fun toSeconds(): Double {
    return seconds + (nanoseconds / 1e9) // 1e9 represents one billion (nanoseconds in a second)
  }

  fun toMillis(): Double {
    return seconds * 1000 + (nanoseconds / 1e6)
  }

  override fun toString(): String {
    return "${toSeconds()}s"//"(${seconds}s, ${nanoseconds}ns)"
  }

  fun clone(): AuNaTimeUnit = AuNaTimeUnit(this.seconds, this.nanoseconds)

  companion object {
    val Zero = AuNaTimeUnit(0.0, 0.0)
  }
}
