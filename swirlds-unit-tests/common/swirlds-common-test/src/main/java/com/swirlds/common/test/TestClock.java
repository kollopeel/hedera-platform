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

package com.swirlds.common.test;

import com.swirlds.common.utility.Units;

import java.time.Duration;
import java.time.Instant;

/**
 * A clock used for unit tests where the user can control the time it outputs
 */
public class TestClock {
	private Instant time = Instant.ofEpochSecond(0);

	public void advanceClock(final Duration duration) {
		time = time.plus(duration);
	}

	public Instant instant() {
		return time;
	}

	public long nanoTime() {
		// if we start off epoch 0, this will not overflow
		return (time.getEpochSecond() * Units.SECONDS_TO_NANOSECONDS) + time.getNano();
	}
}
