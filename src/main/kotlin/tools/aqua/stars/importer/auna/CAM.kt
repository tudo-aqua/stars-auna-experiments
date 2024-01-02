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

package tools.aqua.stars.importer.auna

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CAM(
    @SerialName("msg") override val header: Header,
    @SerialName("robot_name") val robotName: String,
    @SerialName("x") val x: Double,
    @SerialName("y") val y: Double,
    @SerialName("z") val z: Double,
    @SerialName("drive_direction") val driveDirection: Int,
    @SerialName("theta") val theta: Double,
    @SerialName("thetadot") val thetaDot: Double,
    @SerialName("v") val v: Double,
    @SerialName("vdot") val vDot: Double,
    @SerialName("curv") val curv: Double,
    @SerialName("vehicle_length") val vehicleLength: Double,
    @SerialName("vehicle_width") val vehicleWidth: Double
) : Message
