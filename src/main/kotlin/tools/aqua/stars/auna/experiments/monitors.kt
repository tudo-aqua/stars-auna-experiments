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
const val MAX_LATERAL_OFFSET: Double = 0.4

/** Normal lateral offset defined as <= [MAX_LATERAL_OFFSET]. */
val normalLateralOffset =
    predicate(Robot::class) { _, r ->
      globally(r, phi = { it.lateralOffset <= MAX_LATERAL_OFFSET })
    }
// endregion

// region distance to previous vehicle
/*
 * The distance to the previous robot in m.
 */

/**
 * Exceeding the maximum distance to the previous vehicle is defined
 * as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_EXCEEDED].
 */
const val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_EXCEEDED: Double = 3.0

/**
 * Exceeding the minimum distance to the front vehicle is defined as <
 * [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_UNDERCUT].
 */
const val DISTANCE_TO_FRONT_ROBOT_THRESHOLD_UNDERCUT: Double = 0.5

/**
 * Exceeding the minimum distance to the front vehicle is defined as <
 * [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_UNDERCUT].
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
                      rb1.distanceToOther(rb2) < DISTANCE_TO_FRONT_ROBOT_THRESHOLD_UNDERCUT
                    })
              })
    }

/**
 * Exceeding the maximum distance to the front vehicle is defined
 * as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_EXCEEDED].
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
                      rbt1.distanceToOther(rbt2) > DISTANCE_TO_FRONT_ROBOT_THRESHOLD_EXCEEDED
                    })
              })
    }
// endregion

// region acceleration
/*
 * The acceleration of the robot in m/s^2.
 */

/**
 * Exceeding the maximum deceleration is defined as < [ACCELERATION_DECELERATION_STRONG_THRESHOLD].
 */
const val MAXIMUM_DECELERATION_THRESHOLD: Double = -1.0

/** Minimum duration between acceleration and deceleration in seconds. */
const val MIN_ACCELERATION_TO_DECELERATION_DURATION: Double = 2.0

/** Minimum duration between deceleration and acceleration in seconds. */
const val MIN_DECELERATION_TO_ACCELERATION_DURATION: Double = 2.0

/**
 * Exceeding the maximum deceleration is defined as < [ACCELERATION_DECELERATION_STRONG_THRESHOLD].
 */
val maxDecelerationExceeded =
    predicate(Robot::class) { _, r ->
      eventually(r, phi = { it.accelerationCAM <= MAXIMUM_DECELERATION_THRESHOLD })
    }

/**
 * Acceleration to deceleration transition phase must be at least
 * [MIN_ACCELERATION_TO_DECELERATION_DURATION] seconds long.
 */
val noShortAccelerationToDecelerationTransition =
    predicate(Robot::class) { _, r ->
      r.acceleration >= ACCELERATION_ACCELERATION_WEAK_THRESHOLD &&
          !eventually(
              entity = r,
              phi = { it.acceleration <= ACCELERATION_DECELERATION_WEAK_THRESHOLD },
              interval =
                  AuNaTimeDifference(0) to
                      AuNaTimeDifference(MIN_ACCELERATION_TO_DECELERATION_DURATION, 0.0))
    }

/**
 * Deceleration to acceleration transition phase must be at least
 * [MIN_DECELERATION_TO_ACCELERATION_DURATION] seconds long.
 */
val noShortDecelerationToAccelerationTransition =
    predicate(Robot::class) { _, r ->
      r.acceleration <= ACCELERATION_DECELERATION_WEAK_THRESHOLD &&
          !eventually(
              entity = r,
              phi = { it.acceleration >= ACCELERATION_ACCELERATION_WEAK_THRESHOLD },
              interval =
                  AuNaTimeDifference(0) to
                      AuNaTimeDifference(MIN_DECELERATION_TO_ACCELERATION_DURATION, 0.0))
    }
// endregion

// region CAM timeout
/** Deceleration threshold from which a cam message must be sent. */
const val CAM_DECELERATION_THRESHOLD = -4.0

/** Time threshold in nanoseconds for a CAM message to be sent. */
const val CAM_TIME_THRESHOLD_NANOS = 100_000L // 100ms

/**
 * A CAM message must be sent within [CAM_TIME_THRESHOLD_NANOS] after a deceleration of <
 * [CAM_DECELERATION_THRESHOLD].
 */
val camMessageTimeout =
    predicate(Robot::class) { _, r ->
      r.acceleration < -CAM_DECELERATION_THRESHOLD &&
          eventually(
              r,
              phi = { it.dataSource == DataSource.CAM },
              interval = AuNaTimeDifference(0) to AuNaTimeDifference(CAM_TIME_THRESHOLD_NANOS))
    }
// endregion
