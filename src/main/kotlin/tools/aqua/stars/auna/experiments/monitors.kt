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

@file:Suppress("MayBeConstant")

package tools.aqua.stars.auna.experiments

import kotlin.math.*
import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.data.av.track.AuNaTimeDifference
import tools.aqua.stars.data.av.track.DataSource
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.logic.kcmftbl.eventually
import tools.aqua.stars.logic.kcmftbl.globally

// region lateral offset
/*
 * The lateral offset of the robot in m.
 */

/** Exceeding the maximum lateral offset defined as > [MAX_LATERAL_OFFSET]. */
val MAX_LATERAL_OFFSET: Double = 0.4

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
val MAXIMUM_DECELERATION_THRESHOLD: Double = -3.0

/** Exceeding the maximum deceleration is defined as > [MAXIMUM_DECELERATION_THRESHOLD]. */
val maxDecelerationExceeded =
    predicate(Robot::class) { _, r ->
      globally(r, phi = { it.acceleration > MAXIMUM_DECELERATION_THRESHOLD })
    }
// endregion

// region CAM timeout

/** Timeout slack in nanoseconds for a CAM message to be sent. */
val CAM_SLACK_NANOS = 20_000_000L // 5ms

/** A CAM message must be sent within [CAM_SLACK_NANOS] after a previous CAM message. */
val camMessageTimeout =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r1 ->
            r1.dataSource != DataSource.CAM ||
                eventually(
                    r1,
                    phi = { r2 -> r1.id == r2.id && r2.dataSource == DataSource.CAM },
                    interval =
                        AuNaTimeDifference(1) to
                            AuNaTimeDifference(1_000_000_000 + CAM_SLACK_NANOS))
          })
    }

/** A CAM message must be sent within [CAM_SLACK_NANOS] after speed change. */
val camMessageSpeedChange =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r1 ->
            r1.dataSource != DataSource.CAM ||
                globally(
                    r1,
                    phi = { r2 -> r1.id != r2.id || abs(r1.velocityCAM - r2.velocity) < 0.05 }) ||
                eventually(
                    r1,
                    phi = { r2 ->
                      r1.id == r2.id &&
                          abs(r1.velocityCAM - r2.velocity) >= 0.05 &&
                          eventually(
                              r2,
                              phi = { r3 ->
                                r2 !== r3 && r2.id == r3.id && r3.dataSource == DataSource.CAM
                              },
                              interval =
                                  AuNaTimeDifference(1) to AuNaTimeDifference(CAM_SLACK_NANOS))
                    })
          })
    }

/** A CAM message must be sent within [CAM_SLACK_NANOS] after location change. */
val camMessageLocationChange =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r1 ->
            r1.dataSource != DataSource.CAM ||
                globally(r1, phi = { r2 -> r1.id != r2.id || posDiff(r1, r2) < 0.4 }) ||
                eventually(
                    r1,
                    phi = { r2 ->
                      r1.id == r2.id &&
                          posDiff(r1, r2) >= 0.4 &&
                          eventually(
                              r2,
                              phi = { r3 ->
                                r2 !== r3 && r2.id == r3.id && r3.dataSource == DataSource.CAM
                              },
                              interval =
                                  AuNaTimeDifference(1) to AuNaTimeDifference(CAM_SLACK_NANOS))
                    })
          })
    }

private fun posDiff(r1: Robot, r2: Robot) =
    sqrt((r1.positionCAM.x - r2.position.x).pow(2) + (r1.positionCAM.y - r2.position.y).pow(2))

/** A CAM message must be sent within [CAM_SLACK_NANOS] after rotation change. */
val camMessageRotationChange =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          phi = { r1 ->
            r1.dataSource != DataSource.CAM ||
                globally(r1, phi = { r2 -> r1.id != r2.id || yawDiff(r1, r2) < (4 * PI / 180) }) ||
                eventually(
                    r1,
                    phi = { r2 ->
                      r1.id == r2.id &&
                          yawDiff(r1, r2) >= (4 * PI / 180) &&
                          eventually(
                              r2,
                              phi = { r3 ->
                                r2 !== r3 && r2.id == r3.id && r3.dataSource == DataSource.CAM
                              },
                              interval =
                                  AuNaTimeDifference(1) to AuNaTimeDifference(CAM_SLACK_NANOS))
                    })
          })
    }

private fun yawDiff(r1: Robot, r2: Robot) = abs(r1.thetaCAM - r2.rotation.yaw)
// endregion
