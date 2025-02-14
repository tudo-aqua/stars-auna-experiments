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

package tools.aqua.stars.auna.importer

import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import tools.aqua.stars.auna.experiments.STATIC_DATA_FILE
import tools.aqua.stars.data.av.track.DataSource

/**
 * This function imports all file contents under the given [folderPath] and returns a map which maps
 * the [DataSource] to its contents.
 *
 * @param folderPath The folder in which the files to import are stored
 * @return A [Map] from a [DataSource] to a [List] of its contents
 */
fun importDrivingData(folderPath: Path): Map<DataSource, List<Any>> {
  // Get all files for the given folderPath
  val folderContents = folderPath.toFile().walk().filter { it.isFile }.toList()
  require(folderContents.any()) { "There is no content in the folder '$folderPath'" }
  val dataSourceToContentMap = mutableMapOf<DataSource, MutableList<Any>>()
  dataSourceToContentMap.putIfAbsent(DataSource.CAM, mutableListOf())
  dataSourceToContentMap.putIfAbsent(DataSource.ODOMETRY, mutableListOf())
  dataSourceToContentMap.putIfAbsent(DataSource.VICON_POSE, mutableListOf())
  dataSourceToContentMap.putIfAbsent(DataSource.ACKERMANN_CMD, mutableListOf())
  folderContents.forEach { currentFile ->
    if (currentFile.nameWithoutExtension.contains("cam")) {
      dataSourceToContentMap[DataSource.CAM]?.addAll(
          getJsonContentOfPath<List<CAM>>(currentFile.toPath()))
    } else if (currentFile.nameWithoutExtension.contains("odom")) {
      dataSourceToContentMap[DataSource.ODOMETRY]?.addAll(
          getJsonContentOfPath<List<Odometry>>(currentFile.toPath()))
    } else if (currentFile.nameWithoutExtension.contains("vicon_pose")) {
      dataSourceToContentMap[DataSource.VICON_POSE]?.addAll(
          getJsonContentOfPath<List<ViconPose>>(currentFile.toPath()))
    } else if (currentFile.nameWithoutExtension.contains("ackermann_cmd")) {
      dataSourceToContentMap[DataSource.ACKERMANN_CMD]?.addAll(
          getJsonContentOfPath<List<AckermannDriveStamped>>(currentFile.toPath()))
    } else {
      error("Unknown file contents: $currentFile")
    }
  }
  return dataSourceToContentMap
}

/**
 * Import the [Track] data from the JSON file.
 *
 * @return The parsed [Track] object.
 */
fun importTrackData(): Track = getJsonContentOfPath(Path(STATIC_DATA_FILE))

/**
 * Returns the parsed Json content for the given [inputFilePath]. Currently supported file
 * extensions: ".json", ".zip". The generic parameter [T] specifies the class to which the content
 * should be parsed to
 */
@Suppress("DuplicatedCode")
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> getJsonContentOfPath(inputFilePath: Path): T {
  // Create JsonBuilder with correct settings
  val jsonBuilder = Json {
    prettyPrint = true
    isLenient = true
  }

  // Check if inputFilePath exists
  check(inputFilePath.exists()) { "The given file path does not exist: ${inputFilePath.toUri()}" }

  // Check whether the given inputFilePath is a directory
  check(!inputFilePath.isDirectory()) {
    "Cannot get InputStream for directory. Path: $inputFilePath"
  }

  // If ".json"-file: Just return InputStream of file
  if (inputFilePath.extension == "json") {
    return jsonBuilder.decodeFromStream<T>(inputFilePath.inputStream())
  }

  // if ".zip"-file: Extract single archived file
  if (inputFilePath.extension == "zip") {
    // https://stackoverflow.com/a/46644254
    ZipFile(File(inputFilePath.toUri())).use { zip ->
      zip.entries().asSequence().forEach { entry ->
        // Add InputStream to inputStreamBuffer
        return jsonBuilder.decodeFromStream<T>(zip.getInputStream(entry))
      }
    }
  }

  // If none of the supported file extensions is present, throw an Exception
  error(
      "Unexpected file extension: ${inputFilePath.extension}. Supported extensions: '.json', '.zip'")
}
