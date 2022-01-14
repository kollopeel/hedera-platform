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

package com.swirlds.platform;

/**
 * A temporary interface to bridge circumvent the fact that the Settings class is package private
 */
public interface SettingsProvider {
	/**
	 * Returns true if beta mirror node support including zero stake support is enabled
	 */
	boolean isEnableBetaMirror();

	/**
	 * Returns the inverse of a probability that we will create a child for a childless event
	 */
	int getRescueChildlessInverseProbability();

	/**
	 * The probability that after a sync, a node will create an event with a random other parent. The probability is
	 * is 1 in X, where X is the value of randomEventProbability. A value of 0 means that a node will not create any
	 * random events.
	 *
	 * This feature is used to get consensus on events with no descendants which are created by nodes who go offline.
	 */
	int getRandomEventProbability();

	/**
	 * if true, both nodes send and wait for a COMM_EVENT_DONE byte after finishing reading and writing all events of a
	 * sync
	 */
	boolean shouldSendSyncDoneByte();

	/**
	 * Defines a "falling behind" node as a one that received at least N * throttle7threshold events in a sync. A good
	 * choice for this constant might be 1+2*d if a fraction d of received events are duplicates.
	 */
	double getThrottle7Threshold();

	/** if a sync has neither party falling behind, increase the bytes sent by this fraction */
	double getThrottle7Extra();

	/** the maximum number of slowdown bytes to be sent during a sync */
	int getThrottle7MaxBytes();

	/** indicates if throttle7 is enabled or not */
	boolean isThrottle7Enabled();
}
