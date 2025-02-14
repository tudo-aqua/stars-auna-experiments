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

package tools.aqua.stars.auna.experiments

/** Directory of the dynamic data json files. */
const val DYNAMIC_DATA_DIRECTORY = ".\\stars-auna-json-files\\3_0_m_s"

/** Directory of the static data json file. */
const val STATIC_DATA_FILE = ".\\stars-auna-json-files\\flw_waypoints.json"

/** The size of the window for the moving average acceleration in ms. */
const val ACCELERATION_WINDOW_SIZE = 100

/** The minimum ticks per segment. */
const val MIN_TICKS_PER_SEGMENT = 10

/** The hardware limit of the steering angle in radians. */
const val STEERING_ANGLE_LIMIT = 0.4
