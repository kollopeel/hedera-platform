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

package com.swirlds.common.test.threading;

import com.swirlds.common.threading.pool.CachedPoolParallelExecutor;
import com.swirlds.common.threading.pool.ParallelExecutionException;
import com.swirlds.common.threading.pool.ParallelExecutor;
import com.swirlds.test.framework.TestComponentTags;
import com.swirlds.test.framework.TestTypeTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Parallel Executor Tests")
class CachedPoolParallelExecutorTest {

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.THREADING)
	@DisplayName("Simple 2 parallel task test")
	void simpleTasks() throws Exception {
		final ParallelExecutor executor = new CachedPoolParallelExecutor("a name");
		// create 2 latches where both threads need to do the countdown on one and wait for the other
		// these 2 operations need to happen in parallel
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final long expectedReturn = new Random().nextLong();
		final Callable<Long> task1 = () -> {
			latch1.countDown();
			latch2.await();
			return expectedReturn;
		};
		final Callable<Void> task2 = () -> {
			latch2.countDown();
			latch1.await();
			return null;
		};
		final Long actualReturn = executor.doParallel(task1, task2);

		assertEquals(0, latch1.getCount(), "thread 1 should have done a countdown");
		assertEquals(0, latch2.getCount(), "thread 2 should have done a countdown");
		assertEquals(expectedReturn, actualReturn, "doParallel did not return the correct value");
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.THREADING)
	@DisplayName("Exception test")
	void testException() {
		final ParallelExecutor executor = new CachedPoolParallelExecutor("a name");
		final Exception exception1 = new Exception("exception 1");
		final Exception exception2 = new Exception("exception 2");
		final AssertionError error1 = new AssertionError("error 1");

		final Callable<Void> task1 = () -> {
			throw exception1;
		};
		final Callable<Void> task2 = () -> {
			throw exception2;
		};
		final Callable<Void> task3 = () -> {
			throw error1;
		};

		final Callable<Void> noEx = () -> null;

		// check if exceptions get thrown as intended
		ParallelExecutionException ex;

		ex = assertThrows(ParallelExecutionException.class, () -> executor.doParallel(task1, task2));
		assertThat(ex).hasCauseThat().isSameInstanceAs(exception1);
		assertThat(ex.getSuppressed()).hasLength(1);
		assertThat(ex.getSuppressed()[0]).hasCauseThat().isSameInstanceAs(exception2);

		ex = assertThrows(ParallelExecutionException.class, () -> executor.doParallel(task1, noEx));
		assertThat(ex).hasCauseThat().isSameInstanceAs(exception1);
		assertThat(ex.getSuppressed()).isEmpty();

		ex = assertThrows(ParallelExecutionException.class, () -> executor.doParallel(noEx, task2));
		assertThat(ex).hasCauseThat().hasCauseThat().isSameInstanceAs(exception2);
		assertThat(ex.getSuppressed()).isEmpty();

		ex = assertThrows(ParallelExecutionException.class, () -> executor.doParallel(task3, noEx));
		assertThat(ex).hasCauseThat().isSameInstanceAs(error1);
		assertThat(ex.getSuppressed()).isEmpty();

		ex = assertThrows(ParallelExecutionException.class, () -> executor.doParallel(noEx, task3));
		assertThat(ex).hasCauseThat().hasCauseThat().isSameInstanceAs(error1);
		assertThat(ex.getSuppressed()).isEmpty();
	}
}
