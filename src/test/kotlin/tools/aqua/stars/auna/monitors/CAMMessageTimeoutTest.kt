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

package tools.aqua.stars.auna.monitors

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tools.aqua.stars.auna.experiments.camMessageTimeout
import tools.aqua.stars.core.evaluation.PredicateContext
import tools.aqua.stars.data.av.track.*

class CAMMessageTimeoutTest {

  @Test
  fun `No CAM message`() {
    val segment =
        Segment(segmentId = 1, ticks = mutableMapOf(), nextSegment = null, previousSegment = null)
    val tick1 =
        TickData(currentTick = AuNaTimeUnit(0.0, 0.0), entities = listOf()).apply {
          this.segment = segment
        }
    val robot1 =
        Robot(tick1).apply {
          dataSource = DataSource.NOT_SET
          isPrimaryEntity = true
        }
    tick1.entities = listOf(robot1)
    segment.ticks = mapOf(tick1.currentTick to tick1)

    assertTrue {
      camMessageTimeout.holds(
          PredicateContext(segment), segment.tickData.first().getById(robot1.id))
    }
  }

  @Test
  fun `Only one CAM message`() {
    val segment =
        Segment(segmentId = 1, ticks = mutableMapOf(), nextSegment = null, previousSegment = null)
    val tick1 =
        TickData(currentTick = AuNaTimeUnit(0.0, 0.0), entities = listOf()).apply {
          this.segment = segment
        }
    val robot1 =
        Robot(tick1).apply {
          dataSource = DataSource.CAM
          isPrimaryEntity = true
        }
    tick1.entities = listOf(robot1)
    segment.ticks = mapOf(tick1.currentTick to tick1)

    assertTrue {
      camMessageTimeout.holds(
          PredicateContext(segment), segment.tickData.first().getById(robot1.id))
    }
  }

  @Test
  fun `One CAM and one other message`() {
    val segment =
        Segment(segmentId = 1, ticks = mutableMapOf(), nextSegment = null, previousSegment = null)

    val tick1 =
        TickData(currentTick = AuNaTimeUnit(0.0, 0.0), entities = listOf(), id = 1).apply {
          this.segment = segment
        }
    val robot1 =
        Robot(tick1).apply {
          dataSource = DataSource.CAM
          isPrimaryEntity = true
        }
    tick1.entities = listOf(robot1)

    val tick2 =
        TickData(currentTick = AuNaTimeUnit(1.0, 0.0), entities = listOf(), id = 2).apply {
          this.segment = segment
        }
    val robot2 =
        Robot(tick2).apply {
          dataSource = DataSource.NOT_SET
          isPrimaryEntity = true
        }
    tick2.entities = listOf(robot2)

    segment.ticks = mapOf(tick1.currentTick to tick1, tick2.currentTick to tick2)

    assertTrue {
      camMessageTimeout.holds(
          PredicateContext(segment), segment.tickData.first().getById(robot1.id))
    }
  }

  @Test
  fun `CAM Message is last message in list of messages`() {
    val segment =
        Segment(segmentId = 1, ticks = mutableMapOf(), nextSegment = null, previousSegment = null)

    val tick1 =
        TickData(currentTick = AuNaTimeUnit(0.0, 0.0), entities = listOf(), id = 1).apply {
          this.segment = segment
        }
    val robot1 =
        Robot(tick1).apply {
          dataSource = DataSource.NOT_SET
          isPrimaryEntity = true
        }
    tick1.entities = listOf(robot1)

    val tick2 =
        TickData(currentTick = AuNaTimeUnit(1.0, 0.0), entities = listOf(), id = 2).apply {
          this.segment = segment
        }
    val robot2 =
        Robot(tick2).apply {
          dataSource = DataSource.NOT_SET
          isPrimaryEntity = true
        }
    tick2.entities = listOf(robot2)

    val tick3 =
        TickData(currentTick = AuNaTimeUnit(3.0, 0.0), entities = listOf(), id = 3).apply {
          this.segment = segment
        }
    val robot3 =
        Robot(tick3).apply {
          dataSource = DataSource.CAM
          isPrimaryEntity = true
        }
    tick3.entities = listOf(robot3)

    segment.ticks =
        mapOf(tick1.currentTick to tick1, tick2.currentTick to tick2, tick3.currentTick to tick3)

    assertTrue {
      camMessageTimeout.holds(
          PredicateContext(segment), segment.tickData.first().getById(robot1.id))
    }
  }

  @Test
  fun `Two CAM messages with correct time distance`() {
    val segment =
        Segment(segmentId = 1, ticks = mutableMapOf(), nextSegment = null, previousSegment = null)

    val ticks =
        listOf(
            createTick(DataSource.CAM, 0.5, segment, 1),
            createTick(DataSource.NOT_SET, 1.0, segment, 2),
            createTick(DataSource.CAM, 1.5, segment, 3),
            createTick(DataSource.NOT_SET, 2.0, segment, 4),
        )

    segment.ticks = ticks.associateBy { it.currentTick }

    assertTrue {
      camMessageTimeout.holds(PredicateContext(segment), segment.tickData.first().getById(-1))
    }
  }

  @Test
  fun `NOT_SET, CAM, NOT_SET, CAM`() {
    val segment =
        Segment(segmentId = 1, ticks = mutableMapOf(), nextSegment = null, previousSegment = null)

    val ticks =
        listOf(
            createTick(DataSource.NOT_SET, 0.5, segment, 1),
            createTick(DataSource.CAM, 1.0, segment, 2),
            createTick(DataSource.NOT_SET, 1.5, segment, 3),
            createTick(DataSource.CAM, 2.0, segment, 4),
        )

    segment.ticks = ticks.associateBy { it.currentTick }

    assertTrue {
      camMessageTimeout.holds(PredicateContext(segment), segment.tickData.first().getById(-1))
    }
  }

  @Test
  fun `CAM, NOT_SET, NOT_SET, CAM`() {
    val segment =
        Segment(segmentId = 1, ticks = mutableMapOf(), nextSegment = null, previousSegment = null)

    val ticks =
        listOf(
            createTick(DataSource.CAM, 0.5, segment, 1),
            createTick(DataSource.NOT_SET, 1.0, segment, 2),
            createTick(DataSource.NOT_SET, 1.5, segment, 3),
            createTick(DataSource.CAM, 2.0, segment, 4),
        )

    segment.ticks = ticks.associateBy { it.currentTick }

    assertFalse() {
      camMessageTimeout.holds(PredicateContext(segment), segment.tickData.first().getById(-1))
    }
  }

  private fun createTick(
      dataSource: DataSource,
      timeStampSeconds: Double,
      segment: Segment,
      id: Int
  ): TickData {
    val tick =
        TickData(currentTick = AuNaTimeUnit(timeStampSeconds, 0.0), entities = listOf(), id = id)
            .apply { this.segment = segment }
    val robot =
        Robot(tick).apply {
          this.dataSource = dataSource
          isPrimaryEntity = true
        }
    tick.entities = listOf(robot)
    return tick
  }
}
