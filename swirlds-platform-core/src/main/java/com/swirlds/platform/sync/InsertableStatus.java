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

package com.swirlds.platform.sync;

/**
 * A status representing the ability of the {@link ShadowGraph} to insert an event.
 */
public enum InsertableStatus {
	/** The event can be inserted into the shadow graph. */
	INSERTABLE,
	/** The event cannot be inserted into the shadow graph because it is null. */
	NULL_EVENT,
	/** The event cannot be inserted into the shadow graph because it is already in the shadow graph. */
	DUPLICATE_SHADOW_EVENT,
	/** The event cannot be inserted into the shadow graph because it belongs to an expired generation. */
	EXPIRED_EVENT,
	/**
	 * The event cannot be inserted into the shadow graph because its self parent is unknown to this graph and is not
	 * expired.
	 */
	UNKNOWN_CURRENT_SELF_PARENT,
	/**
	 * The event cannot be inserted into the shadow graph because its other parent is unknown to this graph and is not
	 * expired.
	 */
	UNKNOWN_CURRENT_OTHER_PARENT
}
