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

import com.swirlds.common.metrics.DoubleAccumulator;
import com.swirlds.common.metrics.IntegerGauge;
import com.swirlds.common.metrics.Metric;
import com.swirlds.common.metrics.platform.PlatformDoubleAccumulator;
import com.swirlds.common.metrics.platform.PlatformIntegerGauge;
import com.swirlds.common.metrics.platform.Snapshot.SnapshotValue;
import com.swirlds.common.statistics.StatsBuffered;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.swirlds.common.metrics.Metric.ValueType.VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlatformDoubleAccumulatorTest {

	private static final String CATEGORY = "CaTeGoRy";
	private static final String NAME = "NaMe";
	private static final String DESCRIPTION = "DeScRiPtIoN";
	private static final String UNIT = "UnIt";
	private static final String FORMAT = "FoRmAt";
	private static final double EPSILON = 1e-6;

	@Test
	@DisplayName("Constructor should store values")
	void testConstructor() {
		// given
		final DoubleAccumulator.Config config = new DoubleAccumulator.Config(CATEGORY, NAME)
				.withDescription(DESCRIPTION).withUnit(UNIT).withFormat(FORMAT).withInitialValue(Math.PI);
		final DoubleAccumulator accumulator = new PlatformDoubleAccumulator(config);

		assertEquals(CATEGORY, accumulator.getCategory(), "The category was not set correctly in the constructor");
		assertEquals(NAME, accumulator.getName(), "The name was not set correctly in the constructor");
		assertEquals(DESCRIPTION, accumulator.getDescription(), "The description was not set correctly in the constructor");
		assertEquals(UNIT, accumulator.getUnit(), "The unit was not set correctly in the constructor");
		assertEquals(FORMAT, accumulator.getFormat(), "The format was not set correctly in the constructor");
		assertEquals(Math.PI, accumulator.getInitialValue(), EPSILON, "The initial value was not initialized correctly");
		assertEquals(Math.PI, accumulator.get(), EPSILON, "The value was not initialized correctly");
		assertEquals(Math.PI, accumulator.get(VALUE), EPSILON, "The value was not initialized correctly");
		assertThat(accumulator.getValueTypes()).containsExactly(VALUE);
	}

	@Test
	@DisplayName("Test of get() and update()-operation")
	void testGetAndUpdate() {
		// given
		final DoubleAccumulator.Config config = new DoubleAccumulator.Config(CATEGORY, NAME)
				.withAccumulator((op1, op2) -> op1 - op2).withInitialValue(2.0);
		final DoubleAccumulator accumulator = new PlatformDoubleAccumulator(config);

		// when
		accumulator.update(5.0);

		// then
		assertEquals(-3.0, accumulator.get(), EPSILON, "Value should be -3");
		assertEquals(-3.0, accumulator.get(VALUE), EPSILON, "Value should be -3");

		// when
		accumulator.update(3.0);

		// then
		assertEquals(-6.0, accumulator.get(), EPSILON, "Value should be -6");
		assertEquals(-6.0, accumulator.get(VALUE), EPSILON, "Value should be -6");
	}

	@Test
	void testSpecialValues() {
		// given
		final DoubleAccumulator.Config config = new DoubleAccumulator.Config(CATEGORY, NAME)
				.withAccumulator((op1, op2) -> op2);
		final DoubleAccumulator accumulator1 = new PlatformDoubleAccumulator(config);
		final DoubleAccumulator accumulator2 = new PlatformDoubleAccumulator(config);
		final DoubleAccumulator accumulator3 = new PlatformDoubleAccumulator(config);

		// when
		accumulator1.update(Double.NaN);
		accumulator2.update(Double.POSITIVE_INFINITY);
		accumulator3.update(Double.NEGATIVE_INFINITY);

		// then
		assertThat(accumulator1.get()).isNaN();
		assertThat(accumulator2.get()).isPositive().isInfinite();
		assertThat(accumulator3.get()).isNegative().isInfinite();
	}

	@Test
	void testSnapshot() {
		// given
		final DoubleAccumulator.Config config = new DoubleAccumulator.Config(CATEGORY, NAME)
				.withAccumulator(Double::max).withInitialValue(Math.E);
		final PlatformDoubleAccumulator accumulator = new PlatformDoubleAccumulator(config);
		accumulator.update(Math.PI);

		// when
		final List<SnapshotValue> snapshot = accumulator.takeSnapshot();

		// then
		assertEquals(Math.E, accumulator.get(), EPSILON, "Value should be " + Math.E);
		assertEquals(Math.E, accumulator.get(VALUE), EPSILON, "Value should be " + Math.E);
		assertEquals(VALUE, snapshot.get(0).valueType(), "Value-type of snapshot should be VALUE");
		assertEquals(Math.PI, (double) snapshot.get(0).value(), EPSILON, "Snapshot value is not correct");
	}

	@Test
	void testInvalidGets() {
		// given
		final DoubleAccumulator.Config config = new DoubleAccumulator.Config(CATEGORY, NAME);
		final DoubleAccumulator accumulator = new PlatformDoubleAccumulator(config);

		// then
		assertThrows(IllegalArgumentException.class, () -> accumulator.get(null),
				"Calling get() with null should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> accumulator.get(Metric.ValueType.COUNTER),
				"Calling get() with an unsupported MetricType should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> accumulator.get(Metric.ValueType.MIN),
				"Calling get() with an unsupported MetricType should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> accumulator.get(Metric.ValueType.MAX),
				"Calling get() with an unsupported MetricType should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> accumulator.get(Metric.ValueType.STD_DEV),
				"Calling get() with an unsupported MetricType should throw an IAE");
	}

	@Test
	void testReset() {
		// given
		final DoubleAccumulator.Config config = new DoubleAccumulator.Config(CATEGORY, NAME);
		final DoubleAccumulator accumulator = new PlatformDoubleAccumulator(config);

		// then
		assertThatCode(accumulator::reset).doesNotThrowAnyException();
	}

	@SuppressWarnings("removal")
	@Test
	void testGetStatBuffered() {
		// given
		final DoubleAccumulator.Config config = new DoubleAccumulator.Config(CATEGORY, NAME);
		final DoubleAccumulator accumulator = new PlatformDoubleAccumulator(config);

		// when
		final StatsBuffered actual = accumulator.getStatsBuffered();

		// then
		assertThat(actual).isNull();
	}

	@Test
	void testEquals() {
		// given
		final DoubleAccumulator.Config config = new DoubleAccumulator.Config(CATEGORY, NAME);
		final DoubleAccumulator accumulator1 = new PlatformDoubleAccumulator(config);
		final DoubleAccumulator accumulator2 = new PlatformDoubleAccumulator(config);
		accumulator2.update(Math.PI);

		// then
		assertThat(accumulator1)
				.isEqualTo(accumulator2)
				.hasSameHashCodeAs(accumulator2)
				.isNotEqualTo(new PlatformDoubleAccumulator(new DoubleAccumulator.Config("Other", NAME)))
				.isNotEqualTo(new PlatformDoubleAccumulator(new DoubleAccumulator.Config(CATEGORY, "Other")))
				.isNotEqualTo(new PlatformIntegerGauge(new IntegerGauge.Config(CATEGORY, NAME)));
	}

	@Test
	void testToString() {
		// given
		final DoubleAccumulator.Config config = new DoubleAccumulator.Config(CATEGORY, NAME)
				.withDescription(DESCRIPTION).withUnit(UNIT).withFormat(FORMAT).withInitialValue(Math.PI);
		final DoubleAccumulator accumulator = new PlatformDoubleAccumulator(config);

		// then
		assertThat(accumulator.toString()).contains(CATEGORY, NAME, DESCRIPTION, UNIT, FORMAT, "3.1415");
	}
}
