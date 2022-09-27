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

package com.swirlds.common.test.sequence;

import com.swirlds.common.sequence.map.ConcurrentSequenceMap;
import com.swirlds.common.sequence.map.SequenceMap;
import com.swirlds.common.sequence.set.ConcurrentSequenceSet;
import com.swirlds.common.sequence.set.SequenceSet;
import com.swirlds.common.sequence.set.StandardSequenceSet;
import com.swirlds.common.threading.framework.Stoppable;
import com.swirlds.common.threading.framework.StoppableThread;
import com.swirlds.common.threading.framework.config.StoppableThreadConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.swirlds.common.test.AssertionUtils.completeBeforeTimeout;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("SequenceSet Tests")
public class SequenceSetTests {

	private record SequenceSetElement(int key, long sequence) {
		@Override
		public int hashCode() {
			return key;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof final SequenceSetElement that) {
				return this.key == that.key;
			}
			return false;
		}

		@Override
		public String toString() {
			return key + "[" + sequence + "]";
		}
	}

	private record SetBuilder(String name, BiFunction<Long, Long, SequenceSet<SequenceSetElement>> constructor) {
		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<Arguments> testConfiguration() {
		return Stream.of(
				Arguments.of(new SetBuilder("standard",
						(min, max) -> new StandardSequenceSet<>(min, max, SequenceSetElement::sequence))),
				Arguments.of(new SetBuilder("concurrent",
						(min, max) -> new ConcurrentSequenceSet<>(min, max, SequenceSetElement::sequence)))
		);
	}

	private static boolean isKeyPresent(final SequenceSet<SequenceSetElement> set, final Long sequenceNumber) {
		return sequenceNumber != null
				&& sequenceNumber >= set.getLowestAllowedSequenceNumber()
				&& sequenceNumber <= set.getHighestAllowedSequenceNumber();
	}

	/**
	 * Do validation on a set.
	 *
	 * @param set
	 * 		the set being validated
	 * @param smallestKeyToCheck
	 * 		the smallest key to check
	 * @param keyToCheckUpperBound
	 * 		the upper bound (exclusive) of keys to check
	 * @param getSequenceNumber
	 * 		provides the expected sequence number for a key, or null if the key is not expected to be in the set
	 */
	private void validateSetContents(
			final SequenceSet<SequenceSetElement> set,
			final int smallestKeyToCheck,
			final int keyToCheckUpperBound,
			final Function<Integer, Long> getSequenceNumber) {

		final Map<Long, Set<Integer>> keysBySequenceNumber = new HashMap<>();
		long smallestSequenceNumber = Long.MAX_VALUE;
		long largestSequenceNumber = Long.MIN_VALUE;
		int size = 0;

		// Query by key
		for (int key = smallestKeyToCheck; key < keyToCheckUpperBound; key++) {
			final Long sequenceNumber = getSequenceNumber.apply(key);

			if (isKeyPresent(set, sequenceNumber)) {

				assertTrue(set.contains(new SequenceSetElement(key, getSequenceNumber.apply(key))),
						"should contain key");

				keysBySequenceNumber.computeIfAbsent(sequenceNumber, k -> new HashSet<>()).add(key);
				smallestSequenceNumber = Math.min(smallestSequenceNumber, sequenceNumber);
				largestSequenceNumber = Math.max(largestSequenceNumber, sequenceNumber);

				size++;

			} else {
				// Note: the sequence number in the key is unused when we are just querying. So it's
				// ok to lie and provide a sequence number of 0 here, even though 0 may not be the
				// correct sequence number for the given key.
				assertFalse(set.contains(new SequenceSetElement(key, 0)),
						"should not contain key");
			}
		}

		assertEquals(size, set.getSize(), "unexpected set size");
		if (size == 0) {
			// For the sake of sanity, we don't want to attempt to use the default values for these
			// variables under any conditions.
			smallestSequenceNumber = set.getLowestAllowedSequenceNumber();
			largestSequenceNumber = set.getHighestAllowedSequenceNumber();
		}


		// Query by sequence number
		// Start at 100 sequence numbers below the minimum, and query to 100 sequence numbers beyond the maximum
		for (long sequenceNumber = smallestSequenceNumber - 100;
			 sequenceNumber < largestSequenceNumber + 100;
			 sequenceNumber++) {

			final Set<Integer> expectedKeys = keysBySequenceNumber.get(sequenceNumber);
			if (expectedKeys == null) {
				assertTrue(set.getEntriesWithSequenceNumber(sequenceNumber).isEmpty(),
						"set should not contain any keys");
			} else {
				final List<SequenceSetElement> keys = set.getEntriesWithSequenceNumber(sequenceNumber);
				assertEquals(expectedKeys.size(), keys.size(),
						"unexpected number of keys returned");
				for (final SequenceSetElement element : keys) {
					assertTrue(expectedKeys.contains(element.key), "element not in expected set");
					assertEquals(getSequenceNumber.apply(element.key), element.sequence,
							"unexpected sequence number");
				}
			}
		}
	}

	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("Simple Access Test")
	void simpleAccessTest(final SetBuilder setBuilder) {
		final SequenceSet<SequenceSetElement> set = setBuilder.constructor.apply(0L, Long.MAX_VALUE);

		// The number of things inserted into the set
		final int size = 100;

		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		for (int i = 0; i < size; i++) {
			set.add(new SequenceSetElement(i, i / 5));
			assertEquals(i + 1, set.getSize(), "unexpected size");
		}

		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						return (long) key / keysPerSeq;
					} else {
						// key is not present
						return null;
					}
				});
	}

	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("purge() Test")
	void purgeTest(final SetBuilder setBuilder) {
		final SequenceSet<SequenceSetElement> set = setBuilder.constructor.apply(0L, Long.MAX_VALUE);
		assertEquals(0, set.getLowestAllowedSequenceNumber(), "unexpected lower bound");

		// The number of things inserted into the set
		final int size = 100;

		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		for (int i = 0; i < size; i++) {
			set.add(new SequenceSetElement(i, i / keysPerSeq));
			assertEquals(i + 1, set.getSize(), "unexpected size");
		}

		set.purge(size / 2 / keysPerSeq);
		assertEquals(size / 2 / keysPerSeq, set.getLowestAllowedSequenceNumber(),
				"unexpected lower bound");
		assertEquals(size / 2, set.getSize(), "unexpected size");

		validateSetContents(set, 0, 2 * size,
				key -> {
					if (key < size) {
						return (long) key / keysPerSeq;
					} else {
						// key is not present
						return null;
					}
				});
	}

	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("purge() With Callback Test")
	void purgeWithCallbackTest(final SetBuilder setBuilder) {
		final SequenceSet<SequenceSetElement> set = setBuilder.constructor.apply(0L, Long.MAX_VALUE);
		assertEquals(0, set.getLowestAllowedSequenceNumber(), "unexpected lower bound");

		// The number of things inserted into the set
		final int size = 100;

		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		for (int i = 0; i < size; i++) {
			set.add(new SequenceSetElement(i, i / keysPerSeq));
			assertEquals(i + 1, set.getSize(), "unexpected size");
		}

		final Set<Integer> purgedKeys = new HashSet<>();
		set.purge(size / 2 / keysPerSeq, key -> {
			assertTrue(key.sequence < size / 2 / keysPerSeq, "key should not be purged");
			assertEquals(key.key / keysPerSeq, key.sequence, "unexpected sequence number for key");
			assertTrue(purgedKeys.add(key.key), "callback should be invoked once per key");
		});

		assertEquals(size / 2, purgedKeys.size(), "unexpected number of keys purged");

		assertEquals(size / 2 / keysPerSeq, set.getLowestAllowedSequenceNumber(),
				"unexpected lower bound");
		assertEquals(size / 2, set.getSize(), "unexpected size");

		validateSetContents(set, 0, 2 * size,
				key -> {
					if (key < size) {
						return (long) key / keysPerSeq;
					} else {
						// key is not present
						return null;
					}
				});
	}

	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("Upper/Lower Bound Test")
	void upperLowerBoundTest(final SetBuilder setBuilder) {
		// The number of things inserted into the set
		final int size = 100;

		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		final SequenceSet<SequenceSetElement> set =
				setBuilder.constructor.apply(5L, 10L);

		assertEquals(5, set.getLowestAllowedSequenceNumber(), "unexpected lower bound");
		assertEquals(10, set.getHighestAllowedSequenceNumber(), "unexpected upper bound");

		for (int i = 0; i < size; i++) {
			set.add(new SequenceSetElement(i, i / keysPerSeq));
		}

		validateSetContents(set, 0, 2 * size,
				key -> (long) key / keysPerSeq);
	}

	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("Shifting Window Test")
	void shiftingWindowTest(final SetBuilder setBuilder) {
		final int size = 100;

		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		// The lowest permitted sequence number
		int lowerBound = 0;

		// The highest permitted sequence number
		int upperBound = size / keysPerSeq;

		final SequenceSet<SequenceSetElement> set =
				setBuilder.constructor.apply((long) lowerBound, (long) upperBound);

		for (int iteration = 0; iteration < 10; iteration++) {
			if (iteration % 2 == 0) {
				// shift the lower bound
				lowerBound += size / 2 / keysPerSeq;
				set.purge(lowerBound);
			} else {
				// shift the upper bound
				upperBound += size / 2 / keysPerSeq;
				set.expand(upperBound);
			}

			assertEquals(lowerBound, set.getLowestAllowedSequenceNumber(), "unexpected lower bound");
			assertEquals(upperBound, set.getHighestAllowedSequenceNumber(), "unexpected upper bound");

			// Add a bunch of values. Values outside the window should be ignored.
			for (int i = lowerBound * keysPerSeq - 100; i < upperBound * keysPerSeq + 100; i++) {
				set.add(new SequenceSetElement(i, i / keysPerSeq));
			}

			validateSetContents(set, 0, upperBound * keysPerSeq + size,
					key -> {
						if (key >= 0) {
							return (long) key / keysPerSeq;
						} else {
							// key is not present
							return null;
						}
					});
		}
	}

	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("clear() Test")
	void clearTest(final SetBuilder setBuilder) {

		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		// The lowest permitted sequence number
		final int lowerBound = 50;

		// The highest permitted sequence number
		final int upperBound = 100;

		final SequenceSet<SequenceSetElement> set =
				setBuilder.constructor.apply((long) lowerBound, (long) upperBound);

		assertEquals(lowerBound, set.getLowestAllowedSequenceNumber(), "unexpected lower bound");
		assertEquals(upperBound, set.getHighestAllowedSequenceNumber(), "unexpected upper bound");

		for (int i = 0; i < upperBound * keysPerSeq + 100; i++) {
			set.add(new SequenceSetElement(i, i / 5));
		}

		validateSetContents(set, 0, upperBound * keysPerSeq + 100,
				key -> (long) key / keysPerSeq);

		// Shift the window.
		final int newLowerBound = lowerBound + 10;
		final int newUpperBound = upperBound + 10;
		set.purge(newLowerBound);
		set.expand(newUpperBound);

		assertEquals(newLowerBound, set.getLowestAllowedSequenceNumber(), "unexpected lower bound");
		assertEquals(newUpperBound, set.getHighestAllowedSequenceNumber(), "unexpected upper bound");

		for (int i = 0; i < newUpperBound * keysPerSeq + 100; i++) {
			set.add(new SequenceSetElement(i, i / 5));
		}

		validateSetContents(set, 0, newUpperBound * keysPerSeq + 100,
				key -> (long) key / keysPerSeq);

		set.clear();

		// should revert to original bounds
		assertEquals(lowerBound, set.getLowestAllowedSequenceNumber(), "unexpected lower bound");
		assertEquals(upperBound, set.getHighestAllowedSequenceNumber(), "unexpected upper bound");

		assertEquals(0, set.getSize(), "set should be empty");

		validateSetContents(set, 0, upperBound * keysPerSeq + 100, key -> null);

		// Reinserting values should work the same way as when the set was "fresh"
		for (int i = 0; i < upperBound * keysPerSeq + 100; i++) {
			set.add(new SequenceSetElement(i, i / 5));
		}

		validateSetContents(set, 0, upperBound * keysPerSeq + 100,
				key -> (long) key / keysPerSeq);
	}


	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("remove() Test")
	void removeTest(final SetBuilder setBuilder) {
		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		// The lowest permitted sequence number
		final int lowerBound = 0;

		// The highest permitted sequence number
		final int upperBound = 100;

		final SequenceSet<SequenceSetElement> set =
				setBuilder.constructor.apply((long) lowerBound, (long) upperBound);

		// removing values from an empty set shouldn't cause problems
		assertFalse(set.remove(new SequenceSetElement(-100, 0)), "value should not be in set");
		assertFalse(set.remove(new SequenceSetElement(0, 0)), "value should not be in set");
		assertFalse(set.remove(new SequenceSetElement(50, 0)), "value should not be in set");
		assertFalse(set.remove(new SequenceSetElement(100, 0)), "value should not be in set");

		// Validate removal of an existing value
		assertEquals(0, set.getSize(), "set should be empty");
		validateSetContents(set, 0, upperBound * keysPerSeq + 100,
				key -> null);

		set.add(new SequenceSetElement(10, 2));
		assertTrue(set.remove(new SequenceSetElement(10, 2)), "value should have been removed");

		assertEquals(0, set.getSize(), "set should be empty");
		validateSetContents(set, 0, upperBound * keysPerSeq + 100,
				key -> null);

		// Re-inserting after removal should work the same as regular insertion
		set.add(new SequenceSetElement(10, 2));
		assertTrue(set.contains(new SequenceSetElement(10, 2)), "should contain value");

		assertEquals(1, set.getSize(), "set should contain one thing");
		validateSetContents(set, 0, upperBound * keysPerSeq + 100,
				key -> {
					if (key == 10) {
						return 2L;
					} else {
						return null;
					}
				});
	}

	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("Replacing add() Test")
	void replacingPutTest(final SetBuilder setBuilder) {
		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		// The lowest permitted sequence number
		final int lowerBound = 0;

		// The highest permitted sequence number
		final int upperBound = 100;

		final SequenceSet<SequenceSetElement> set =
				setBuilder.constructor.apply((long) lowerBound, (long) upperBound);

		assertTrue(set.add(new SequenceSetElement(10, 2)), "no value should currently be in set");

		assertEquals(1, set.getSize(), "set should contain one thing");
		validateSetContents(set, 0, upperBound * keysPerSeq + 100,
				key -> {
					if (key == 10) {
						return 2L;
					} else {
						return null;
					}
				});

		assertFalse(set.add(new SequenceSetElement(10, 2)),
				"previous value should be returned");

		assertEquals(1, set.getSize(), "set should contain one thing");
		validateSetContents(set, 0, upperBound * keysPerSeq + 100,
				key -> {
					if (key == 10) {
						return 2L;
					} else {
						return null;
					}
				});
	}

	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("removeSequenceNumber() Test")
	void removeSequenceNumberTest(final SetBuilder setBuilder) {
		final SequenceSet<SequenceSetElement> set = setBuilder.constructor.apply(0L, Long.MAX_VALUE);

		// The number of things inserted into the set
		final int size = 100;

		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		for (int i = 0; i < size; i++) {
			set.add(new SequenceSetElement(i, i / 5));
			assertEquals(i + 1, set.getSize(), "unexpected size");
		}

		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						return (long) key / keysPerSeq;
					} else {
						// key is not present
						return null;
					}
				});

		// Remove sequence numbers that are not in the set
		set.removeSequenceNumber(-1000);
		set.removeSequenceNumber(1000);
		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						return (long) key / keysPerSeq;
					} else {
						// key is not present
						return null;
					}
				});

		// Remove a sequence number that is in the set
		set.removeSequenceNumber(1);

		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						final long sequenceNumber = key / keysPerSeq;
						if (sequenceNumber == 1) {
							return null;
						}
						return sequenceNumber;
					} else {
						// key is not present
						return null;
					}
				});

		// Removing the same sequence number a second time shouldn't have any ill effects
		set.removeSequenceNumber(1);

		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						final long sequenceNumber = key / keysPerSeq;
						if (sequenceNumber == 1) {
							return null;
						}
						return sequenceNumber;
					} else {
						// key is not present
						return null;
					}
				});

		// It should be ok to re-insert into the removed sequence number
		set.add(new SequenceSetElement(5, 1));
		set.add(new SequenceSetElement(6, 1));
		set.add(new SequenceSetElement(7, 1));
		set.add(new SequenceSetElement(8, 1));
		set.add(new SequenceSetElement(9, 1));

		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						return (long) key / keysPerSeq;
					} else {
						// key is not present
						return null;
					}
				});
	}

	@ParameterizedTest
	@MethodSource("testConfiguration")
	@DisplayName("removeSequenceNumber() With Callback Test")
	void removeSequenceNumberWithCallbackTest(final SetBuilder setBuilder) {
		final SequenceSet<SequenceSetElement> set = setBuilder.constructor.apply(0L, Long.MAX_VALUE);

		// The number of things inserted into the set
		final int size = 100;

		// The number of keys for each sequence number
		final int keysPerSeq = 5;

		for (int i = 0; i < size; i++) {
			set.add(new SequenceSetElement(i, i / 5));
			assertEquals(i + 1, set.getSize(), "unexpected size");
		}

		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						return (long) key / keysPerSeq;
					} else {
						// key is not present
						return null;
					}
				});

		// Remove sequence numbers that are not in the set
		set.removeSequenceNumber(-1000, key -> fail("should not be called"));
		set.removeSequenceNumber(1000, key -> fail("should not be called"));
		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						return (long) key / keysPerSeq;
					} else {
						// key is not present
						return null;
					}
				});

		// Remove a sequence number that is in the set
		final Set<Integer> removedKeys = new HashSet<>();
		set.removeSequenceNumber(1, key -> {
			assertEquals(1, key.sequence, "key should not be removed");
			assertEquals(key.key / keysPerSeq, key.sequence, "unexpected sequence number for key");
			assertTrue(removedKeys.add(key.key), "callback should be invoked once per key");
		});
		assertEquals(5, removedKeys.size(), "unexpected number of keys removed");

		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						final long sequenceNumber = key / keysPerSeq;
						if (sequenceNumber == 1) {
							return null;
						}
						return sequenceNumber;
					} else {
						// key is not present
						return null;
					}
				});

		// Removing the same sequence number a second time shouldn't have any ill effects
		set.removeSequenceNumber(1, key -> fail("should not be called"));

		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						final long sequenceNumber = key / keysPerSeq;
						if (sequenceNumber == 1) {
							return null;
						}
						return sequenceNumber;
					} else {
						// key is not present
						return null;
					}
				});

		// It should be ok to re-insert into the removed sequence number
		set.add(new SequenceSetElement(5, 1));
		set.add(new SequenceSetElement(6, 1));
		set.add(new SequenceSetElement(7, 1));
		set.add(new SequenceSetElement(8, 1));
		set.add(new SequenceSetElement(9, 1));

		validateSetContents(set, -size, 2 * size,
				key -> {
					if (key >= 0 && key < size) {
						return (long) key / keysPerSeq;
					} else {
						// key is not present
						return null;
					}
				});
	}

	@Test
	@DisplayName("Parallel SequenceSet Test")
	void parallelSequenceSetTest() throws InterruptedException {
		final Random random = new Random();

		final AtomicInteger lowerBound = new AtomicInteger(0);
		final AtomicInteger upperBound = new AtomicInteger(100);

		final SequenceSet<SequenceSetElement> set = new ConcurrentSequenceSet<>(
				lowerBound.get(),
				upperBound.get(),
				SequenceSetElement::sequence);

		final AtomicBoolean error = new AtomicBoolean();

		final StoppableThread purgeThread = new StoppableThreadConfiguration<>()
				.setMinimumPeriod(Duration.ofMillis(10))
				.setExceptionHandler((t, e) -> {
					e.printStackTrace();
					error.set(true);
				})
				.setWork(() -> {

					// Verify that no data is present that should not be present
					for (int sequenceNumber = lowerBound.get() - 100;
						 sequenceNumber < upperBound.get() + 100;
						 sequenceNumber++) {

						if (sequenceNumber < lowerBound.get() || sequenceNumber > upperBound.get()) {
							final List<SequenceSetElement> keys = set.getEntriesWithSequenceNumber(sequenceNumber);
							assertEquals(0, keys.size(), "no keys should be present for this round");
						}
					}

					// shift the window
					lowerBound.getAndAdd(5);
					upperBound.getAndAdd(5);
					set.purge(lowerBound.get());
					set.expand(upperBound.get());
				})
				.build(true);

		final int threadCount = 4;
		final List<StoppableThread> updaterThreads = new LinkedList<>();
		for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
			updaterThreads.add(new StoppableThreadConfiguration<>()
					.setExceptionHandler((t, e) -> {
						e.printStackTrace();
						error.set(true);
					})
					.setWork(() -> {

						final double choice = random.nextDouble();
						final int sequenceNumber =
								random.nextInt(lowerBound.get() - 50, upperBound.get() + 50);

						if (choice < 0.5) {
							// attempt to delete a value
							final int key = random.nextInt(sequenceNumber * 10, sequenceNumber * 10 + 10);
							set.remove(new SequenceSetElement(key, sequenceNumber));

						} else if (choice < 0.999) {
							// insert/replace a value
							final int key = random.nextInt(sequenceNumber * 10, sequenceNumber * 10 + 10);
							set.add(new SequenceSetElement(key, sequenceNumber));

						} else {
							// very rarely, delete an entire sequence number
							set.removeSequenceNumber(sequenceNumber);
						}
					})
					.build(true));
		}

		// Let the threads fight each other for a little while. At the end, tear everything down and make sure
		// our constraints weren't violated.
		SECONDS.sleep(2);
		purgeThread.stop();
		updaterThreads.forEach(Stoppable::stop);

		completeBeforeTimeout(() -> purgeThread.join(), Duration.ofSeconds(1),
				"thread did not die on time");
		updaterThreads.forEach(thread -> {
			try {
				completeBeforeTimeout(() -> thread.join(), Duration.ofSeconds(1),
						"thread did not die on time");
			} catch (InterruptedException e) {
				fail(e);
			}
		});

		assertFalse(error.get(), "error(s) encountered");
	}
}
