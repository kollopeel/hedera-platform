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

import com.swirlds.common.crypto.Hash;
import com.swirlds.common.events.Event;
import com.swirlds.platform.EventImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.swirlds.common.CommonUtils.throwArgNull;
import static com.swirlds.logging.LogMarker.EXPIRE_EVENT;

/**
 * A shadow graph is a graph which replicates the hashgraph structure, with child links in addition to parent links.
 * It is aggregated by the shadow graph manager, which is its external interface.
 * these additional links facilitate searching when constructing a list of
 * hashgraph events to be sent for gossiping.
 *
 * It supports:
 * <ul>
 * <li>insertion of a shadow event</li>
 * <li>expiration of a shadow event and its ancestry</li>
 * <li>querying for a shadow event by hashgraph event base hash</li>
 * <li>providing a descendant sub-DAG iterator of a given shadow event</li>
 * </ul>
 * A shadow graph is thread-unaware. It provides no synchronization or general concurrency guarantees.
 * Use of this type in production code is through the {@link ShadowGraphManager} type.
 *
 * Because a shadow graph's events are expired by generation, and hashgraph's events are expired by round-created, it
 * is typical for a shadow graph to have a references to a small number of expired hashgraph events.
 *
 */
public class ShadowGraph implements Iterable<ShadowEvent> {
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * to look up an event from the received hash
	 */
	private final Map<Hash, ShadowEvent> hashToShadowEvent;

	/**
	 * all events currently in the shadow graph
	 */
	private final Set<ShadowEvent> shadowEvents;

	/**
	 * Construct an empty shadow graph
	 */
	public ShadowGraph() {
		this.hashToShadowEvent = new HashMap<>();
		this.shadowEvents = new HashSet<>();
	}

	/**
	 * Reset this shadow graph its default-constructed, empty, state
	 */
	public void clear() {
		this.hashToShadowEvent.clear();
		this.shadowEvents.clear();
	}

	/**
	 * Get the number of shadow events
	 *
	 * @return the number of shadow events
	 */
	public int getNumShadowEvents() {
		return shadowEvents.size();
	}

	/**
	 * Get the shadow event of a hashgraph event
	 *
	 * @param e
	 * 		hashgraph event
	 * @return the shadow event
	 */
	public ShadowEvent shadow(final Event e) {
		if (e == null) {
			return null;
		}

		return hashToShadowEvent.get(e.getBaseHash());
	}

	/**
	 * Get the shadow event of a hashgraph event with a given hash
	 *
	 * @param hash
	 * 		the hash
	 * @return the shadow event
	 */
	public ShadowEvent shadow(final Hash hash) {
		return hashToShadowEvent.get(hash);
	}

	/**
	 * Get a reference to the shadow event set for this shadow graph
	 *
	 * @return a reference to the shadow event set for this shadow graph
	 */
	public Set<ShadowEvent> getShadowEvents() {
		return shadowEvents;
	}

	/**
	 * Get a reference to the hash map of cryptographic hash to shadow event
	 *
	 * @return a reference to the hash map of cryptographic hash to shadow event
	 */
	public Map<Hash, ShadowEvent> getHashToShadowEvent() {
		return hashToShadowEvent;
	}

	/**
	 * Attach a shadow of a Hashgraph event to this graph. Only a shadow for which a parent
	 * hash matches a hash in this@entry is inserted.
	 *
	 * @param event
	 * 		The Hashgraph event to be inserted
	 * @return the shadow event that references inserted hashgraph event
	 */
	public ShadowEvent insert(final Event event) {
		return insert(new ShadowEvent(event));
	}

	/**
	 * Attach a shadow of a Hashgraph event to this graph. Only a shadow for which a parent
	 * hash matches a hash in this@entry is inserted.
	 *
	 * @param shadowEvent
	 * 		The Hashgraph event shadow to be inserted
	 * @return the inserted shadow event
	 */
	private ShadowEvent insert(final ShadowEvent shadowEvent) {
		final ShadowEvent sp = shadow(shadowEvent.getEvent().getSelfParent());
		final ShadowEvent op = shadow(shadowEvent.getEvent().getOtherParent());

		if (sp != null) {
			sp.addSelfChild(shadowEvent);
		}

		if (op != null) {
			op.addOtherChild(shadowEvent);
		}

		hashToShadowEvent.put(shadowEvent.getEventBaseHash(), shadowEvent);
		shadowEvents.add(shadowEvent);

		return shadowEvent;
	}

	/**
	 * Remove from this graph every self-ancestor of a given shadow event that satisfies a given predicate, excluding
	 * the event itself.
	 *
	 * @param s
	 * 		The shadow event whose self-ancestors are to be removed
	 * @param p
	 * 		The selection predicate for ancestral events
	 * @return The number of removed events
	 */
	public int removeStrictSelfAncestry(final ShadowEvent s, final Predicate<ShadowEvent> p) {
		return removeSelfAncestry(s.getSelfParent(), p);
	}

	/**
	 * Remove from this graph every self-ancestor of a given shadow event that satisfies a given predicate. This
	 * includes the given event.
	 *
	 * @param s
	 * 		The shadow event whose self-ancestors are to be removed
	 * @param p
	 * 		The selection predicate for ancestral events
	 * @return The number of removed events
	 */
	public int removeSelfAncestry(ShadowEvent s, final Predicate<ShadowEvent> p) {
		final Deque<ShadowEvent> stack = new ArrayDeque<>();

		while (s != null) {
			if (p.test(s)) {
				stack.push(s);
			}
			s = s.getSelfParent();
		}

		final int count = stack.size();

		while (!stack.isEmpty()) {
			remove(stack.pop());
		}

		return count;
	}

	/**
	 * Remove from this graph every ancestor of a given shadow event that satisfies a given predicate, excluding the
	 * event itself.
	 *
	 * @param s
	 * 		The shadow event whose ancestors are to be removed
	 * @param p
	 * 		The selection predicate for ancestral events
	 * @return The number of removed events
	 */
	protected int removeStrictAncestry(final ShadowEvent s, final Predicate<ShadowEvent> p) {
		int count = 0;
		count += removeAncestry(s.getSelfParent(), p);
		count += removeAncestry(s.getOtherParent(), p);
		return count;
	}

	/**
	 * Remove from this graph every ancestor of a given shadow event that satisfies a given predicate. This includes the
	 * given event.
	 *
	 * @param s
	 * 		The shadow event whose ancestors are to be removed
	 * @param p
	 * 		The selection predicate for ancestral events
	 * @return The number of removed events
	 */
	public int removeAncestry(final ShadowEvent s, final Predicate<ShadowEvent> p) {
		if (s == null) {
			return 0;
		}

		int count = 0;

		count += removeAncestry(s.getSelfParent(), p);
		count += removeAncestry(s.getOtherParent(), p);

		if (p.test(s)) {
			count += remove(s) ? 1 : 0;
		}

		return count;
	}

	/**
	 * Remove a single event from this graph. This routine dissociates a given event from its
	 * parents and children, and removes it from internal storage.
	 *
	 * @param s
	 * 		The shadow event to be removed
	 * @return true iff the event was removed
	 */
	private boolean remove(final ShadowEvent s) {
		if (s == null) {
			return false;
		}

		LOG.debug(EXPIRE_EVENT.getMarker(),
				"SG removing {}", () -> ((EventImpl) s.getEvent()).toShortString());

		if (!shadowEvents.contains(s)) {
			return false;
		}

		s.disconnect();
		hashToShadowEvent.remove(s.getEventBaseHash());
		shadowEvents.remove(s);

		return true;
	}

	/**
	 * DFS graph iteration on descendants from the {@code start} node.
	 *
	 * @param start
	 * 		the node from which to begin the depth first search.
	 * @param sendingTips
	 * 		the list of sending tips to bound the traversal operation.
	 * @param visited
	 * 		the {@link Set} used to track visited state of each node.
	 * @param maxTipGenerations
	 * 		the {@link List} of maximum generations per creator.
	 * @param includeOtherChildren
	 * 		a flag indicating whether this iterator should traverse the other children of each node.
	 * @return a view instance for this iterator
	 * @throws IllegalArgumentException
	 * 		if the {@code start}, {@code sendingTips}, {@code visited}, or {@code maxTipGenerations} argument is
	 * 		passed a {@code null} value.
	 */
	public ShadowGraphDescendantView graphDescendants(
			final ShadowEvent start,
			final Set<Hash> sendingTips,
			final Set<ShadowEvent> visited,
			final List<Long> maxTipGenerations,
			final boolean includeOtherChildren) {
		throwArgNull(start, "start");
		throwArgNull(sendingTips, "sendingTips");
		throwArgNull(visited, "visited");
		throwArgNull(maxTipGenerations, "maxTipGenerations");

		return new ShadowGraphDescendantView(start, sendingTips, visited, maxTipGenerations, includeOtherChildren);
	}

	/**
	 * HashSet encounter-order iteration
	 *
	 * @return an iterator for the set of shadow events held by this graph
	 */
	@Override
	public Iterator<ShadowEvent> iterator() {
		return shadowEvents.iterator();
	}

}
