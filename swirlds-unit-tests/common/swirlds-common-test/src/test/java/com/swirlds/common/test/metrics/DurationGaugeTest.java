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

import com.swirlds.common.metrics.DurationGauge;
import com.swirlds.common.metrics.FloatFormats;
import com.swirlds.common.metrics.Metric;
import com.swirlds.common.metrics.platform.PlatformDurationGauge;
import com.swirlds.common.metrics.platform.Snapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

import static com.swirlds.common.metrics.Metric.ValueType.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DurationGaugeTest {

	private static final String CATEGORY = "CaTeGoRy";
	private static final String NAME = "NaMe";
	private static final String DESCRIPTION = "DeScRiPtIoN";
	private static final double EPSILON = 1e-6;
	private static final ChronoUnit SECONDS = ChronoUnit.SECONDS;


	@Test
	@DisplayName("Constructor should store values")
	void testConstructor() {
		DurationGauge gauge = new PlatformDurationGauge(new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, SECONDS));

		assertEquals(CATEGORY, gauge.getCategory(), "The category was not set correctly in the constructor");
		assertEquals(NAME + " (sec)", gauge.getName(), "The name was not set correctly in the constructor");
		assertEquals(DESCRIPTION, gauge.getDescription(), "The description was not set correctly in the constructor");
		assertEquals(FloatFormats.FORMAT_DECIMAL_3, gauge.getFormat(),
				"The format was not set correctly in the constructor for seconds");
		assertEquals(0.0, gauge.getNanos(), EPSILON, "The value was not initialized correctly");
		assertEquals(0.0, gauge.get(VALUE), EPSILON, "The value was not initialized correctly");
		assertEquals(EnumSet.of(VALUE), gauge.getValueTypes(), "ValueTypes should be [VALUE]");

		gauge = new PlatformDurationGauge(new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, ChronoUnit.MILLIS));
		assertEquals(FloatFormats.FORMAT_DECIMAL_3, gauge.getFormat(),
				"The format was not set correctly in the constructor for milliseconds");
		assertEquals(NAME + " (millis)", gauge.getName(), "The name was not set correctly in the constructor");

		gauge = new PlatformDurationGauge(new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, ChronoUnit.MICROS));
		assertEquals(FloatFormats.FORMAT_DECIMAL_0, gauge.getFormat(),
				"The format was not set correctly in the constructor for microsecond");
		assertEquals(NAME + " (micros)", gauge.getName(), "The name was not set correctly in the constructor");

		gauge = new PlatformDurationGauge(new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, ChronoUnit.NANOS));
		assertEquals(FloatFormats.FORMAT_DECIMAL_0, gauge.getFormat(),
				"The format was not set correctly in the constructor for nanoseconds");
		assertEquals(NAME + " (nanos)", gauge.getName(), "The name was not set correctly in the constructor");
	}

	@Test
	@DisplayName("Constructor should throw IAE when passing null")
	void testConstructorWithNullParameter() {
		assertThrows(IllegalArgumentException.class,
				() -> new DurationGauge.Config(null, NAME, DESCRIPTION, SECONDS),
				"Calling the constructor without a category should throw an IAE");
		assertThrows(IllegalArgumentException.class,
				() -> new DurationGauge.Config(CATEGORY, null, DESCRIPTION, SECONDS),
				"Calling the constructor without a name should throw an IAE");
		assertThrows(IllegalArgumentException.class,
				() -> new DurationGauge.Config(CATEGORY, NAME, null, SECONDS),
				"Calling the constructor without a description should throw an IAE");
		assertThrows(IllegalArgumentException.class,
				() -> new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, null),
				"Calling the constructor without a time unit should throw an IAE");
	}

	@Test
	void testUpdate() {
		// given
		final DurationGauge gauge = new PlatformDurationGauge(
				new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, SECONDS));

		testDurationUpdate(gauge, Duration.ofMillis(1500), SECONDS);
		testDurationUpdate(gauge, Duration.ofMillis(700), SECONDS);

		// test null
		gauge.update(null);
		assertValue(gauge, Duration.ofMillis(700), SECONDS);
	}

	private void testDurationUpdate(final DurationGauge gauge, final Duration value, final ChronoUnit unit) {
		// when
		gauge.update(value);

		// then
		assertValue(gauge, value, unit);
	}

	private double assertValue(final DurationGauge gauge, final Duration value, final ChronoUnit unit) {
		assertEquals(value.toNanos(), gauge.getNanos(), EPSILON, "Value should be " + value.toNanos());
		final double decimalValue = (double) value.toNanos() / unit.getDuration().toNanos();
		assertEquals(decimalValue, gauge.get(VALUE), EPSILON, "Value should be " + value);
		return decimalValue;
	}

	@Test
	void testSnapshot() {
		// given
		final PlatformDurationGauge gauge = new PlatformDurationGauge(
				new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, SECONDS));
		testDurationUpdate(gauge, Duration.ofMillis(3500), SECONDS);

		// when
		final List<Snapshot.SnapshotValue> snapshot = gauge.takeSnapshot();

		// then
		final double expectedValue = assertValue(gauge, Duration.ofMillis(3500), SECONDS);
		assertEquals(List.of(new Snapshot.SnapshotValue(VALUE, expectedValue)), snapshot, "Snapshot is not correct");
	}

	@Test
	void testInvalidGets() {
		// given
		final DurationGauge gauge = new PlatformDurationGauge(
				new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, SECONDS));

		// then
		assertThrows(IllegalArgumentException.class, () -> gauge.get(null),
				"Calling get() with null should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> gauge.get(Metric.ValueType.COUNTER),
				"Calling get() with an unsupported MetricType should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> gauge.get(Metric.ValueType.MIN),
				"Calling get() with an unsupported MetricType should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> gauge.get(Metric.ValueType.MAX),
				"Calling get() with an unsupported MetricType should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> gauge.get(Metric.ValueType.STD_DEV),
				"Calling get() with an unsupported MetricType should throw an IAE");
	}

	@Test
	void testUnit() {
		DurationGauge gauge = new PlatformDurationGauge(new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, SECONDS));
		testDurationUpdate(gauge, Duration.ofNanos(100), SECONDS);
		testDurationUpdate(gauge, Duration.ofMinutes(2), SECONDS);

		gauge = new PlatformDurationGauge(new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, ChronoUnit.MILLIS));
		testDurationUpdate(gauge, Duration.ofNanos(100), ChronoUnit.MILLIS);
		testDurationUpdate(gauge, Duration.ofMinutes(2), ChronoUnit.MILLIS);

		gauge = new PlatformDurationGauge(new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, ChronoUnit.MICROS));
		testDurationUpdate(gauge, Duration.ofNanos(100), ChronoUnit.MICROS);
		testDurationUpdate(gauge, Duration.ofMinutes(2), ChronoUnit.MICROS);

		gauge = new PlatformDurationGauge(new DurationGauge.Config(CATEGORY, NAME, DESCRIPTION, ChronoUnit.NANOS));
		testDurationUpdate(gauge, Duration.ofNanos(100), ChronoUnit.NANOS);
		testDurationUpdate(gauge, Duration.ofMinutes(2), ChronoUnit.NANOS);
	}
}
