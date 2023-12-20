/*
 * Copyright 2023 The STARS AuNa Experiments Authors
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
import tools.aqua.stars.data.av.track.Robot
import tools.aqua.stars.data.av.track.Segment
import tools.aqua.stars.data.av.track.TickData

fun tsc() =
    TSC(
        root<Robot, TickData, Segment> {
          all("TSCRoot") {
            projectionIDs = mapOf(projRec(("all")))
            leaf("Max lateral offset") { monitorFunction = { ctx -> maxLateralOffset.holds(ctx) } }
            leaf("Max distance to front robot") {
              monitorFunction = { ctx ->
                ctx.entityIds.any { robotId1 ->
                  ctx.entityIds.any { robotId2 ->
                    robotId1 != robotId2 &&
                        maxDistanceToPreviousVehicle.holds(
                            ctx, actor1Id = robotId1, actor2Id = robotId2)
                  }
                }
              }
            }
          }
        })