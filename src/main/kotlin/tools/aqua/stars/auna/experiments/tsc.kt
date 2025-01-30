/*
 * Copyright 2023-2025 The STARS AuNa Experiments Authors
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

@file:Suppress("StringLiteralDuplication")

package tools.aqua.stars.auna.experiments

import tools.aqua.stars.core.tsc.*
import tools.aqua.stars.core.tsc.builder.*
import tools.aqua.stars.data.av.track.*

@Suppress("LongMethod")
/** The TSC for the AuNa experiments. */
fun tsc() =
    TSC(
        root<Robot, TickData, Segment, AuNaTimeUnit, AuNaTimeDifference> {
          all("TSCRoot") {
            projections {
              projectionRecursive("all")
              projection("Driving situation")
              projection("Position on Track")
              projection("Distance to front vehicle")
              projection("Velocity")
              projection("Driving maneuver")
              projection("Acceleration")
              projection("Steering Angle")
            }

            all("Driving situation") {
              projections {
                projectionRecursive("Driving situation")
                projection("Position on Track")
                projection("Distance to front vehicle")
                projection("Velocity")
              }

              all("Position on Track") {
                projections { projectionRecursive("Position on Track") }

                exclusive("Lane Type") {
                  leaf("Top Straight") { condition { ctx -> topStraight.holds(ctx) } }
                  leaf("Bottom Straight") { condition { ctx -> bottomStraight.holds(ctx) } }
                  leaf("Wide Curve") { condition { ctx -> wideCurve.holds(ctx) } }
                  leaf("Tight Curve") { condition { ctx -> tightCurve.holds(ctx) } }
                }

                exclusive("Lane Section") {
                  leaf("Entering") { condition { ctx -> entering.holds(ctx) } }
                  leaf("Middle") { condition { ctx -> middle.holds(ctx) } }
                  leaf("Exiting") { condition { ctx -> leaving.holds(ctx) } }
                }
              }

              exclusive("Distance to front vehicle") {
                projections { projectionRecursive("Distance to front vehicle") }
                leaf("High distance to front vehicle") {
                  condition { ctx -> highDistanceToFrontVehicle.holds(ctx) }
                }
                leaf("Normal distance to front vehicle") {
                  condition { ctx -> normalDistanceToFrontVehicle.holds(ctx) }
                }
                leaf("Low distance to front vehicle") {
                  condition { ctx -> lowDistanceToFrontVehicle.holds(ctx) }
                }
              }

              exclusive("Velocity") {
                projections { projectionRecursive("Velocity") }
                leaf("Low Velocity") { condition { ctx -> lowVelocity.holds(ctx) } }
                leaf("High Velocity") { condition { ctx -> highVelocity.holds(ctx) } }
                leaf("Max Velocity") { condition { ctx -> maxVelocity.holds(ctx) } }
              }
            }

            all("Driving maneuver") {
              projections {
                projectionRecursive("Driving maneuver")
                projection("Acceleration")
                projection("Steering Angle")
              }

              exclusive("Acceleration") {
                projections { projectionRecursive("Acceleration") }
                leaf("Weak Deceleration") { condition { ctx -> weakDeceleration.holds(ctx) } }
                leaf("Strong Deceleration") { condition { ctx -> strongDeceleration.holds(ctx) } }
                leaf("Weak Acceleration") { condition { ctx -> weakAcceleration.holds(ctx) } }
                leaf("Strong Acceleration") { condition { ctx -> strongAcceleration.holds(ctx) } }
              }

              any("Steering Angle") {
                projections { projectionRecursive("Steering Angle") }
                leaf("No Steering") {
                  condition { ctx ->
                    ctx.entityIds.any { entityId -> noSteering.holds(ctx, entityId = entityId) }
                  }
                }
                leaf("Low Steering") {
                  condition { ctx ->
                    ctx.entityIds.any { entityId -> lowSteering.holds(ctx, entityId = entityId) }
                  }
                }
                leaf("Hard Steering") {
                  condition { ctx ->
                    ctx.entityIds.any { entityId -> hardSteering.holds(ctx, entityId = entityId) }
                  }
                }
              }
            }

            monitors {
              monitor("Max lateral offset") { normalLateralOffset.holds(it) }
              monitor("CAM message timeout") { camMessageTimeout.holds(it) }
              monitor("CAM message speed change") { camMessageSpeedChange.holds(it) }
              monitor("CAM message location change") { camMessagePositionChange.holds(it) }
              monitor("CAM message rotation change") { camMessageHeadingChange.holds(it) }
              monitor("Maximum deceleration") { maxDecelerationExceeded.holds(it) }
              monitor("Minimum distance to front robot") { minDistanceToPrecedingVehicle.holds(it) }
              monitor("Maximum distance to front robot") { maxDistanceToPrecedingVehicle.holds(it) }
            }
          }
        })
