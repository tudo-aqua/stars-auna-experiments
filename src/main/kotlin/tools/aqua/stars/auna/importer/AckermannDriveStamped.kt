/*
 * Copyright 2024 The STARS AuNa Experiments Authors
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

package tools.aqua.stars.auna.importer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AckermannDriveStamped(
    @SerialName("msg") override val header: tools.aqua.stars.auna.importer.Header,
    @SerialName("drive") val ackermannDrive: tools.aqua.stars.auna.importer.AckermannDrive
) : tools.aqua.stars.auna.importer.Message