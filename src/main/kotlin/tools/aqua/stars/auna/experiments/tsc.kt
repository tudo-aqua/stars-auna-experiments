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
import tools.aqua.stars.core.tsc.projection.projRec
import tools.aqua.stars.data.av.track.*

fun tsc() =
    TSC(
        root<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference> {
          all("TSCRoot") {
            projectionIDs = mapOf(projRec(("all")))
            leaf("Max lateral offset") {
              condition = { _ -> true }
              monitorFunction = { ctx ->
                ctx.entityIds.all { normalLateralOffset.holds(ctx, entityId = it) }
              }
            }
            exclusive("Lane Type") {
              leaf("Straight Lane") { condition = { ctx -> isOnStraightLane.holds(ctx) } }
              leaf("Curved Lane") { condition = { ctx -> isOnCurvedLane.holds(ctx) } }
            }

            any("Distance to front vehicle") {
              leaf("None") { condition = { ctx -> ctx.primaryEntityId == 1 } }
              leaf("Normal") {
                condition = { ctx ->
                  ctx.primaryEntityId > 1 &&
                      normalDistanceToPreviousVehicle.holds(
                          ctx, entityId1 = ctx.primaryEntityId, entityId2 = ctx.primaryEntityId - 1)
                }
              }
              leaf("Min") {
                condition = { ctx ->
                  ctx.primaryEntityId > 1 &&
                      minDistanceToPreviousVehicleExceeded.holds(
                          ctx, entityId1 = ctx.primaryEntityId, entityId2 = ctx.primaryEntityId - 1)
                }
              }
              leaf("Max") {
                condition = { ctx ->
                  ctx.primaryEntityId > 1 &&
                      maxDistanceToPreviousVehicleExceeded.holds(
                          ctx, entityId1 = ctx.primaryEntityId, entityId2 = ctx.primaryEntityId - 1)
                }
              }
            }

            any("Acceleration") {
              leaf("Weak Deceleration") { condition = { ctx -> weakDeceleration.holds(ctx) } }
              leaf("Strong Deceleration") { condition = { ctx -> strongDeceleration.holds(ctx) } }
              leaf("Weak Acceleration") { condition = { ctx -> weakAcceleration.holds(ctx) } }
              leaf("Strong Acceleration") { condition = { ctx -> strongAcceleration.holds(ctx) } }
              leaf("No Acceleration (Driving)") { condition = { ctx -> noAcceleration.holds(ctx) } }
            }
          }
        })
