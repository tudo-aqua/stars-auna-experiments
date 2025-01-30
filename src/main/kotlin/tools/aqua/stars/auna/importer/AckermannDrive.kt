/*
 * Copyright 2024-2025 The STARS AuNa Experiments Authors
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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/** Ackermann drive. */
data class AckermannDrive(
    @SerialName("steering_angle")
    /** The steering angle. */
    val steeringAngle: Double,
    @SerialName("steering_angle_velocity")
    /** The steering angle velocity. */
    val steeringAngleVelocity: Double,
    @SerialName("speed")
    /** The speed. */
    val speed: Double,
    @SerialName("acceleration")
    /** The acceleration. */
    val acceleration: Double,
    @SerialName("jerk")
    /** The jerk. */
    val jerk: Double,
)
