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

import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.data.av.track.Lane
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.logic.kcmftbl.eventually
import tools.aqua.stars.logic.kcmftbl.globally
import tools.aqua.stars.logic.kcmftbl.minPrevalence

// region distance to previous vehicle
/*
 * The distance to the previous robot in m.
 */
/**
 * High distance to the previous vehicle is defined as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH].
 */
val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH: Double = 3.0

/** Low distance to the previous vehicle is defined as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW]. */
val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_LOW: Double = 1.5

/** High distance to the front vehicle is defined as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH]. */
val highDistanceToFrontVehicle =
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

/** Maximum velocity is defined as > [VELOCITY_MAX]. */
val VELOCITY_MAX: Double = 2.75

/** High velocity is defined as >= [VELOCITY_HIGH]. */
val VELOCITY_HIGH: Double = 2.0

/** Maximum velocity is defined as > [VELOCITY_MAX]. */
val maxVelocity =
    predicate(Robot::class) { _, r ->
      minPrevalence(r, percentage = 0.5, phi = { it.velocity > VELOCITY_MAX })
    }

/** High velocity is defined in the interval ([VELOCITY_HIGH] ... [VELOCITY_MAX]). */
val highVelocity =
    predicate(Robot::class) { _, r ->
      minPrevalence(r, percentage = 0.5, phi = { it.velocity in VELOCITY_HIGH..VELOCITY_MAX })
    }

/** Low velocity is defined as < [VELOCITY_HIGH]. */
val lowVelocity =
    predicate(Robot::class) { _, r ->
      minPrevalence(r, percentage = 0.5, phi = { it.velocity < VELOCITY_HIGH })
    }
// endregion

// region lane type
/** Robot is currently entering the track section. */
val entering = predicate(Robot::class) { _, r -> r.lane.laneSegment == Lane.LaneSegment.ENTERING }
/** Robot is currently driving in the middle of the track section. */
val middle = predicate(Robot::class) { _, r -> r.lane.laneSegment == Lane.LaneSegment.MIDDLE }
/** Robot is currently leaving the track section. */
val leaving = predicate(Robot::class) { _, r -> r.lane.laneSegment == Lane.LaneSegment.LEAVING }

/** Robot is currently leaving in the top straight section. */
val topStraight =
    predicate(Robot::class) { _, r -> r.lane.laneCurvature == Lane.LaneCurvature.TOP_STRAIGHT }
/** Robot is currently leaving in the bottom straight section. */
val bottomStraight =
    predicate(Robot::class) { _, r -> r.lane.laneCurvature == Lane.LaneCurvature.BOTTOM_STRAIGHT }
/** Robot is currently in the tight curve. */
val tightCurve =
    predicate(Robot::class) { _, r -> r.lane.laneCurvature == Lane.LaneCurvature.TIGHT_CURVE }
/** Robot is currently in the wide curve. */
val wideCurve =
    predicate(Robot::class) { _, r -> r.lane.laneCurvature == Lane.LaneCurvature.WIDE_CURVE }
// endregion
