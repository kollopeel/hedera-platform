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

package com.swirlds.platform.sync;

/**
 * A graph view facade for the descendants of a given event in a shadow graph.
 */
class SyncShadowGraphDescendantView implements Iterable<SyncShadowEvent> {
	/**
	 * The shadow event from which iterator begins
	 */
	private final SyncShadowEvent start;

	/**
	 * Construct a DFS graph descendant view for a given starting event
	 *
	 * @param start
	 * 		the starting event
	 */
	public SyncShadowGraphDescendantView(final SyncShadowEvent start) {
		this.start = start;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SyncShadowGraphDescendantDFSIterator iterator() {
		return new SyncShadowGraphDescendantDFSIterator(start);
	}
}