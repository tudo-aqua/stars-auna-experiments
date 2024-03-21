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

import tools.aqua.stars.core.types.TickDifference

class AuNaTimeDifference(val secondsDifference: Double, val nanosecondsDifference: Double) :
    TickDifference<AuNaTimeDifference> {

  constructor(
      nanosecondsDifference: Long
  ) : this(nanosecondsDifference / 1e9, nanosecondsDifference % 1e9)

  override fun compareTo(other: AuNaTimeDifference): Int {
    return if (secondsDifference != other.secondsDifference) {
      secondsDifference.compareTo(other.secondsDifference)
    } else {
      nanosecondsDifference.compareTo(other.nanosecondsDifference)
    }
  }

  override fun minus(other: AuNaTimeDifference): AuNaTimeDifference =
      AuNaTimeDifference(
          secondsDifference - other.secondsDifference,
          nanosecondsDifference - other.nanosecondsDifference)

  override fun plus(other: AuNaTimeDifference): AuNaTimeDifference =
      AuNaTimeDifference(
          secondsDifference + other.secondsDifference,
          nanosecondsDifference + other.nanosecondsDifference)

  fun toDoubleValue(): Double {
    return secondsDifference +
        (nanosecondsDifference / 1e9) // 1e9 represents one billion (nanoseconds in a second)
  }

  override fun toString(): String {
    return "(${secondsDifference}s, ${nanosecondsDifference}ns)"
  }
}
