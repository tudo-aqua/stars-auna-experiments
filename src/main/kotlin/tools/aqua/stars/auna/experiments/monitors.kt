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

import java.lang.Math.pow
import kotlin.math.*
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
      globally(r, phi = { abs(it.lateralOffset) <= MAX_LATERAL_OFFSET })
    }
// endregion

// region distance to previous vehicle
/*
 * The distance to the previous robot in m.
 */

/** Distance in m to front vehicle must globally be greater than breaking distance. */
val minDistanceToFrontVehicleExceeded =
    predicate(Robot::class) { _, r ->
      val frontRobot = r.tickData.getEntityById(r.id - 1)
      if (frontRobot == null) false
      else
          globally(
              r,
              frontRobot,
              phi = { rb1, rb2,
                ->
                // Distance in m to front vehicle is smaller than breaking distance
                rb1.distanceToOther(rb2) > ((rb1.velocity * 3.6) / 10).pow(2)
              })
    }

/**
 * Exceeding the maximum distance to the front vehicle is defined
 * as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH] * 1.25.
 */
val maxDistanceToFrontVehicleExceeded =
    predicate(Robot::class) { _, r ->
      val frontRobot = r.tickData.getEntityById(r.id - 1)
      if (frontRobot == null) false
      else
          globally(
              r,
              frontRobot,
              phi = { rb1, rb2,
                ->
                rb1.distanceToOther(rb2) < DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH * 1.25
              })
    }
// endregion

// region acceleration
/*
 * The acceleration of the robot in m/s^2.
 */

/** Exceeding the maximum deceleration is defined as < -3.0 m/s². */
const val MAXIMUM_DECELERATION_THRESHOLD: Double = -3.0

/** Minimum duration between acceleration and deceleration in seconds. */
const val MIN_ACCELERATION_TO_DECELERATION_DURATION: Double = 2.0

/** Minimum duration between deceleration and acceleration in seconds. */
const val MIN_DECELERATION_TO_ACCELERATION_DURATION: Double = 2.0

/** Exceeding the maximum deceleration is defined as > [MAXIMUM_DECELERATION_THRESHOLD]. */
val maxDecelerationExceeded =
    predicate(Robot::class) { _, r ->
      globally(r, phi = { it.acceleration > MAXIMUM_DECELERATION_THRESHOLD })
    }

/**
 * Acceleration to deceleration transition phase must be at least
 * [MIN_ACCELERATION_TO_DECELERATION_DURATION] seconds long.
 */
val noShortAccelerationToDecelerationTransition =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r ->
            r.acceleration >= ACCELERATION_ACCELERATION_WEAK_THRESHOLD &&
                globally(
                    r,
                    phi = { r.acceleration > ACCELERATION_DECELERATION_WEAK_THRESHOLD },
                    interval =
                        AuNaTimeDifference(0) to
                            AuNaTimeDifference(MIN_ACCELERATION_TO_DECELERATION_DURATION, 0.0))
          })
    }

/**
 * Deceleration to acceleration transition phase must be at least
 * [MIN_DECELERATION_TO_ACCELERATION_DURATION] seconds long.
 */
val noShortDecelerationToAccelerationTransition =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r ->
            r.acceleration <= ACCELERATION_DECELERATION_WEAK_THRESHOLD &&
                globally(
                    r,
                    phi = { r.acceleration < ACCELERATION_ACCELERATION_WEAK_THRESHOLD },
                    interval =
                        AuNaTimeDifference(0) to
                            AuNaTimeDifference(MIN_DECELERATION_TO_ACCELERATION_DURATION, 0.0))
          })
    }
// endregion

// region CAM timeout

/** Timeout in nanoseconds for a CAM message to be sent. */
const val CAM_TIMEOUT_NANOS = 1_100_000_000L // 1.1s

/** Timeout in nanoseconds for a CAM message to be sent after velocity change. */
const val CAM_VELOCITY_CHANGE_TIMEOUT_NANOS = 50_000_000L // 50ms

/** Timeout in nanoseconds for a CAM message to be sent after location change. */
const val CAM_LOCATION_CHANGE_TIMEOUT_NANOS = 50_000_000L // 50ms

/** A CAM message must be sent within [CAM_TIMEOUT_NANOS] after a previous CAM message. */
val camMessageTimeout =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r1 ->
            r1.dataSource != DataSource.CAM ||
                eventually(
                    r1,
                    phi = { r2 -> r1.id == r2.id && r2.dataSource == DataSource.CAM },
                    interval = AuNaTimeDifference(0L) to AuNaTimeDifference(CAM_TIMEOUT_NANOS))
          })
    }

/** A CAM message must be sent within [CAM_VELOCITY_CHANGE_TIMEOUT_NANOS] after speed change. */
val camMessageSpeedChange =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r1 ->
            r1.dataSource != DataSource.CAM ||
                until(
                    r1,
                    phi1 = { r2 -> abs(r1.velocityCAM - r2.velocity) < 0.05 },
                    phi2 = { r2 ->
                      eventually(
                          r2,
                          phi = { r3 -> r3.dataSource == DataSource.CAM },
                          interval =
                              AuNaTimeDifference(0) to
                                  AuNaTimeDifference(CAM_VELOCITY_CHANGE_TIMEOUT_NANOS))
                    })
          })
    }

/** A CAM message must be sent within [CAM_LOCATION_CHANGE_TIMEOUT_NANOS] after location change. */
val camMessageLocationChange =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r1 ->
            r1.dataSource != DataSource.CAM ||
                until(
                    r1,
                    phi1 = { r2 ->
                      sqrt(
                          (r1.positionCAM.x - r2.position.x).pow(2) +
                              (r1.positionCAM.y - r2.position.y).pow(2)) < 0.4
                    },
                    phi2 = { r2 ->
                      eventually(
                          r2,
                          phi = { r3 -> r3.dataSource == DataSource.CAM },
                          interval =
                              AuNaTimeDifference(0) to
                                  AuNaTimeDifference(CAM_LOCATION_CHANGE_TIMEOUT_NANOS))
                    })
          })
    }

/** A CAM message must be sent within [CAM_LOCATION_CHANGE_TIMEOUT_NANOS] after rotation change. */
val camMessageRotationChange =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r1 ->
            r1.dataSource != DataSource.CAM ||
                until(
                    r1,
                    phi1 = { r2 -> abs(r1.thetaCAM - r2.rotation.yaw) < (4 * PI / 180) },
                    phi2 = { r2 ->
                      eventually(
                          r2,
                          phi = { r3 -> r3.dataSource == DataSource.CAM },
                          interval =
                              AuNaTimeDifference(0) to
                                  AuNaTimeDifference(CAM_LOCATION_CHANGE_TIMEOUT_NANOS))
                    })
          })
    }
// endregion
