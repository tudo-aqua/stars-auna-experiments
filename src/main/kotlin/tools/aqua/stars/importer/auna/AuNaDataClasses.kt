/*
 * Copyright 2023 The STARS auNa Experiments Authors
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
data class Header(
    @SerialName("stamp") val timeStamp: Time,
    @SerialName("frame_id") val frameId: String,
)

@Serializable
data class Time(
    @SerialName("seconds") val seconds: Double,
    @SerialName("nanoseconds") val nanoseconds: Double
)

@Serializable
data class CAM(
    @SerialName("msg") val header: Header,
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
)

@Serializable
data class Transform(
    @SerialName("translation") val translation: Vector,
    @SerialName("rotation") val rotation: Quaternion
)

@Serializable
data class Quaternion(
    @SerialName("x") val x: Double,
    @SerialName("y") val y: Double,
    @SerialName("z") val z: Double,
    @SerialName("w") val w: Double
)

@Serializable
data class Vector(
    @SerialName("x") val x: Double,
    @SerialName("y") val y: Double,
    @SerialName("z") val z: Double,
)

@Serializable
data class ViconPose(
    @SerialName("msg") val header: Header,
    @SerialName("child_frame_id") val childFrameId: String,
    @SerialName("transform") val transform: Transform
)

@Serializable
data class Odometry(
    @SerialName("msg") val header: Header,
    @SerialName("pose") val pose: PoseWithCovariance,
    @SerialName("twist") val twist: TwistWithCovariance
)

@Serializable
data class PoseWithCovariance(
    @SerialName("pose") val pose: Pose,
    @SerialName("covariance") val covariance: List<Double>
)

@Serializable
data class Pose(
    @SerialName("position") val position: Point,
    @SerialName("orientation") val orientation: Quaternion
)

@Serializable
data class Point(
    @SerialName("x") val x: Double,
    @SerialName("y") val y: Double,
    @SerialName("z") val z: Double,
)

@Serializable
data class TwistWithCovariance(
    @SerialName("twist") val twist: Twist,
    @SerialName("covariance") val covariance: List<Double>
)

@Serializable
data class Twist(
    @SerialName("linear") val linear: Vector,
    @SerialName("angular") val angular: Vector
)
