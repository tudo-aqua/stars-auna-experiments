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

import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.data.av.track.AuNaTimeDifference
import tools.aqua.stars.data.av.track.DataSource
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.logic.kcmftbl.eventually
import tools.aqua.stars.logic.kcmftbl.globally
import tools.aqua.stars.logic.kcmftbl.until

// region lateral offset
/*
 * The lateral offset of the robot in m.
 */

/** Exceeding the maximum lateral offset defined as > [MAX_LATERAL_OFFSET]. */
const val MAX_LATERAL_OFFSET: Double = 0.5

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
const val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG: Double = 3.0

/**
 * Exceeding the minimum distance to the front vehicle is defined as <
 * [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT].
 */
const val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT: Double = 0.5 // in m

/**
 * Exceeding the minimum distance to the front vehicle is defined as <
 * [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT].
 */
val minDistanceToFrontVehicleExceeded =
    predicate(Robot::class) { _, r ->
      val frontRobot = r.tickData.getEntityById(r.id - 1)
      if (frontRobot == null) false
      else
          until(
              r,
              frontRobot,
              phi1 = { rb1, rb2 -> rb1.lane != rb2.lane },
              phi2 = { rb1, rb2 ->
                eventually(
                    rb1,
                    rb2,
                    phi = { rb1, rb2 ->
                      rb1.distanceToOther(rb2) < DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT
                    })
              })
    }

/**
 * The distance to the front vehicle is in the normal range if it is in the interval
 * ([DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT] ... [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG]).
 */
val normalDistanceToFrontVehicle =
    predicate(Robot::class) { _, r ->
      val frontRobot = r.tickData.getEntityById(r.id - 1)
      if (frontRobot == null) false
      else
          until(
              r,
              frontRobot,
              phi1 = { rb1, rb2 -> rb1.lane != rb2.lane },
              phi2 = { rb1, rb2 ->
                globally(
                    rb1,
                    rb2,
                    phi = { rbt1, rbt2 ->
                      rbt1.distanceToOther(rbt2) in
                          DISTANCE_TO_FRONT_ROBOT_THRESHOLD_SHORT..DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG
                    })
              })
    }

/**
 * Exceeding the maximum distance to the front vehicle is defined
 * as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG].
 */
val maxDistanceToFrontVehicleExceeded =
    predicate(Robot::class) { _, r ->
      val frontRobot = r.tickData.getEntityById(r.id - 1)
      if (frontRobot == null) false
      else
          until(
              r,
              frontRobot,
              phi1 = { rb1, rb2 -> rb1.lane != rb2.lane },
              phi2 = { rb1, rb2 ->
                eventually(
                    rb1,
                    rb2,
                    phi = { rbt1, rbt2 ->
                      rbt1.distanceToOther(rbt2) > DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LONG
                    })
              })
    }
// endregion

// region velocity
/*
 * The velocity of the robot in m/s.
 */

/** Maximum velocity is defined as >= [VELOCITY_MAX]. */
const val VELOCITY_MAX: Double = 2.5

/** High velocity is defined as >= [VELOCITY_HIGH]. */
const val VELOCITY_HIGH: Double = 1.5

/** Maximum velocity is defined as >= [STEERING_ANGLE_LOW]. */
val maxVelocity =
    predicate(Robot::class) { _, r ->
      eventually(r, phi = { (it.steeringAngle ?: 0.0) >= VELOCITY_MAX })
    }

/** High velocity is defined in the interval ([VELOCITY_HIGH] ... [VELOCITY_MAX]). */
val highVelocity =
    predicate(Robot::class) { ctx, r ->
      eventually(r, phi = { (it.velocity ?: 0.0) in VELOCITY_HIGH ..< VELOCITY_MAX }) &&
          !maxVelocity.holds(ctx, r)
    }

/** Low velocity is defined as < [VELOCITY_HIGH]. */
val lowVelocity =
    predicate(Robot::class) { _, r -> globally(r, phi = { (it.velocity ?: 0.0) < VELOCITY_HIGH }) }
// endregion

// region acceleration
/*
 * The acceleration of the robot in m/s^2.
 */

/** Strong acceleration is defined as >= [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]. */
const val ACCELERATION_ACCELERATION_STRONG_THRESHOLD: Double = 0.5

/**
 * Weak acceleration is defined in the interval ([ACCELERATION_ACCELERATION_WEAK_THRESHOLD] ...
 * [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]).
 */
const val ACCELERATION_ACCELERATION_WEAK_THRESHOLD: Double = 0.1

/**
 * Weak deceleration is defined in the interval ([ACCELERATION_DECELERATION_STRONG_THRESHOLD] ...
 * [ACCELERATION_DECELERATION_WEAK_THRESHOLD]).
 */
const val ACCELERATION_DECELERATION_WEAK_THRESHOLD: Double = -0.1

/** Strong deceleration is defined as < [ACCELERATION_DECELERATION_STRONG_THRESHOLD]. */
const val ACCELERATION_DECELERATION_STRONG_THRESHOLD: Double = -0.5

/** Strong acceleration is defined as > [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]. */
val strongAcceleration =
    predicate(Robot::class) { _, r ->
      eventually(
          r, phi = { (it.accelerationCAM ?: 0.0) >= ACCELERATION_ACCELERATION_STRONG_THRESHOLD })
    }

/**
 * Weak acceleration is defined in the interval ([ACCELERATION_ACCELERATION_WEAK_THRESHOLD] ...
 * [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]).
 */
val weakAcceleration =
    predicate(Robot::class) { _, r ->
      eventually(
          r,
          phi = {
            (it.accelerationCAM ?: 0.0) in
                ACCELERATION_ACCELERATION_WEAK_THRESHOLD ..<
                    ACCELERATION_ACCELERATION_STRONG_THRESHOLD
          })
    }

/**
 * No acceleration is defined in the interval ([ACCELERATION_DECELERATION_WEAK_THRESHOLD] ...
 * [ACCELERATION_ACCELERATION_WEAK_THRESHOLD]).
 */
val noAcceleration =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = {
            (it.accelerationCAM ?: 0.0) in
                ACCELERATION_DECELERATION_WEAK_THRESHOLD ..<
                    ACCELERATION_ACCELERATION_WEAK_THRESHOLD
          })
    }

/**
 * Weak deceleration is defined in the interval ([ACCELERATION_DECELERATION_STRONG_THRESHOLD] ...
 * [ACCELERATION_DECELERATION_WEAK_THRESHOLD]).
 */
val weakDeceleration =
    predicate(Robot::class) { _, r ->
      eventually(
          r,
          phi = {
            (it.accelerationCAM ?: 0.0) in
                ACCELERATION_DECELERATION_STRONG_THRESHOLD ..<
                    ACCELERATION_DECELERATION_WEAK_THRESHOLD
          })
    }

/** Strong deceleration is defined as < [ACCELERATION_DECELERATION_STRONG_THRESHOLD]. */
val strongDeceleration =
    predicate(Robot::class) { _, r ->
      eventually(
          r, phi = { (it.accelerationCAM ?: 0.0) < ACCELERATION_DECELERATION_STRONG_THRESHOLD })
    }
// endregion

// region lane type
/** Robot is mainly driving on a straight lane. */
val isOnStraightLane =
    predicate(Robot::class) { _, r -> globally(r, phi = { it.lane!!.isStraight }) }

/** Robot is mainly driving on a curved lane. */
val isOnCurvedLane =
    predicate(Robot::class) { _, r -> globally(r, phi = { !it.lane!!.isStraight }) }

// endregion

// region steering angle
/*
 * The steering angle of the robot in degrees.
 */

/** Hard steering angle is defined as >= [STEERING_ANGLE_HARD]. */
const val STEERING_ANGLE_HARD: Double = 30.0

/** Low steering angle is defined as >= [STEERING_ANGLE_LOW]. */
const val STEERING_ANGLE_LOW: Double = 7.5

/** Hard steering angle is defined as >= [STEERING_ANGLE_HARD]. */
val hardSteering =
    predicate(Robot::class) { _, r ->
      eventually(r, phi = { (it.steeringAngle ?: 0.0) >= STEERING_ANGLE_HARD })
    }

/** Low steering is defined in the interval ([STEERING_ANGLE_LOW] ... [STEERING_ANGLE_HARD]). */
val lowSteering =
    predicate(Robot::class) { _, r ->
      eventually(
          r, phi = { (it.steeringAngle ?: 0.0) in STEERING_ANGLE_LOW ..< STEERING_ANGLE_HARD })
    }

/** No steering angle is defined as >= [STEERING_ANGLE_LOW]. */
val noSteering =
    predicate(Robot::class) { _, r ->
      eventually(r, phi = { (it.steeringAngle ?: 0.0) < STEERING_ANGLE_LOW })
    }

// endregion
// TODO: Comments
val enteringCurve =
    predicate(Robot::class) { _, r ->
      r.lane!!.isCurve && r.lane.previousLane!!.isStraight
    }
val inCurve =
    predicate(Robot::class) { _, r ->
      r.lane!!.isCurve && r.lane.previousLane!!.isCurve && r.lane.nextLane!!.isCurve
    }
val exitingCurve =
    predicate(Robot::class) { _, r -> r.lane!!.isCurve && r.lane.nextLane!!.isStraight }

val enteringStraight =
    predicate(Robot::class) { _, r -> r.lane!!.isStraight && r.lane.previousLane!!.isCurve }
val inStraight =
    predicate(Robot::class) { _, r ->
      r.lane!!.isStraight && r.lane.previousLane!!.isStraight && r.lane.nextLane!!.isStraight
    }
val exitingStraight =
    predicate(Robot::class) { _, r -> r.lane!!.isStraight && r.lane.nextLane!!.isCurve }

const val CAM_DECELERATION_THRESHOLD = -4.0
const val CAM_TIME_THRESHOLD_NANOS = 100_000L // 100ms
val camMessageTimeout =
    predicate(Robot::class) { _, r ->
      r.acceleration!! < -CAM_DECELERATION_THRESHOLD &&
          eventually(
              r,
              phi = { it.dataSource == DataSource.CAM },
              interval = AuNaTimeDifference(0) to AuNaTimeDifference(CAM_TIME_THRESHOLD_NANOS))
    }
