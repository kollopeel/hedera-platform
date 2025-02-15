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

package com.swirlds.common.bloom.hasher;

import com.swirlds.common.bloom.BloomHasher;
import com.swirlds.common.io.streams.SerializableDataInputStream;
import com.swirlds.common.io.streams.SerializableDataOutputStream;

import java.io.IOException;

import static com.swirlds.common.utility.NonCryptographicHashing.hash64;

/**
 * A {@link BloomHasher} capable of hashing longs.
 */
public class LongBloomHasher implements BloomHasher<Long> {

	private static final long CLASS_ID = 0x661ac9ecfb26ac2L;

	private static final class ClassVersion {
		public static final int ORIGINAL = 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void hash(final Long element, final long maxHash, final long[] hashes) {
		long runningHash = element;
		for (int index = 0; index < hashes.length; index++) {
			runningHash = hash64(runningHash);
			hashes[index] = Math.abs(runningHash) % maxHash;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getClassId() {
		return CLASS_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serialize(final SerializableDataOutputStream out) throws IOException {
		// no-op
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deserialize(final SerializableDataInputStream in, final int version) throws IOException {
		// no-op
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getVersion() {
		return ClassVersion.ORIGINAL;
	}
}
