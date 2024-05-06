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

import java.math.BigInteger
import kotlin.math.roundToLong
import tools.aqua.stars.core.types.TickDifference

@Suppress("MemberVisibilityCanBePrivate")
/**
 * Time difference implementation for AuNa.
 *
 * @property differenceNanos The difference in nanoseconds.
 */
class AuNaTimeDifference(val differenceNanos: BigInteger) : TickDifference<AuNaTimeDifference> {

  /** Time cut to seconds. */
  val seconds
    get() = differenceNanos / 1e9.toLong().toBigInteger()

  /** Time cut to milliseconds. */
  val millis
    get() = differenceNanos / 1e6.toLong().toBigInteger()

  /** Time cut to microseconds. */
  val micros
    get() = differenceNanos / 1e3.toLong().toBigInteger()

  constructor(differenceNanos: Long) : this(differenceNanos.toBigInteger())

  constructor(
      secondsDifference: Double,
      nanosecondsDifference: Double
  ) : this(
      (secondsDifference * 1e9).roundToLong().toBigInteger() +
          nanosecondsDifference.roundToLong().toBigInteger())

  override fun compareTo(other: AuNaTimeDifference): Int =
      differenceNanos.compareTo(other.differenceNanos)

  override fun minus(other: AuNaTimeDifference): AuNaTimeDifference =
      AuNaTimeDifference(differenceNanos - other.differenceNanos)

  override fun plus(other: AuNaTimeDifference): AuNaTimeDifference =
      AuNaTimeDifference(differenceNanos + other.differenceNanos)

  override fun toString(): String =
      "(${seconds}s, " +
          "${millis.mod(1e3.toLong().toBigInteger())}ms, " +
          "${micros.mod(1e3.toLong().toBigInteger())}µs, " +
          "${differenceNanos.mod(1e3.toLong().toBigInteger())}ns)"
}
