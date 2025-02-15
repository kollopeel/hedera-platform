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

package com.swirlds.platform.components;

import com.swirlds.common.system.SwirldMain;

/**
 * Manages the interaction with {@link SwirldMain}
 */
public interface SwirldMainManager {
	/**
	 * Announce that an event is about to be created.
	 *
	 * This is called just before an event is created, to give the Platform a chance to create any system
	 * transactions that should be sent out immediately. It is similar to SwirldMain.preEvent, except that
	 * the "app" is the Platform itself.
	 */
	void preEvent();
}
