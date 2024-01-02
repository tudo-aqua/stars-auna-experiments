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

@file:Suppress("NAME_SHADOWING")

package tools.aqua.stars.auna.experiments

import kotlin.math.abs
import tools.aqua.stars.core.evaluation.predicate
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.logic.kcmftbl.globally

const val MAX_LATERAL_OFFSET = 0.4 // in m
const val MAX_DISTANCE_TO_FRONT_ROBOT = 0.5 // in m

// Each Robot must not have a lateral offset of more than MAX_LATERAL_OFFSET
val maxLateralOffset =
    predicate(Robot::class) { _, v ->
      globally(v) { v -> (v.lateralOffset ?: 0.0) <= MAX_LATERAL_OFFSET }
    }

// The distance to the robot in front should not exceed MAX_DISTANCE_TO_FRONT_ROBOT
val maxDistanceToPreviousVehicle =
    predicate(Robot::class to Robot::class) { _, r1, r2 ->
      globally(r1, r2) { r1, r2 ->
        (abs(r1.id - r2.id) == 1) &&
            r1.lane == r2.lane &&
            abs((r1.posOnLane ?: 0.0) - (r2.posOnLane ?: 0.0)) > MAX_DISTANCE_TO_FRONT_ROBOT &&
            abs((r1.posOnLaneCAM ?: 0.0) - (r2.posOnLaneCAM ?: 0.0)) > MAX_DISTANCE_TO_FRONT_ROBOT
      }
    }
