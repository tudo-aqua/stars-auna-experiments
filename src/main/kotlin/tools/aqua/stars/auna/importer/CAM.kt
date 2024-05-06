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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/** Cooperative Awareness Message. */
data class CAM(
    @SerialName("msg")
    /** The [Header]. */
    override val header: Header,
    @SerialName("robot_name")
    /** The robot's name. */
    val robotName: String,
    @SerialName("x")
    /** The x-ordinate. */
    val x: Double,
    @SerialName("y")
    /** The y-ordinate. */
    val y: Double,
    @SerialName("z")
    /** The z-ordinate. */
    val z: Double,
    @SerialName("drive_direction")
    /** The drive direction. */
    val driveDirection: Int,
    @SerialName("theta")
    /** The rotation. */
    val theta: Double,
    @SerialName("thetadot")
    /** The current rotation change. */
    val thetaDot: Double,
    @SerialName("v")
    /** The velocity. */
    val v: Double,
    @SerialName("vdot")
    /** The current velocity change (acceleration). */
    val vDot: Double,
    @SerialName("curv")
    /** The curv. */
    val curv: Double,
    @SerialName("vehicle_length")
    /** The vehicle's length. */
    val vehicleLength: Double,
    @SerialName("vehicle_width")
    /** The vehicle's width. */
    val vehicleWidth: Double
) : Message
