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

package com.swirlds.common.notification.listeners;

import com.swirlds.common.system.SwirldState;
import com.swirlds.common.notification.AbstractNotification;

import java.time.Instant;

/**
 * Class that provides {@link com.swirlds.common.notification.Notification} when reconnect completes
 */
public class ReconnectCompleteNotification extends AbstractNotification {

	private long roundNumber;
	private Instant consensusTimestamp;
	private SwirldState state;

	public ReconnectCompleteNotification(final long roundNumber, final Instant consensusTimestamp,
			final SwirldState state) {
		this.roundNumber = roundNumber;
		this.consensusTimestamp = consensusTimestamp;
		this.state = state;
	}

	/**
	 * get round number from the {@link SwirldState}
	 *
	 * @return round number
	 */
	public long getRoundNumber() {
		return roundNumber;
	}

	/**
	 * The last consensus timestamp handled before the state was signed to the callback
	 *
	 * @return last consensus timestamp
	 */
	public Instant getConsensusTimestamp() {
		return consensusTimestamp;
	}

	/**
	 * get the {@link SwirldState} instance
	 *
	 * @return SwirldState
	 */
	public SwirldState getState() {
		return state;
	}
}
