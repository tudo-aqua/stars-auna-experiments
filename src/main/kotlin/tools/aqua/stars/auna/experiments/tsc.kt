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
import tools.aqua.stars.core.tsc.builder.all
import tools.aqua.stars.core.tsc.builder.exclusive
import tools.aqua.stars.core.tsc.builder.leaf
import tools.aqua.stars.core.tsc.builder.root
import tools.aqua.stars.core.tsc.projection.projRec
import tools.aqua.stars.data.av.track.*

fun tsc() =
    TSC(
        root<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference> {
          all("TSCRoot") {
            projectionIDs = mapOf(projRec(("all")))
            leaf("Max lateral offset") {
              condition = { ctx ->
                ctx.entityIds.all { normalLateralOffset.holds(ctx, entityId = it) }
              }
              monitorFunction = { ctx ->
                ctx.entityIds.all { normalLateralOffset.holds(ctx, entityId = it) }
              }
            }
            leaf("Max distance to front robot") {
              condition = { ctx ->
                ctx.entityIds.any { robotId1 ->
                  ctx.entityIds.any { robotId2 ->
                    robotId1 != robotId2 &&
                        normalDistanceToPreviousVehicle.holds(
                            ctx, entityId1 = robotId1, entityId2 = robotId2)
                  }
                }
              }
              monitorFunction = { ctx ->
                ctx.entityIds.any { robotId1 ->
                  ctx.entityIds.any { robotId2 ->
                    robotId1 != robotId2 &&
                        normalDistanceToPreviousVehicle.holds(
                            ctx, entityId1 = robotId1, entityId2 = robotId2)
                  }
                }
              }
            }
            exclusive("Lane Type") {
              leaf("Straight Lane") { condition = { ctx -> isOnStraightLane.holds(ctx) } }
              leaf("Curved Lane") { condition = { ctx -> isOnCurvedLane.holds(ctx) } }
            }
          }
        })
