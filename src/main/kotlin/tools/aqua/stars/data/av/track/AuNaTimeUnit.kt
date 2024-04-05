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
import tools.aqua.stars.core.types.TickUnit

@Suppress("MemberVisibilityCanBePrivate")
class AuNaTimeUnit(val nanos: BigInteger) : TickUnit<AuNaTimeUnit, AuNaTimeDifference> {

  val seconds
    get() = nanos / 1e9.toLong().toBigInteger()

  val millis
    get() = nanos / 1e6.toLong().toBigInteger()

  val micros
    get() = nanos / 1e3.toLong().toBigInteger()

  constructor(
      seconds: Double,
      nanoseconds: Double
  ) : this((seconds * 1e9).roundToLong().toBigInteger() + nanoseconds.roundToLong().toBigInteger())

  override fun compareTo(other: AuNaTimeUnit): Int = nanos.compareTo(other.nanos)

  override fun minus(other: AuNaTimeDifference): AuNaTimeUnit =
      AuNaTimeUnit(nanos - other.differenceNanos)

  override fun minus(other: AuNaTimeUnit): AuNaTimeDifference =
      AuNaTimeDifference(nanos - other.nanos)

  override fun plus(other: AuNaTimeDifference): AuNaTimeUnit =
      AuNaTimeUnit(nanos + other.differenceNanos)

  fun toSeconds(): Double = (nanos.toBigDecimal() / 1e9.toBigDecimal()).toDouble()

  fun toMillis(): Double = (nanos.toBigDecimal() / 1e6.toBigDecimal()).toDouble()

  override fun toString(): String =
      "(${seconds}s, ${millis.mod(1e3.toLong().toBigInteger())}ms, ${micros.mod(1e3.toLong().toBigInteger())}µs, ${nanos.mod(1e3.toLong().toBigInteger())}ns)"

  fun clone(): AuNaTimeUnit = AuNaTimeUnit(nanos)

  fun equals(other: AuNaTimeUnit): Boolean = nanos == other.nanos

  companion object {
    @Suppress("unused") val Zero = AuNaTimeUnit(0.0, 0.0)
  }
}
