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
import tools.aqua.stars.core.evaluation.BinaryPredicate.Companion.predicate
import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.logic.kcmftbl.eventually
import tools.aqua.stars.logic.kcmftbl.globally
import tools.aqua.stars.logic.kcmftbl.minPrevalence
import tools.aqua.stars.logic.kcmftbl.until

// region lateral offset
/*
 * The lateral offset of the robot in m.
 */

/** Exceeding the maximum lateral offset defined as > [MAX_LATERAL_OFFSET]. */
const val MAX_LATERAL_OFFSET: Double = 0.4

/** Normal lateral offset defined as <= [MAX_LATERAL_OFFSET]. */
val normalLateralOffset =
    predicate(Robot::class) { _, r ->
      globally(r, phi = { (it.lateralOffset ?: 0.0) <= MAX_LATERAL_OFFSET })
    }

/// ** Exceeding the maximum lateral offset defined as > [MAX_LATERAL_OFFSET]. */
// val maxLateralOffsetExceeded =
//    predicate(Robot::class) { _, r ->
//      eventually(r, phi = { (it.lateralOffset ?: 0.0) > MAX_LATERAL_OFFSET })
//    }
// endregion

// region distance to previous vehicle
/*
 * The distance to the previous robot in m.
 */

/**
 * Exceeding the maximum distance to the previous vehicle is defined
 * as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG].
 */
const val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG: Double = 2.0

/**
 * Exceeding the minimum distance to the previous vehicle is defined as <
 * [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT].
 */
const val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT: Double = 0.5 // in m

/**
 * Exceeding the minimum distance to the previous vehicle is defined as <
 * [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT].
 */
val minDistanceToPreviousVehicleExceeded =
    predicate(Robot::class to Robot::class) { _, r1, r2 ->
      until(
        r1,
        r2,
        phi1 = { rb1, rb2 -> rb1.lane != rb2.lane },
        phi2 = { rb1, rb2 ->
          globally(
            rb1,
            rb2,
            phi = { rb1, rb2 ->
              abs((rb1.posOnLane ?: 0.0) - (rb2.posOnLane ?: 0.0)) <
                  DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT
            })
        })
    }

/**
 * The distance to the previous vehicle is in the normal range if it is in the interval
 * ([DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT] .. [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG]).
 */
val normalDistanceToPreviousVehicle =
    predicate(Robot::class to Robot::class) { _, r1, r2 ->
      until(
          r1,
          r2,
          phi1 = { rb1, rb2 -> rb1.lane != rb2.lane },
          phi2 = { rb1, rb2 ->
            globally(
                rb1,
                rb2,
                phi = { rbt1, rbt2 ->
                  abs((rbt1.posOnLane ?: 0.0) - (rbt2.posOnLane ?: 0.0)) in
                      DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT..DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG
                })
          })
    }

/**
 * Exceeding the maximum distance to the previous vehicle is defined
 * as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG].
 */
val maxDistanceToPreviousVehicleExceeded =
    predicate(Robot::class to Robot::class) { _, r1, r2 ->
      until(
          r1,
          r2,
          phi1 = { rb1, rb2 -> rb1.lane != rb2.lane },
          phi2 = { rb1, rb2 ->
            globally(
                rb1,
                rb2,
                phi = { rbt1, rbt2 ->
                  abs((rbt1.posOnLane ?: 0.0) - (rbt2.posOnLane ?: 0.0)) >
                      DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG
                })
          })
    }

/// **
// * There is no distance to the previous robot, as neither of the "normal", "min" and "max"
// distance
// * holds.
// */
// val noDistanceToPreviousVehicle =
//    predicate(Robot::class to Robot::class) { ctx, r1, r2 ->
//      !(normalDistanceToPreviousVehicle.holds(ctx, r1, r2) ||
//          minDistanceToPreviousVehicleExceeded.holds(ctx, r1, r2) ||
//          maxDistanceToPreviousVehicleExceeded.holds(ctx, r1, r2))
//    }
// endregion

// region acceleration
/*
 * The acceleration of the robot in m/s^2.
 */

/** Strong acceleration is defined as >= [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]. */
const val ACCELERATION_ACCELERATION_STRONG_THRESHOLD: Double = 0.5

/**
 * Weak acceleration is defined in the interval ([ACCELERATION_ACCELERATION_WEAK_THRESHOLD] ..
 * [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]).
 */
const val ACCELERATION_ACCELERATION_WEAK_THRESHOLD: Double = 0.1

/**
 * Weak deceleration is defined in the interval ([ACCELERATION_DECELERATION_STRONG_THRESHOLD] ..
 * [ACCELERATION_DECELERATION_WEAK_THRESHOLD]).
 */
const val ACCELERATION_DECELERATION_WEAK_THRESHOLD: Double = -0.1

/** Strong deceleration is defined as < [ACCELERATION_DECELERATION_STRONG_THRESHOLD]. */
const val ACCELERATION_DECELERATION_STRONG_THRESHOLD: Double = -0.5

/** Strong acceleration is defined as > [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]. */
val strongAcceleration =
    predicate(Robot::class) { _, r ->
      eventually(
          r, phi = { (it.acceleration ?: 0.0) >= ACCELERATION_ACCELERATION_STRONG_THRESHOLD })
    }

/**
 * Weak acceleration is defined in the interval ([ACCELERATION_ACCELERATION_WEAK_THRESHOLD] ..
 * [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]).
 */
val weakAcceleration =
    predicate(Robot::class) { _, r ->
      eventually(
          r,
          phi = {
            (it.acceleration ?: 0.0) in
                ACCELERATION_ACCELERATION_WEAK_THRESHOLD ..<
                    ACCELERATION_ACCELERATION_STRONG_THRESHOLD
          })
    }

/**
 * No acceleration is defined in the interval ([ACCELERATION_DECELERATION_WEAK_THRESHOLD] ..
 * [ACCELERATION_ACCELERATION_WEAK_THRESHOLD]).
 */
val noAcceleration =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = {
            (it.acceleration ?: 0.0) in
                ACCELERATION_DECELERATION_WEAK_THRESHOLD ..<
                    ACCELERATION_ACCELERATION_WEAK_THRESHOLD
          })
    }

/**
 * Weak deceleration is defined in the interval ([ACCELERATION_DECELERATION_STRONG_THRESHOLD] ..
 * [ACCELERATION_DECELERATION_WEAK_THRESHOLD]).
 */
val weakDeceleration =
    predicate(Robot::class) { _, r ->
      eventually(
          r,
          phi = {
            (it.acceleration ?: 0.0) in
                ACCELERATION_DECELERATION_STRONG_THRESHOLD ..<
                    ACCELERATION_DECELERATION_WEAK_THRESHOLD
          })
    }

/** Strong deceleration is defined as < [ACCELERATION_DECELERATION_STRONG_THRESHOLD]. */
val strongDeceleration =
    predicate(Robot::class) { _, r ->
      eventually(r, phi = { (it.acceleration ?: 0.0) < ACCELERATION_DECELERATION_STRONG_THRESHOLD })
    }
// endregion

// region lane type
/** Robot is mainly driving on a straight lane */
val isOnStraightLane =
    predicate(Robot::class) { _, r ->
      minPrevalence(r, 0.8, phi = { it.lane?.isStraight ?: false })
    }

/** Robot is mainly driving on a curved lane */
val isOnCurvedLane = predicate(Robot::class) { ctx, r -> !isOnStraightLane.holds(ctx, r) }

// endregion
