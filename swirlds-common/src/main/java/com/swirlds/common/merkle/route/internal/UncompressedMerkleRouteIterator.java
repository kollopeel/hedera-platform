/*
 * (c) 2016-2022 Swirlds, Inc.
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

package com.swirlds.common.merkle.route.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterates over steps in a route encoded using {@link UncompressedMerkleRoute}.
 */
public class UncompressedMerkleRouteIterator implements Iterator<Integer> {

	private final int[] routeData;
	private int nextIndex;

	public UncompressedMerkleRouteIterator(final int[] routeData) {
		this.routeData = routeData;
		nextIndex = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasNext() {
		return nextIndex < routeData.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer next() {
		if (nextIndex > routeData.length) {
			throw new NoSuchElementException();
		}
		final int step = routeData[nextIndex];
		nextIndex++;
		return step;
	}
}
