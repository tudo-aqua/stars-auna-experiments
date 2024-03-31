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
import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.data.av.track.Lane
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.logic.kcmftbl.eventually
import tools.aqua.stars.logic.kcmftbl.globally

// region distance to previous vehicle
/*
 * The distance to the previous robot in m.
 */
/**
 * High distance to the previous vehicle is defined as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH].
 */
const val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH: Double = 3.0

/** Low distance to the previous vehicle is defined as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW]. */
const val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW: Double =
    1.1664 // break distance: (10.8km/h / 10)^2. 3m/s = 10.8km/h

/** High distance to the front vehicle is defined as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH]. */
val highDistanceToFrontVehicle = // FIXME: Measure across multiple lanes
    predicate(Robot::class) { _, r ->
      val frontRobot = checkNotNull(r.tickData.getEntityById(r.id - 1))
      eventually(
          r,
          frontRobot,
          phi = { rbt1, rbt2 ->
            rbt1.distanceToOther(rbt2) > DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH
          })
    }

/**
 * The distance to the front vehicle is in the normal range if it is in the interval
 * ([DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW] ... [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH]).
 */
val normalDistanceToFrontVehicle =
    predicate(Robot::class) { _, r ->
      val frontRobot = checkNotNull(r.tickData.getEntityById(r.id - 1))
      globally(
          r,
          frontRobot,
          phi = { rbt1, rbt2 ->
            rbt1.distanceToOther(rbt2) in
                DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW..DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH
          })
    }

/** Low distance to the front vehicle is defined as < [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW]. */
val lowDistanceToFrontVehicle =
    predicate(Robot::class) { _, r ->
      val frontRobot = checkNotNull(r.tickData.getEntityById(r.id - 1))
      eventually(
          r,
          frontRobot,
          phi = { rb1, rb2 -> rb1.distanceToOther(rb2) < DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW })
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

/** Maximum velocity is defined as >= [VELOCITY_MAX]. */
val maxVelocity =
    predicate(Robot::class) { _, r -> eventually(r, phi = { it.velocity >= VELOCITY_MAX }) }

/** High velocity is defined in the interval ([VELOCITY_HIGH] ... [VELOCITY_MAX]). */
val highVelocity =
    predicate(Robot::class) { ctx, r ->
      eventually(r, phi = { it.velocity in VELOCITY_HIGH ..< VELOCITY_MAX }) &&
          !maxVelocity.holds(ctx, r)
    }

/** Low velocity is defined as < [VELOCITY_HIGH]. */
val lowVelocity =
    predicate(Robot::class) { _, r -> globally(r, phi = { it.velocity < VELOCITY_HIGH }) }
// endregion

// region acceleration
/*
 * The acceleration of the robot in m/s^2.
 */

/** Strong acceleration is defined as >= [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]. */
const val ACCELERATION_ACCELERATION_STRONG_THRESHOLD: Double = 2.0

/**
 * Weak acceleration is defined in the interval ([ACCELERATION_ACCELERATION_WEAK_THRESHOLD] ...
 * [ACCELERATION_ACCELERATION_STRONG_THRESHOLD]).
 */
const val ACCELERATION_ACCELERATION_WEAK_THRESHOLD: Double = 0.5

/**
 * Weak deceleration is defined in the interval ([ACCELERATION_DECELERATION_STRONG_THRESHOLD] ...
 * [ACCELERATION_DECELERATION_WEAK_THRESHOLD]).
 */
const val ACCELERATION_DECELERATION_WEAK_THRESHOLD: Double = -0.5

/** Strong deceleration is defined as < [ACCELERATION_DECELERATION_STRONG_THRESHOLD]. */
const val ACCELERATION_DECELERATION_STRONG_THRESHOLD: Double = -2.0

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
 * No acceleration is defined in the interval ([ACCELERATION_DECELERATION_WEAK_THRESHOLD] ...
 * [ACCELERATION_ACCELERATION_WEAK_THRESHOLD]).
 */
val noAcceleration =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = {
            it.acceleration in
                ACCELERATION_DECELERATION_WEAK_THRESHOLD..ACCELERATION_ACCELERATION_WEAK_THRESHOLD
          })
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

// region lane type
/** Robot is currently entering a straight lane. */
val enteringStraight =
    predicate(Robot::class) { ctx, r -> entering.holds(ctx, r) && straight.holds(ctx, r) }
/** Robot is currently driving on a straight lane. */
val inStraight =
    predicate(Robot::class) { ctx, r -> middle.holds(ctx, r) && straight.holds(ctx, r) }
/** Robot is currently leaving on a straight lane. */
val leavingStraight =
    predicate(Robot::class) { ctx, r -> leaving.holds(ctx, r) && straight.holds(ctx, r) }

/** Robot is currently entering a tight curve. */
val enteringTightCurve =
    predicate(Robot::class) { ctx, r -> entering.holds(ctx, r) && tightCurve.holds(ctx, r) }
/** Robot is currently driving in a tight curve. */
val inTightCurve =
    predicate(Robot::class) { ctx, r -> middle.holds(ctx, r) && tightCurve.holds(ctx, r) }
/** Robot is currently leaving a tight curve. */
val leavingTightCurve =
    predicate(Robot::class) { ctx, r -> leaving.holds(ctx, r) && tightCurve.holds(ctx, r) }

/** Robot is currently entering a wide curve. */
val enteringWideCurve =
    predicate(Robot::class) { ctx, r -> entering.holds(ctx, r) && wideCurve.holds(ctx, r) }
/** Robot is currently driving in a wide curve. */
val inWideCurve =
    predicate(Robot::class) { ctx, r -> middle.holds(ctx, r) && wideCurve.holds(ctx, r) }
/** Robot is currently leaving a wide curve. */
val leavingWideCurve =
    predicate(Robot::class) { ctx, r -> leaving.holds(ctx, r) && wideCurve.holds(ctx, r) }

private val entering =
    predicate(Robot::class) { _, r -> r.lane.laneSegment == Lane.LaneSegment.ENTERING }
private val middle =
    predicate(Robot::class) { _, r -> r.lane.laneSegment == Lane.LaneSegment.MIDDLE }
private val leaving =
    predicate(Robot::class) { _, r -> r.lane.laneSegment == Lane.LaneSegment.LEAVING }
private val straight =
    predicate(Robot::class) { _, r -> r.lane.laneCurvature == Lane.LaneCurvature.STRAIGHT }
private val tightCurve =
    predicate(Robot::class) { _, r -> r.lane.laneCurvature == Lane.LaneCurvature.TIGHT_CURVE }
private val wideCurve =
    predicate(Robot::class) { _, r -> r.lane.laneCurvature == Lane.LaneCurvature.WIDE_CURVE }

// endregion

// region steering angle
/*
 * The steering angle of the robot in degrees.
 */

/** Hard steering angle is defined as >= [STEERING_ANGLE_HARD]. */
const val STEERING_ANGLE_HARD: Double = 10.0

/** Low steering angle is defined as >= [STEERING_ANGLE_LOW]. */
const val STEERING_ANGLE_LOW: Double = 2.5

/** Hard steering angle is defined as >= [STEERING_ANGLE_HARD]. */
val hardSteering =
    predicate(Robot::class) { _, r ->
      eventually(r, phi = { abs(it.steeringAngle) >= STEERING_ANGLE_HARD })
    }

/** Low steering is defined in the interval ([STEERING_ANGLE_LOW] ... [STEERING_ANGLE_HARD]). */
val lowSteering =
    predicate(Robot::class) { ctx, r ->
      eventually(r, phi = { abs(it.steeringAngle) >= STEERING_ANGLE_LOW }) &&
          !hardSteering.holds(ctx, r)
    }

/** No steering angle is defined as >= [STEERING_ANGLE_LOW]. */
val noSteering =
    predicate(Robot::class) { _, r ->
      globally(r, phi = { abs(it.steeringAngle) <= STEERING_ANGLE_LOW })
    }
// endregion
