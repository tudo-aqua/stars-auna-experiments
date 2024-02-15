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

@file:Suppress("DuplicatedCode")

package tools.aqua.stars.auna.experiments

import tools.aqua.stars.core.tsc.TSC
import tools.aqua.stars.core.tsc.builder.all
import tools.aqua.stars.core.tsc.builder.exclusive
import tools.aqua.stars.core.tsc.builder.leaf
import tools.aqua.stars.core.tsc.builder.root
import tools.aqua.stars.core.tsc.projection.projRec
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

fun tsc() =
  TSC(
    root<Robot, TickData, Segment> {
      all("TSCRoot") {
        projectionIDs = mapOf(projRec(("all")))
        exclusive("Lateral offset") {
          projectionIDs = mapOf(projRec(("lateralOffset")))
          leaf("Normal lateral offset") {
            condition = { ctx -> ctx.entityIds.all { normalLateralOffset.holds(ctx, actorId = it) } }
          }
          leaf("Maximum lateral offset exceeded") {
            condition = { ctx -> ctx.entityIds.all { maxLateralOffsetExceeded.holds(ctx, actorId = it) } }
            monitorFunction = { ctx ->
              ctx.entityIds.all { maxLateralOffsetExceeded.holds(ctx, actorId = it) }
            }
          }
        }
        exclusive("Distance to front robot") {
          projectionIDs = mapOf(projRec(("Distance to front robot")))
          leaf("Normal distance to front robot") {
            condition = { ctx ->
              ctx.entityIds.any { robotId1 ->
                ctx.entityIds.any { robotId2 ->
                  robotId1 != robotId2 &&
                      normalDistanceToPreviousVehicle.holds(ctx, actor1 = robotId1, actor2 = robotId2)
                }
              }
            }
          }
          leaf("Maximum distance to front robot exceeded") {
            condition = { ctx ->
              ctx.entityIds.any { robotId1 ->
                ctx.entityIds.any { robotId2 ->
                  robotId1 != robotId2 &&
                      maxDistanceToPreviousVehicleExceeded.holds(ctx, actor1 = robotId1, actor2 = robotId2)
                }
              }
            }
            monitorFunction = { ctx ->
              ctx.entityIds.any { robotId1 ->
                ctx.entityIds.any { robotId2 ->
                  robotId1 != robotId2 &&
                      maxDistanceToPreviousVehicleExceeded.holds(ctx, actor1 = robotId1, actor2 = robotId2)
                }
              }
            }
          }
          leaf("Minimal distance to front robot exceeded") {
            condition = { ctx ->
              ctx.entityIds.any { robotId1 ->
                ctx.entityIds.any { robotId2 ->
                  robotId1 != robotId2 &&
                      minDistanceToPreviousVehicleExceeded.holds(ctx, actor1 = robotId1, actor2 = robotId2)
                }
              }
            }
            monitorFunction = { ctx ->
              ctx.entityIds.any { robotId1 ->
                ctx.entityIds.any { robotId2 ->
                  robotId1 != robotId2 &&
                      minDistanceToPreviousVehicleExceeded.holds(ctx, actor1 = robotId1, actor2 = robotId2)
                }
              }
            }
          }
        }
        exclusive("Acceleration") {
          projectionIDs = mapOf(projRec(("Acceleration")))
          leaf("Strong acceleration") {
            condition = { ctx -> ctx.entityIds.any { r -> strongAcceleration.holds(ctx, actorId = r) } }
          }
          leaf("Weak acceleration") {
            condition = { ctx -> ctx.entityIds.any { r -> weakAcceleration.holds(ctx, actorId = r) } }
          }
          leaf("No acceleration") {
            condition = { ctx -> ctx.entityIds.any { r -> noAcceleration.holds(ctx, actorId = r) } }
          }
          leaf("Weak deceleration") {
            condition = { ctx -> ctx.entityIds.any { r -> weakDeceleration.holds(ctx, actorId = r) } }
          }
          leaf("Strong deceleration") {
            condition = { ctx -> ctx.entityIds.any { r -> strongDeceleration.holds(ctx, actorId = r) } }
          }
        }
      }
    })
