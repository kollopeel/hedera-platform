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

import com.swirlds.common.threading.framework.config.ThreadConfiguration;
import com.swirlds.common.threading.futures.StandardFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.swirlds.common.test.AssertionUtils.assertEventuallyTrue;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StandardFuture Tests")
class StandardFutureTests {

	@Test
	@DisplayName("Standard Use Test")
	void standardUseTest() throws InterruptedException {

		final StandardFuture<Integer> future = new StandardFuture<>();
		assertFalse(future.isDone(), "future should not be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		final int value = 12345;

		final AtomicBoolean finished = new AtomicBoolean(false);

		new ThreadConfiguration()
				.setRunnable(() -> {
					try {
						assertEquals(value, future.get(), "unexpected value");
					} catch (final Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					finished.set(true);
				})
				.build(true);

		assertFalse(finished.get(), "should not have finished");
		// Sleep a little while to allow future to complete if it wants to misbehave
		MILLISECONDS.sleep(20);
		assertFalse(finished.get(), "should not have finished");

		future.complete(value);
		assertTrue(future.isDone(), "future should be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		assertEventuallyTrue(finished::get, Duration.ofSeconds(1), "should have finished by now");
	}

	@Test
	@DisplayName("Standard Use getAndRethrow()")
	void standardUseGetAndRethrow() throws InterruptedException {
		final StandardFuture<Integer> future = new StandardFuture<>();
		assertFalse(future.isDone(), "future should not be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");
		final int value = 12345;

		final AtomicBoolean finished = new AtomicBoolean(false);

		new ThreadConfiguration()
				.setRunnable(() -> {
					try {
						assertEquals(value, future.getAndRethrow(), "unexpected value");
					} catch (final Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					finished.set(true);
				})
				.build(true);

		assertFalse(finished.get(), "should not have finished");
		// Sleep a little while to allow future to complete if it wants to misbehave
		MILLISECONDS.sleep(20);
		assertFalse(finished.get(), "should not have finished");

		future.complete(value);
		assertTrue(future.isDone(), "future should be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		assertEventuallyTrue(finished::get, Duration.ofSeconds(1), "should have finished by now");
	}

	@Test
	@DisplayName("Future Starts Out As Completed")
	void futureStartsOutAsCompleted() {

		final int value = 12345;
		final StandardFuture<Integer> future = new StandardFuture<>(value);
		assertTrue(future.isDone(), "future should be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		final AtomicBoolean finished = new AtomicBoolean(false);

		new ThreadConfiguration()
				.setRunnable(() -> {
					try {
						assertEquals(value, future.get(), "unexpected value");
					} catch (final Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					finished.set(true);
				})
				.build(true);

		assertEventuallyTrue(finished::get, Duration.ofSeconds(1), "should have finished by now");
	}

	@Test
	@DisplayName("get() With Timeout")
	void getWithTimeout() throws InterruptedException {
		final StandardFuture<Integer> future = new StandardFuture<>();
		assertFalse(future.isDone(), "future should not be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");
		final int value = 12345;

		final AtomicBoolean finished = new AtomicBoolean(false);

		new ThreadConfiguration()
				.setRunnable(() -> {
					// This one should time out
					assertThrows(TimeoutException.class, () -> future.get(1, MILLISECONDS));

					// This one should wait until the future is completed
					try {
						assertEquals(value, future.get(1, SECONDS), "unexpected value");
					} catch (final Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					finished.set(true);
				})
				.build(true);

		assertFalse(finished.get(), "should not have finished");
		// Sleep a little while to allow future to complete if it wants to misbehave
		MILLISECONDS.sleep(20);
		assertFalse(finished.get(), "should not have finished");

		future.complete(value);
		assertTrue(future.isDone(), "future should be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		assertEventuallyTrue(finished::get, Duration.ofSeconds(1), "should have finished by now");
	}

	@Test
	@DisplayName("getAndRethrow() With Timeout")
	void getAndRethrowWithTimeout() throws InterruptedException {
		final StandardFuture<Integer> future = new StandardFuture<>();
		assertFalse(future.isDone(), "future should not be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");
		final int value = 12345;

		final AtomicBoolean finished = new AtomicBoolean(false);

		new ThreadConfiguration()
				.setRunnable(() -> {
					// This one should time out
					assertThrows(TimeoutException.class, () -> future.getAndRethrow(1, MILLISECONDS));

					// This one should wait until the future is completed
					try {
						assertEquals(value, future.getAndRethrow(1, SECONDS), "unexpected value");
					} catch (final Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					finished.set(true);
				})
				.build(true);

		assertFalse(finished.get(), "should not have finished");
		// Sleep a little while to allow future to complete if it wants to misbehave
		MILLISECONDS.sleep(20);
		assertFalse(finished.get(), "should not have finished");

		future.complete(value);
		assertTrue(future.isDone(), "future should be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		assertEventuallyTrue(finished::get, Duration.ofSeconds(1), "should have finished by now");
	}

	@Test
	@DisplayName("cancel() Test")
	void cancelTest() throws InterruptedException {

		final StandardFuture<Integer> future = new StandardFuture<>();
		assertFalse(future.isDone(), "future should not be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		final AtomicBoolean finished = new AtomicBoolean(false);

		new ThreadConfiguration()
				.setRunnable(() -> {
					try {
						assertThrows(CancellationException.class, future::get,
								"expected future to be cancelled");
					} catch (final Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					finished.set(true);
				})
				.build(true);

		assertFalse(finished.get(), "should not have finished");
		// Sleep a little while to allow future to complete if it wants to misbehave
		MILLISECONDS.sleep(20);
		assertFalse(finished.get(), "should not have finished");

		future.cancel();
		assertTrue(future.isDone(), "future should be done");
		assertTrue(future.isCancelled(), "future should be cancelled");

		assertEventuallyTrue(finished::get, Duration.ofSeconds(1), "should have finished by now");
	}

	@Test
	@DisplayName("cancelWithError() Test")
	void cancelWithErrorTest() throws InterruptedException {

		final StandardFuture<Integer> future = new StandardFuture<>();
		assertFalse(future.isDone(), "future should not be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		final AtomicBoolean finished = new AtomicBoolean(false);

		new ThreadConfiguration()
				.setRunnable(() -> {
					try {
						assertThrows(ExecutionException.class, future::get,
								"expected future to be cancelled");
					} catch (final Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					finished.set(true);
				})
				.build(true);

		assertFalse(finished.get(), "should not have finished");
		// Sleep a little while to allow future to complete if it wants to misbehave
		MILLISECONDS.sleep(20);
		assertFalse(finished.get(), "should not have finished");

		future.cancelWithError(new RuntimeException());
		assertTrue(future.isDone(), "future should be done");
		assertTrue(future.isCancelled(), "future should be cancelled");

		assertEventuallyTrue(finished::get, Duration.ofSeconds(1), "should have finished by now");
	}

	@Test
	@DisplayName("cancelWithError() getAndRethrow() Test")
	void cancelWithErrorGetAndRethrowTest() throws InterruptedException {

		final StandardFuture<Integer> future = new StandardFuture<>();
		assertFalse(future.isDone(), "future should not be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		final AtomicBoolean finished = new AtomicBoolean(false);

		new ThreadConfiguration()
				.setRunnable(() -> {
					try {
						assertThrows(RuntimeException.class, future::getAndRethrow,
								"expected future to be cancelled");
					} catch (final Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					finished.set(true);
				})
				.build(true);

		assertFalse(finished.get(), "should not have finished");
		// Sleep a little while to allow future to complete if it wants to misbehave
		MILLISECONDS.sleep(20);
		assertFalse(finished.get(), "should not have finished");

		future.cancelWithError(new RuntimeException());
		assertTrue(future.isDone(), "future should be done");
		assertTrue(future.isCancelled(), "future should be cancelled");

		assertEventuallyTrue(finished::get, Duration.ofSeconds(1), "should have finished by now");
	}

	@Test
	@DisplayName("cancel() Callback Test")
	void cancelCallbackTest() throws InterruptedException {

		final AtomicBoolean finished = new AtomicBoolean(false);
		final AtomicBoolean callbackFinished = new AtomicBoolean(false);

		final StandardFuture<Integer> future = new StandardFuture<>(
				(final boolean interrupt, final Throwable exception) -> {
					assertTrue(interrupt, "interrupt should be true");
					assertTrue(exception instanceof IllegalAccessError, "exception should have correct type");
					callbackFinished.set(true);
				});
		assertFalse(future.isDone(), "future should not be done");
		assertFalse(future.isCancelled(), "future should not be cancelled");

		new ThreadConfiguration()
				.setRunnable(() -> {
					try {
						assertThrows(ExecutionException.class, future::get,
								"expected future to be cancelled");
					} catch (final Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					finished.set(true);
				})
				.build(true);

		assertFalse(finished.get(), "should not have finished");
		assertFalse(callbackFinished.get(), "should not have finished");
		// Sleep a little while to allow future to complete if it wants to misbehave
		MILLISECONDS.sleep(20);
		assertFalse(finished.get(), "should not have finished");
		assertFalse(callbackFinished.get(), "should not have finished");

		future.cancelWithError(true, new IllegalAccessError());
		assertTrue(future.isDone(), "future be done");
		assertTrue(future.isCancelled(), "future should be cancelled");

		assertEventuallyTrue(finished::get, Duration.ofSeconds(1), "should have finished by now");
		assertEventuallyTrue(callbackFinished::get, Duration.ofSeconds(1), "should have finished by now");
	}
}