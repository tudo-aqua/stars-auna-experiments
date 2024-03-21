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

package tools.aqua.stars.auna.experiments

import tools.aqua.stars.core.tsc.*
import tools.aqua.stars.core.tsc.builder.*
import tools.aqua.stars.core.tsc.projection.proj
import tools.aqua.stars.core.tsc.projection.projRec
import tools.aqua.stars.data.av.track.*

fun tsc() =
    TSC(
        root<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference> {
          all("TSCRoot") {
            projectionIDs = mapOf(projRec("all"), proj("Driving situation"), proj("Driving maneuver"))

            all("Driving situation") {
              projectionIDs = mapOf(projRec("Driving situation"))

              exclusive("Lane Change") {
                leaf("Entering Curve") { condition = { ctx -> enteringCurve.holds(ctx) } }
                leaf("In Curve") { condition = { ctx -> inCurve.holds(ctx) } }
                leaf("Exiting Curve") { condition = { ctx -> exitingCurve.holds(ctx) } }

                leaf("Entering Straight") { condition = { ctx -> enteringStraight.holds(ctx) } }
                leaf("In Straight") { condition = { ctx -> inStraight.holds(ctx) } }
                leaf("Exiting Straight") { condition = { ctx -> exitingStraight.holds(ctx) } }
              }

              any("Distance to front vehicle") {
                leaf("None") { condition = { ctx -> ctx.primaryEntityId == 1 } }
                leaf("Normal") {
                  condition = { ctx ->
                    ctx.primaryEntityId > 1 && normalDistanceToFrontVehicle.holds(ctx)
                  }
                }
                leaf("Min") {
                  condition = { ctx ->
                    ctx.primaryEntityId > 1 && minDistanceToFrontVehicleExceeded.holds(ctx)
                  }
                }
                leaf("Max") {
                  condition = { ctx ->
                    ctx.primaryEntityId > 1 && maxDistanceToFrontVehicleExceeded.holds(ctx)
                  }
                }
              }

              exclusive("Velocity") {
                leaf("Low Velocity") { condition = { ctx -> lowVelocity.holds(ctx) } }
                leaf("High Velocity") { condition = { ctx -> highVelocity.holds(ctx) } }
                leaf("Max Velocity") { condition = { ctx -> maxVelocity.holds(ctx) } }
              }
            }

            all("Driving maneuver") {
              projectionIDs = mapOf(projRec(("Driving maneuver")))

              any("Acceleration") {
                leaf("Weak Deceleration") { condition = { ctx -> weakDeceleration.holds(ctx) } }
                leaf("Strong Deceleration") { condition = { ctx -> strongDeceleration.holds(ctx) } }
                leaf("Weak Acceleration") { condition = { ctx -> weakAcceleration.holds(ctx) } }
                leaf("Strong Acceleration") { condition = { ctx -> strongAcceleration.holds(ctx) } }
                leaf("No Acceleration (Driving)") { condition = { ctx -> noAcceleration.holds(ctx) } }
              }

              any("Steering Angle") {
                leaf("No Steering") {
                  condition = { ctx ->
                    ctx.entityIds.any { entityId -> noSteering.holds(ctx, entityId = entityId) }
                  }
                }
                leaf("Low Steering") {
                  condition = { ctx ->
                    ctx.entityIds.any { entityId -> lowSteering.holds(ctx, entityId = entityId) }
                  }
                }
                leaf("Hard Steering") {
                  condition = { ctx ->
                    ctx.entityIds.any { entityId -> hardSteering.holds(ctx, entityId = entityId) }
                  }
                }
              }
            }

            // region monitors
            leaf("Max lateral offset") {
              condition = { _ -> true }
              monitorFunction = { ctx -> normalLateralOffset.holds(ctx) }
            }
            leaf("Minimum distance to front robot") {
              condition = { _ -> true }
              monitorFunction = { ctx -> minDistanceToFrontVehicleExceeded.holds(ctx) }
            }
            leaf("Maximum deceleration") {
              condition = { _ -> true }
              monitorFunction = { ctx -> strongDeceleration.holds(ctx) }
            }
            leaf("CAM message timeout") {
              condition = { _ -> true }
              monitorFunction = { ctx -> camMessageTimeout.holds(ctx) }
            }
            // endregion
          }
        })
