/*
 * Copyright 2016-2022 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.swirlds.common.test.metrics;

import com.swirlds.common.metrics.Counter;
import com.swirlds.common.metrics.IntegerGauge;
import com.swirlds.common.metrics.Metric;
import com.swirlds.common.metrics.platform.PlatformCounter;
import com.swirlds.common.metrics.platform.PlatformIntegerGauge;
import com.swirlds.common.metrics.platform.Snapshot.SnapshotValue;
import com.swirlds.common.statistics.StatsBuffered;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.swirlds.common.metrics.Metric.ValueType.COUNTER;
import static com.swirlds.common.metrics.Metric.ValueType.VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlatformCounterTest {

    private static final String CATEGORY = "CaTeGoRy";
    private static final String NAME = "NaMe";
    private static final String DESCRIPTION = "DeScRiPtIoN";
	private static final String UNIT = "UnIt";

    @Test
    @DisplayName("Constructor should store values")
    void testConstructor() {
        final Counter.Config config = new Counter.Config(CATEGORY, NAME).withDescription(DESCRIPTION).withUnit(UNIT);
        final Counter counter = new PlatformCounter(config);

        assertEquals(CATEGORY, counter.getCategory(), "The category was not set correctly in the constructor");
        assertEquals(NAME, counter.getName(), "The name was not set correctly in the constructor");
        assertEquals(DESCRIPTION, counter.getDescription(), "The description was not set correctly in the constructor");
		assertEquals(UNIT, counter.getUnit(), "The unit was not set correctly in the constructor");
        assertEquals("%d", counter.getFormat(), "The format was not set correctly in constructor");
        assertEquals(0L, counter.get(), "The value was not initialized correctly");
        assertEquals(0L, counter.get(COUNTER), "The value was not initialized correctly");
        assertEquals(0L, counter.get(VALUE), "The value was not initialized correctly");
        assertThat(counter.getValueTypes()).containsExactly(COUNTER);
    }

    @Test
    @DisplayName("Counter should add non-negative values")
    void testAddingValues() {
        // given
        final Counter.Config config = new Counter.Config(CATEGORY, NAME);
        final Counter counter = new PlatformCounter(config);

        // when
        counter.add(3L);

        // then
        assertEquals(3L, counter.get(), "Value should be 3");
        assertEquals(3L, counter.get(COUNTER), "Value should be 3");
        assertEquals(3L, counter.get(VALUE), "Value should be 3");

        // when
        counter.add(5L);

        // then
        assertEquals(8L, counter.get(), "Value should be 8");
        assertEquals(8L, counter.get(COUNTER), "Value should be 8");
        assertEquals(8L, counter.get(VALUE), "Value should be 8");
    }

    @Test
    @DisplayName("Counter should not allow to add negative value")
    void testAddingNegativeValueShouldFail() {
        final Counter.Config config = new Counter.Config(CATEGORY, NAME);
        final Counter counter = new PlatformCounter(config);
        assertThrows(IllegalArgumentException.class, () -> counter.add(-1L),
                "Calling add() with negative value should throw IAE");
        assertThrows(IllegalArgumentException.class, () -> counter.add(0),
                "Calling add() with negative value should throw IAE");
    }

    @Test
    @DisplayName("Counter should increment by 1")
    void testIncrement() {
        // given
        final Counter.Config config = new Counter.Config(CATEGORY, NAME);
        final Counter counter = new PlatformCounter(config);

        // when
        counter.increment();

        // then
        assertEquals(1L, counter.get(), "Value should be 1");
        assertEquals(1L, counter.get(COUNTER), "Value should be 1");
        assertEquals(1L, counter.get(VALUE), "Value should be 1");

        // when
        counter.increment();

        // then
        assertEquals(2L, counter.get(), "Value should be 2");
        assertEquals(2L, counter.get(COUNTER), "Value should be 2");
        assertEquals(2L, counter.get(VALUE), "Value should be 2");
    }

    @Test
    void testSnapshot() {
        // given
        final Counter.Config config = new Counter.Config(CATEGORY, NAME);
        final PlatformCounter counter = new PlatformCounter(config);
        counter.add(2L);

        // when
        final List<SnapshotValue> snapshot = counter.takeSnapshot();

        // then
        assertEquals(2L, counter.get(), "Value should be 2");
        assertEquals(2L, counter.get(COUNTER), "Value should be 2");
        assertEquals(2L, counter.get(VALUE), "Value should be 2");
        assertThat(snapshot).containsExactly(new SnapshotValue(COUNTER, 2L));
    }

    @Test
    void testInvalidGets() {
        // given
        final Counter.Config config = new Counter.Config(CATEGORY, NAME);
        final Counter counter = new PlatformCounter(config);

        // then
        assertThrows(IllegalArgumentException.class, () -> counter.get(null),
                "Calling get() with null should throw an IAE");
        assertThrows(IllegalArgumentException.class, () -> counter.get(Metric.ValueType.MIN),
                "Calling get() with an unsupported MetricType should throw an IAE");
        assertThrows(IllegalArgumentException.class, () -> counter.get(Metric.ValueType.MAX),
                "Calling get() with an unsupported MetricType should throw an IAE");
        assertThrows(IllegalArgumentException.class, () -> counter.get(Metric.ValueType.STD_DEV),
                "Calling get() with an unsupported MetricType should throw an IAE");
    }

    @Test
    void testReset() {
        // given
        final Counter.Config config = new Counter.Config(CATEGORY, NAME);
        final Counter counter = new PlatformCounter(config);

        // then
        assertThatCode(counter::reset).doesNotThrowAnyException();
    }

    @Test
    void testGetStatBuffered() {
        // given
        final Counter.Config config = new Counter.Config(CATEGORY, NAME);
        final Counter counter = new PlatformCounter(config);

        // when
        final StatsBuffered actual = counter.getStatsBuffered();

        // then
        assertThat(actual).isNull();
    }

    @Test
    void testEquals() {
        // given
        final Counter.Config config = new Counter.Config(CATEGORY, NAME);
        final Counter counter1 = new PlatformCounter(config);
        final Counter counter2 = new PlatformCounter(config);
        counter2.add(42L);

        // then
        assertThat(counter1)
                .isEqualTo(counter2)
                .hasSameHashCodeAs(counter2)
                .isNotEqualTo(new PlatformCounter(new Counter.Config("Other", NAME)))
                .isNotEqualTo(new PlatformCounter(new Counter.Config(CATEGORY, "Other")))
                .isNotEqualTo(new PlatformIntegerGauge(new IntegerGauge.Config(CATEGORY, NAME)));
    }

    @Test
    void testToString() {
        // given
        final Counter.Config config = new Counter.Config(CATEGORY, NAME).withDescription(DESCRIPTION).withUnit(UNIT);
        final Counter counter = new PlatformCounter(config);
        counter.add(42L);

        // then
        assertThat(counter.toString()).contains(CATEGORY, NAME, DESCRIPTION, UNIT, "42");
    }
}
