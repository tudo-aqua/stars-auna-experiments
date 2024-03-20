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

// The download size is approximately 60MB!
// Manual download via: DRIVING_DATA_DOWNLOAD_URL and TRACK_DATA_DOWNLOAD_URL
val DOWNLOAD_EXPERIMENTS_DATA = true

val DRIVING_DATA_DOWNLOAD_URL = "https://tu-dortmund.sciebo.de/s/iYG2SXDAmzLt5Lb/download"
val TRACK_DATA_DOWNLOAD_URL = "https://tu-dortmund.sciebo.de/s/OKFiTtZ4Bby0Y5p/download"

val DOWNLOAD_FOLDER_NAME = "stars-auna-json-files"
val SIMULATION_RUN_FOLDER = ".\\$DOWNLOAD_FOLDER_NAME\\3_0_m_s"
val WAYPOINTS_FILE_NAME = "flw_waypoints.json"

/** The size of the window for the moving average acceleration in ms. */
val ACCELERATION_WINDOW_SIZE = 100

/** The number of segments per lane. */
val SEGMENTS_PER_LANE = 3

/** The minimum ticks per segment. */
val MIN_TICKS_PER_SEGMENT = 10

/** The hardware limit of the steering angle in radians. */
val STEERING_ANGLE_LIMIT = 0.4
