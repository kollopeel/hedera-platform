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

package com.swirlds.common.metrics;

import com.swirlds.common.statistics.StatsBuffered;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

import static com.swirlds.common.utility.CommonUtils.throwArgNull;

/**
 * A single metric that is monitored here.
 */
public abstract class Metric {

	/**
	 * A {@code Metric} can keep track of several values, which are distinguished via the {@code MetricType}
	 */
	public enum ValueType {COUNTER, VALUE, MAX, MIN, STD_DEV}

	protected static final List<ValueType> VALUE_TYPE = List.of(ValueType.VALUE);
	protected static final List<ValueType> DISTRIBUTION_TYPES = List.of(
			ValueType.VALUE,
			ValueType.MAX,
			ValueType.MIN,
			ValueType.STD_DEV
	);
	protected static final List<ValueType> COUNTER_TYPE = List.of(ValueType.COUNTER);

	private final String category;
	private final String name;
	private final String description;
	private final String format;

	/**
	 * Constructor of {@code Metric}
	 *
	 * @param category
	 * 		the kind of metric (metrics are grouped or filtered by this)
	 * @param name
	 * 		a short name for the metric
	 * @param description
	 * 		a one-sentence description of the metric
	 * @param format
	 * 		a string that can be passed to String.format() to format the metric
	 * @throws IllegalArgumentException if one of the parameters is {@code null}
	 */
	protected Metric(final String category,
			final String name,
			final String description,
			final String format) {
		this.category = throwArgNull(category, "category");
		this.name = throwArgNull(name, "name");
		this.description = throwArgNull(description, "description");
		this.format= throwArgNull(format, "format");
	}

	/**
	 * the kind of metric (metrics are grouped or filtered by this)
	 *
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * a short name for the statistic
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * a one-sentence description of the statistic
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * a string that can be passed to String.format() to format the statistic
	 *
	 * @return the format
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Returns a list of {@link ValueType}s that are provided by this {@code Metric}.
	 * The list of {@code ValueTypes} will always be in the same order.
	 *
	 * @return the list of {@code ValueTypes}
	 */
	public abstract List<ValueType> getValueTypes();

	/**
	 * Returns the current value of the given {@link ValueType}.
	 *
	 * @param valueType The {@code ValueType} of the requested value
	 * @return the current value
	 * @throws IllegalArgumentException if the given {@code ValueType} is not supported by this {@code Metric}
	 */
	public abstract Object get(final ValueType valueType);

	/**
	 * Take a snapshot of the current value(s) and return them. If the functionality of this {@code Metric}
	 * requires it to be reset in regular intervals, it is done automatically after the snapshot was generated.
	 * The list of {@code ValueTypes} will always be in the same order.
	 *
	 * @return the list of {@code ValueTypes} with their current values
	 * @deprecated Implementation detail, will become non-public at some point
	 */
	@Deprecated(forRemoval = true)
	public abstract List<Pair<ValueType, Object>> takeSnapshot();

	/**
	 * This method initializes the {@code Metric}
	 *
	 * @deprecated This method seems to be a byproduct of the current Statistics-initialization
	 * and is very likely to become obsolete. Instances of {@code Metric} should be initialized
	 * in the constructor instead.
	 */
	@Deprecated(forRemoval = true)
	public void init() {
		// default implementation is empty
	}

	/**
	 * This method resets a {@code Metric}. It is for example called after startup to ensure that the
	 * startup time is not taken into consideration;
	 */
	public void reset() {
		// default implementation is empty
	}

	/**
	 * This method returns the {@link StatsBuffered} of this metric, if there is one.
	 * <p>
	 * This method is only used to simplify the migration and will be removed afterwards
	 *
	 * @return the {@code StatsBuffered}, if there is one, {@code null} otherwise
	 * @deprecated This method is only temporary and will be removed during the Metric overhaul.
	 */
	@Deprecated(forRemoval = true)
	public StatsBuffered getStatsBuffered() {
		return null;
	}

}