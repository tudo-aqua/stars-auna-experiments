/*
 * Copyright 2023-2025 The STARS AuNa Experiments Authors
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

@file:Suppress("MayBeConstant", "MagicNumber")

package tools.aqua.stars.auna.experiments

import kotlin.math.abs
import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.logic.kcmftbl.eventually
import tools.aqua.stars.logic.kcmftbl.globally

// region acceleration
/*
 * The acceleration of the robot in m/s^2.
 */

/** Strong acceleration is defined as >= [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]. */
val ACCELERATION_ACCELERATION_STRONG_THRESHOLD: Double = 2.0

/**
 * Weak acceleration is defined in the interval ([ACCELERATION_ACCELERATION_WEAK_THRESHOLD] ...
 * [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]).
 */
val ACCELERATION_ACCELERATION_WEAK_THRESHOLD: Double = 0.4

/**
 * Weak deceleration is defined in the interval ([ACCELERATION_DECELERATION_STRONG_THRESHOLD] ...
 * [ACCELERATION_DECELERATION_WEAK_THRESHOLD]).
 */
val ACCELERATION_DECELERATION_WEAK_THRESHOLD: Double = -ACCELERATION_ACCELERATION_WEAK_THRESHOLD

/** Strong deceleration is defined as < [ACCELERATION_DECELERATION_STRONG_THRESHOLD]. */
val ACCELERATION_DECELERATION_STRONG_THRESHOLD: Double = -ACCELERATION_ACCELERATION_STRONG_THRESHOLD

/** Strong acceleration is defined as > [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]. */
val strongAcceleration =
    predicate(Robot::class) { _, r ->
      eventually(r, phi = { it.acceleration > ACCELERATION_ACCELERATION_STRONG_THRESHOLD })
    }

/**
 * Weak acceleration is defined in the interval ([ACCELERATION_ACCELERATION_WEAK_THRESHOLD] ...
 * [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]).
 */
val weakAcceleration =
    predicate(Robot::class) { ctx, r ->
      eventually(r, phi = { it.acceleration > ACCELERATION_ACCELERATION_WEAK_THRESHOLD }) &&
          !strongAcceleration.holds(ctx, r)
    }

/**
 * Weak deceleration is defined in the interval ([ACCELERATION_DECELERATION_STRONG_THRESHOLD] ...
 * [ACCELERATION_DECELERATION_WEAK_THRESHOLD]).
 */
val weakDeceleration =
    predicate(Robot::class) { ctx, r ->
      eventually(r, phi = { it.acceleration < ACCELERATION_DECELERATION_WEAK_THRESHOLD }) &&
          !strongDeceleration.holds(ctx, r)
    }

/** Strong deceleration is defined as < [ACCELERATION_DECELERATION_STRONG_THRESHOLD]. */
val strongDeceleration =
    predicate(Robot::class) { _, r ->
      eventually(r, phi = { it.acceleration < ACCELERATION_DECELERATION_STRONG_THRESHOLD })
    }
// endregion

// region steering angle
/*
 * The steering angle of the robot in degrees.
 */

/** Hard steering angle is defined as > [STEERING_ANGLE_HARD]. */
val STEERING_ANGLE_HARD: Double = 10.0

/** Low steering angle is defined as >= [STEERING_ANGLE_LOW]. */
val STEERING_ANGLE_LOW: Double = 2.5

/** Hard steering angle is defined as > [STEERING_ANGLE_HARD]. */
val hardSteering =
    predicate(Robot::class) { _, r ->
      eventually(r, phi = { abs(it.steeringAngle) > STEERING_ANGLE_HARD })
    }

/** Low steering is defined in the interval ([STEERING_ANGLE_LOW] ... [STEERING_ANGLE_HARD]). */
val lowSteering =
    predicate(Robot::class) { ctx, r ->
      eventually(r, phi = { abs(it.steeringAngle) >= STEERING_ANGLE_LOW }) &&
          !hardSteering.holds(ctx, r)
    }

/** No steering angle is defined as < [STEERING_ANGLE_LOW]. */
val noSteering =
    predicate(Robot::class) { _, r ->
      globally(r, phi = { abs(it.steeringAngle) < STEERING_ANGLE_LOW })
    }
// endregion
