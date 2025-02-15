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

package com.swirlds.common.stream;

import com.swirlds.common.crypto.Hash;
import com.swirlds.common.crypto.RunningHashable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static com.swirlds.logging.LogMarker.OBJECT_STREAM;

/**
 * A MultiStream instance might have multiple nextStreams.
 * It accepts a SerializableRunningHashable object each time, and sends it to each of its nextStreams
 *
 * @param <T>
 * 		type of the objects
 */
public class MultiStream<T extends RunningHashable> implements LinkedObjectStream<T> {
	/** use this for all logging, as controlled by the optional data/log4j2.xml file */
	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * message of the exception thrown when setting a nextStream to be null
	 */
	public static final String NEXT_STREAM_NULL = "MultiStream should not have null nextStream";

	/**
	 * nextStreams should have at least this many elements
	 */
	private static final int NEXT_STREAMS_MIN_SIZE = 1;

	/**
	 * message of the exception thrown when nextStreams has less than two elements
	 */
	public static final String NOT_ENOUGH_NEXT_STREAMS = String.format("MultiStream should have at least %d " +
			"nextStreams", NEXT_STREAMS_MIN_SIZE);

	/**
	 * a list of LinkedObjectStreams which receives objects from this multiStream
	 */
	private List<LinkedObjectStream<T>> nextStreams;

	public MultiStream(List<LinkedObjectStream<T>> nextStreams) {
		if (nextStreams == null || nextStreams.size() < NEXT_STREAMS_MIN_SIZE) {
			throw new IllegalArgumentException(NOT_ENOUGH_NEXT_STREAMS);
		}

		for (LinkedObjectStream<T> nextStream : nextStreams) {
			if (nextStream == null) {
				throw new IllegalArgumentException(NEXT_STREAM_NULL);
			}
		}
		this.nextStreams = nextStreams;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRunningHash(final Hash hash) {
		for (LinkedObjectStream<T> nextStream : nextStreams) {
			nextStream.setRunningHash(hash);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addObject(T t) {
		for (LinkedObjectStream<T> nextStream : nextStreams) {
			nextStream.addObject(t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		for (LinkedObjectStream<T> nextStream : nextStreams) {
			nextStream.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		for (LinkedObjectStream<T> nextStream : nextStreams) {
			nextStream.close();
		}
		LOGGER.info(OBJECT_STREAM.getMarker(), "MultiStream is closed");
	}
}

