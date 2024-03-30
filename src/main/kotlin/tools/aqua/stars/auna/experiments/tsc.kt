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
            projectionIDs =
                mapOf(projRec("all"), proj("Driving situation"), proj("Driving maneuver"))

            all("Driving situation") {
              projectionIDs = mapOf(projRec("Driving situation"))

              exclusive("Lane Change") {
                leaf("Entering Straight") { condition = { ctx -> enteringStraight.holds(ctx) } }
                leaf("In Straight") { condition = { ctx -> inStraight.holds(ctx) } }
                leaf("Exiting Straight") { condition = { ctx -> leavingStraight.holds(ctx) } }

                leaf("Entering Tight Curve") {
                  condition = { ctx -> enteringTightCurve.holds(ctx) }
                }
                leaf("In Tight Curve") { condition = { ctx -> inTightCurve.holds(ctx) } }
                leaf("Exiting Tight Curve") { condition = { ctx -> leavingTightCurve.holds(ctx) } }

                leaf("Entering Wide Curve") { condition = { ctx -> enteringWideCurve.holds(ctx) } }
                leaf("In Wide Curve") { condition = { ctx -> inWideCurve.holds(ctx) } }
                leaf("Exiting Wide Curve") { condition = { ctx -> leavingWideCurve.holds(ctx) } }
              }

              any("Distance to front vehicle") {
                leaf("High") { condition = { ctx -> highDistanceToFrontVehicle.holds(ctx) } }
                leaf("Normal") { condition = { ctx -> normalDistanceToFrontVehicle.holds(ctx) } }
                leaf("Low") { condition = { ctx -> lowDistanceToFrontVehicle.holds(ctx) } }
              }

              exclusive("Velocity") {
                leaf("Low Velocity") { condition = { ctx -> lowVelocity.holds(ctx) } }
                leaf("High Velocity") { condition = { ctx -> highVelocity.holds(ctx) } }
                leaf("Max Velocity") { condition = { ctx -> maxVelocity.holds(ctx) } }
              }
            }

            all("Driving maneuver") {
              projectionIDs = mapOf(projRec(("Driving maneuver")))

              exclusive("Acceleration") {
                leaf("Weak Deceleration") { condition = { ctx -> weakDeceleration.holds(ctx) } }
                leaf("Strong Deceleration") { condition = { ctx -> strongDeceleration.holds(ctx) } }
                leaf("Weak Acceleration") { condition = { ctx -> weakAcceleration.holds(ctx) } }
                leaf("Strong Acceleration") { condition = { ctx -> strongAcceleration.holds(ctx) } }
                leaf("No Acceleration") { condition = { ctx -> noAcceleration.holds(ctx) } }
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

            all("Monitors") {
              leaf("Max lateral offset") {
                condition = { _ -> true }
                monitorFunction = { normalLateralOffset.holds(it) }
              }
              leaf("CAM message timeout") {
                condition = { _ -> true }
                monitorFunction = { camMessageTimeout.holds(it) }
              }
              leaf("Maximum deceleration") {
                condition = { _ -> true }
                monitorFunction = { maxDecelerationExceeded.holds(it) }
              }
              leaf("Short acceleration phase") {
                condition = { _ -> true }
                monitorFunction = { noShortAccelerationToDecelerationTransition.holds(it) }
              }
              leaf("Short deceleration phase") {
                condition = { _ -> true }
                monitorFunction = { noShortDecelerationToAccelerationTransition.holds(it) }
              }
              leaf("Minimum distance to front robot") {
                condition = { _ -> true }
                monitorFunction = { minDistanceToFrontVehicleExceeded.holds(it) }
              }
              leaf("Maximum distance to front robot") {
                condition = { _ -> true }
                monitorFunction = { maxDistanceToFrontVehicleExceeded.holds(it) }
              }
            }
          }
        })
