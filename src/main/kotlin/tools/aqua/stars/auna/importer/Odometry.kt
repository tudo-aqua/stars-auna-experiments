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

package tools.aqua.stars.auna.importer

import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Odometry(
    @SerialName("msg") override val header: Header,
    @SerialName("pose") val pose: PoseWithCovariance,
    @SerialName("twist") val twist: TwistWithCovariance
) : Message {
  fun getVelocity(): Double {
    val velocityVector = this.twist.twist.linear
    return sqrt(velocityVector.x.pow(2) + velocityVector.y.pow(2) + velocityVector.z.pow(2))
  }

  fun getAngularVelocity() {}
}
