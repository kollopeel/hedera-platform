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

import com.swirlds.common.metrics.IntegerGauge;
import com.swirlds.common.metrics.Metric;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.swirlds.common.metrics.Metric.ValueType.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegerGaugeTest {

	private static final String CATEGORY = "CaTeGoRy";
	private static final String NAME = "NaMe";
	private static final String DESCRIPTION = "DeScRiPtIoN";
	private static final String FORMAT = "FoRmAt";

	@Test
	@DisplayName("Constructor should store values")
	void testConstructor() {
		final IntegerGauge gauge = new IntegerGauge(CATEGORY, NAME, DESCRIPTION, FORMAT);

		assertEquals(CATEGORY, gauge.getCategory(), "The category was not set correctly in the constructor");
		assertEquals(NAME, gauge.getName(), "The name was not set correctly in the constructor");
		assertEquals(DESCRIPTION, gauge.getDescription(), "The description was not set correctly in the constructor");
		assertEquals(FORMAT, gauge.getFormat(), "The format was not set correctly in the constructor");
		assertEquals(0, gauge.get(), "The value was not initialized correctly");
		assertEquals(0, gauge.get(VALUE), "The value was not initialized correctly");
		assertEquals(List.of(VALUE), gauge.getValueTypes(), "ValueTypes should be [VALUE]");
	}

	@Test
	@DisplayName("Constructor should throw IAE when passing null")
	void testConstructorWithNullParameter() {
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(null, NAME, DESCRIPTION, FORMAT),
				"Calling the constructor without a category should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, null, DESCRIPTION, FORMAT),
				"Calling the constructor without a name should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, NAME, null, FORMAT),
				"Calling the constructor without a description should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, NAME, DESCRIPTION, null),
				"Calling the constructor without a format should throw an IAE");
	}

	@Test
	@DisplayName("Constructor with initial value should store values")
	void testInitialValueConstructor() {
		final IntegerGauge gauge = new IntegerGauge(CATEGORY, NAME, DESCRIPTION, FORMAT, 42);

		assertEquals(CATEGORY, gauge.getCategory(), "The category was not set correctly in the constructor");
		assertEquals(NAME, gauge.getName(), "The name was not set correctly in the constructor");
		assertEquals(DESCRIPTION, gauge.getDescription(), "The description was not set correctly in the constructor");
		assertEquals(FORMAT, gauge.getFormat(), "The format was not set correctly in the constructor");
		assertEquals(42, gauge.get(), "The value was not initialized correctly");
		assertEquals(42, gauge.get(VALUE), "The value was not initialized correctly");
		assertEquals(List.of(VALUE), gauge.getValueTypes(), "ValueTypes should be [VALUE]");
	}

	@Test
	@DisplayName("Constructor with initial value should throw IAE when passing null")
	void testInitialValueConstructorWithNullParameter() {
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(null, NAME, DESCRIPTION, FORMAT, 42),
				"Calling the constructor without a category should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, null, DESCRIPTION, FORMAT, 42),
				"Calling the constructor without a name should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, NAME, null, FORMAT, 42),
				"Calling the constructor without a description should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, NAME, DESCRIPTION, null, 42),
				"Calling the constructor without a format should throw an IAE");
	}

	@Test
	@DisplayName("Constructor without format should store values")
	void testNoFormatConstructor() {
		final IntegerGauge gauge = new IntegerGauge(CATEGORY, NAME, DESCRIPTION);

		assertEquals(CATEGORY, gauge.getCategory(), "The category was not set correctly in the constructor");
		assertEquals(NAME, gauge.getName(), "The name was not set correctly in the constructor");
		assertEquals(DESCRIPTION, gauge.getDescription(), "The description was not set correctly in the constructor");
		assertEquals("%d", gauge.getFormat(), "The format was not set correctly in the constructor");
		assertEquals(0, gauge.get(), "The value was not initialized correctly");
		assertEquals(0, gauge.get(VALUE), "The value was not initialized correctly");
		assertEquals(List.of(VALUE), gauge.getValueTypes(), "ValueTypes should be [VALUE]");
	}

	@Test
	@DisplayName("Constructor without format should throw IAE when passing null")
	void testNoFormatConstructorWithNullParameter() {
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(null, NAME, DESCRIPTION),
				"Calling the constructor without a category should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, null, DESCRIPTION),
				"Calling the constructor without a name should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, NAME, null),
				"Calling the constructor without a description should throw an IAE");
	}

	@Test
	@DisplayName("Constructor without format, but with initial value should store values")
	void testNoFormatInitialValueConstructor() {
		final IntegerGauge gauge = new IntegerGauge(CATEGORY, NAME, DESCRIPTION, 42);

		assertEquals(CATEGORY, gauge.getCategory(), "The category was not set correctly in the constructor");
		assertEquals(NAME, gauge.getName(), "The name was not set correctly in the constructor");
		assertEquals(DESCRIPTION, gauge.getDescription(), "The description was not set correctly in the constructor");
		assertEquals("%d", gauge.getFormat(), "The format was not set correctly in the constructor");
		assertEquals(42, gauge.get(), "The value was not initialized correctly");
		assertEquals(42, gauge.get(VALUE), "The value was not initialized correctly");
		assertEquals(List.of(VALUE), gauge.getValueTypes(), "ValueTypes should be [VALUE]");
	}

	@Test
	@DisplayName("Constructor without format, but with initial value should throw IAE when passing null")
	void testNoFormatInitialValueConstructorWithNullParameter() {
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(null, NAME, DESCRIPTION, 42),
				"Calling the constructor without a category should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, null, DESCRIPTION, 42),
				"Calling the constructor without a name should throw an IAE");
		assertThrows(IllegalArgumentException.class, () -> new IntegerGauge(CATEGORY, NAME, null, 42),
				"Calling the constructor without a description should throw an IAE");
	}

	@Test
	@DisplayName("Test of get() and set()-operation")
	void testGetAndSet() {
		// given
		final IntegerGauge gauge = new IntegerGauge(CATEGORY, NAME, DESCRIPTION, FORMAT, 2);

		// when
		gauge.set(5);

		// then
		assertEquals(5, gauge.get(), "Value should be 5");
		assertEquals(5, gauge.get(VALUE), "Value should be 5");

		// when
		gauge.set(3);

		// then
		assertEquals(3, gauge.get(), "Value should be 3");
		assertEquals(3, gauge.get(VALUE), "Value should be 3");
	}

	@Test
	void testSnapshot() {
		// given
		final IntegerGauge gauge = new IntegerGauge(CATEGORY, NAME, DESCRIPTION, FORMAT, 2);

		// when
		final List<Pair<Metric.ValueType, Object>> snapshot = gauge.takeSnapshot();

		// then
		assertEquals(2, gauge.get(), "Value should be 2");
		assertEquals(2, gauge.get(VALUE), "Value should be 2");
		assertEquals(List.of(Pair.of(VALUE, 2)), snapshot, "Snapshot is not correct");
	}

	@Test
	void testInvalidGets() {
		// given
		final IntegerGauge gauge = new IntegerGauge(CATEGORY, NAME, DESCRIPTION, FORMAT, 2);

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
}