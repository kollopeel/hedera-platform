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

package com.swirlds.common.utility;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the {@link com.swirlds.common.constructable.RuntimeConstructable} objects currently in memory.
 * One registry per process.
 */
public final class RuntimeObjectRegistry {

	/**
	 * Contains records of runtime objects, per class, that are currently in memory or that were recently in memory.
	 */
	private static final Map<Class<?>, List<RuntimeObjectRecord>> RECORDS =	new ConcurrentHashMap<>();

	private RuntimeObjectRegistry() {

	}

    /**
     * Create a new record for a runtime object with the specified class and add it to the records list.
     * When the record is released, it gets deleted from this registry.
     *
     * @param cls
     *      the object class
     *
     * @return a new {@link RuntimeObjectRecord}. Should be saved by the runtime object and released
     * 	    when the object is released.
     */
	public static <T> RuntimeObjectRecord createRecord(final Class<T> cls) {
		final Instant now = Instant.now();
		final List<RuntimeObjectRecord> classRecords = RECORDS.computeIfAbsent(cls,
				clsid -> Collections.synchronizedList(new ArrayList<>()));
		final RuntimeObjectRecord objectRecord = new RuntimeObjectRecord(now, classRecords::remove);
		classRecords.add(objectRecord);

		return objectRecord;
	}

	/**
	 * Get the current number of runtime objects of the specified class tracked in this registry.
     *
	 * @param cls
	 *      the objwct class
	 */
	public static <T> int getActiveObjectsCount(final Class<T> cls) {
		final List<RuntimeObjectRecord> classRecords = RECORDS.get(cls);
		return classRecords != null ? classRecords.size() : 0;
	}

	/**
	 * Get the age of the oldest runtime object of the specified class tracked in this registry.
	 *
	 * @param cls
	 *      the object class
	 * @param now
	 * 	    the current time
	 */
	public static <T> Duration getOldestActiveObjectAge(final Class<T> cls, final Instant now) {
		final List<RuntimeObjectRecord> classRecords = RECORDS.get(cls);
		if (classRecords == null) {
			return Duration.ZERO;
		}
		try {
			// It doesn't make sense to check if the list is empty, as it may become empty at any moment, as
			// the method isn't synchronized. Instead, just catch IOOBE
			final RuntimeObjectRecord oldestRecord = classRecords.get(0);
			return Duration.between(oldestRecord.getCreationTime(), now);
		} catch (final IndexOutOfBoundsException e) {
			return Duration.ZERO;
		}
	}

	/**
	 * Get object classes tracked in this registry. The set of classes is immutable.
	 *
	 * @return
	 * 		the list of tracked classes
	 */
	public static Set<Class<?>> getTrackedClasses() {
		return Collections.unmodifiableSet(RECORDS.keySet());
	}

	/**
	 * Drop all records of active runtime constructable objects. Allows this class to be unit tested
	 * without interference from prior unit tests.
	 */
	public static void reset() {
		RECORDS.clear();
	}

}
