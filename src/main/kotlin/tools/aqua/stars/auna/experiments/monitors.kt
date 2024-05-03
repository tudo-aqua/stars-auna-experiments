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
val minDistanceToPrecedingVehicle =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          r.tickData.getEntityById(r.id - 1)!!,
          phi = { rb1, rb2,
            ->
            // Distance in m to front vehicle is smaller than breaking distance,
            // i.e. ((v * 3.6) / 10) ^ 2
            rb1.distanceToOther(rb2) > (0.36 * rb1.velocity).pow(2)
          })
    }

/**
 * Exceeding the maximum distance to the front vehicle is defined
 * as > [DISTANCE_TO_FRONT_ROBOT_THRESHOLD_HIGH] * 1.25.
 */
val maxDistanceToPrecedingVehicle =
    predicate(Robot::class) { _, r ->
      globally(
          r,
          r.tickData.getEntityById(r.id - 1)!!,
          phi = { rb1, rb2 ->
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
/*
 * Timeout for CAM messages that should be sent.
 */

/** Timeout slack in nanoseconds for a CAM message to be sent. */
val CAM_SLACK_NANOS = 5_000_000L // 5ms

/** Eventually there is a CAM message within the next [millis] ms + slack. default: 0ms. */
fun nextCAMMessageInTime(millis: Int = 0) =
    predicate(Robot::class) { _, r ->
      eventually(
          r,
          phi = { r1 -> r !== r1 && r.id == r1.id && r1.dataSource == DataSource.CAM },
          interval =
              AuNaTimeDifference(0) to AuNaTimeDifference(millis * 1_000_000 + CAM_SLACK_NANOS))
    }

/** There are no more ticks [millis] ms from now. default: 0ms. */
fun noMoreTicks(millis: Int = 0) =
    predicate(Robot::class) { _, r ->
      !eventually(
          r,
          phi = { true },
          interval =
              AuNaTimeDifference(millis * 1_000_000 + CAM_SLACK_NANOS) to
                  AuNaTimeDifference(Long.MAX_VALUE))
    }

/** There are no more speed changes in this segment. */
val noFurtherSpeedChanges =
    predicate(Robot::class) { _, r ->
      globally(r, phi = { r1 -> r.id != r1.id || abs(r.velocityCAM - r1.velocity) < 0.05 })
    }

/** There are no more position changes in this segment. */
val noFurtherPositionChanges =
    predicate(Robot::class) { _, r ->
      globally(r, phi = { r1 -> r.id != r1.id || posDiff(r, r1) < 0.4 })
    }

/** There are no more heading changes in this segment. */
val noFurtherHeadingChanges =
    predicate(Robot::class) { _, r ->
      globally(r, phi = { r1 -> r.id != r1.id || yawDiff(r, r1) < (4 * PI / 180) })
    }

/**
 * A CAM message must be sent within [CAM_SLACK_NANOS] after a previous CAM message.
 *
 * I.e. For this robot there are no more ticks within the next 1000ms OR there is a CAM message
 * within the next 1000ms + slack.
 */
val camMessageTimeout =
    predicate(Robot::class) { ctx, r ->
      globally(
          r,
          phi = { r1 ->
            // This tick is no CAM message
            r.id != r1.id ||
                r1.dataSource != DataSource.CAM ||
                noMoreTicks(1000).holds(ctx, r1) ||
                nextCAMMessageInTime(1000).holds(ctx, r1)
          })
    }

/**
 * A CAM message must be sent within [CAM_SLACK_NANOS] after speed change.
 *
 * I.e. For this robot there are no more speed changes OR there is a speed change for this robot AND
 * from then there are either no more ticks within the slack time OR there is a CAM message within
 * the slack time.
 */
val camMessageSpeedChange =
    predicate(Robot::class) { ctx, r ->
      globally(
          r,
          phi = { r1 ->
            r.id != r1.id ||
                r1.dataSource != DataSource.CAM ||
                noFurtherSpeedChanges.holds(ctx, r1) ||
                eventually(
                    r1,
                    phi = { r2 ->
                      r1 !== r2 &&
                          r1.id == r2.id &&
                          abs(r1.velocityCAM - r2.velocity) >= 0.05 &&
                          (noMoreTicks().holds(ctx, r2) || nextCAMMessageInTime().holds(ctx, r2))
                    })
          })
    }

/**
 * A CAM message must be sent within [CAM_SLACK_NANOS] after location change.
 *
 * I.e. For this robot there are no more location changes OR there is a location change for this
 * robot AND from then there are either no more ticks within the slack time OR there is a CAM
 * message within the slack time.
 */
val camMessagePositionChange =
    predicate(Robot::class) { ctx, r ->
      globally(
          r,
          phi = { r1 ->
            r.id != r1.id ||
                r1.dataSource != DataSource.CAM ||
                noFurtherPositionChanges.holds(ctx, r1) ||
                eventually(
                    r1,
                    phi = { r2 ->
                      r1 !== r2 &&
                          r1.id == r2.id &&
                          posDiff(r1, r2) >= 0.4 &&
                          (noMoreTicks().holds(ctx, r2) || nextCAMMessageInTime().holds(ctx, r2))
                    })
          })
    }

private fun posDiff(r1: Robot, r2: Robot) =
    sqrt((r1.positionCAM.x - r2.position.x).pow(2) + (r1.positionCAM.y - r2.position.y).pow(2))

/**
 * A CAM message must be sent within [CAM_SLACK_NANOS] after heading change.
 *
 * I.e. For this robot there are no more heading changes OR there is a heading change for this robot
 * AND from then there are either no more ticks within the slack time OR there is a CAM message
 * within the slack time.
 */
val camMessageHeadingChange =
    predicate(Robot::class) { ctx, r ->
      globally(
          r,
          phi = { r1 ->
            r.id != r1.id ||
                r1.dataSource != DataSource.CAM ||
                noFurtherHeadingChanges.holds(ctx, r1) ||
                eventually(
                    r1,
                    phi = { r2 ->
                      r1 !== r2 &&
                          r1.id == r2.id &&
                          yawDiff(r1, r2) >= (4 * PI / 180) &&
                          (noMoreTicks().holds(ctx, r2) || nextCAMMessageInTime().holds(ctx, r2))
                    })
          })
    }

private fun yawDiff(r1: Robot, r2: Robot) = abs(r1.thetaCAM - r2.rotation.yaw)
// endregion
