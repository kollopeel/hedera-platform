/*
 * (c) 2016-2021 Swirlds, Inc.
 *
 * This software is owned by Swirlds, Inc., which retains title to the software. This software is protected by various
 * intellectual property laws throughout the world, including copyright and patent laws. This software is licensed and
 * not sold. You must use this software only in accordance with the terms of the Hashgraph Open Review license at
 *
 * https://github.com/hashgraph/swirlds-open-review/raw/master/LICENSE.md
 *
 * SWIRLDS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THIS SOFTWARE, EITHER EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * OR NON-INFRINGEMENT.
 */

package com.swirlds.fchashmap.internal;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This object is capable of iterating over all keys that map to a given value
 *
 * @param <K>
 * 		the type of the key
 * @param <V>
 * 		the type of the value
 */
public class FCOneToManyRelationIterator<K, V> implements Iterator<V> {

	private final Map<Pair<K, Integer>, V> associationMap;
	private final K key;
	private final int endIndex;

	private int nextIndex;

	/**
	 * Create a new iterator.
	 *
	 * @param associationMap
	 * 		the map containing all associations
	 * @param key
	 * 		the key whose values will be iterated
	 * @param startIndex
	 * 		the index where iteration starts (inclusive)
	 * @param endIndex
	 * 		the index where iteration ends (exclusive)
	 */
	public FCOneToManyRelationIterator(
			final Map<Pair<K, Integer>, V> associationMap,
			final K key,
			final int startIndex,
			final int endIndex) {

		this.associationMap = associationMap;
		this.key = key;
		this.endIndex = endIndex;
		this.nextIndex = startIndex;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasNext() {
		return nextIndex < endIndex;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		final int index = nextIndex;
		nextIndex++;

		final V next = associationMap.get(Pair.of(key, index));
		if (next == null) {
			throw new IllegalStateException("end index exceeds number of available values");
		}
		return next;
	}
}