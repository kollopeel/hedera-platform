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

package com.swirlds.common.io;

import com.swirlds.common.constructable.RuntimeConstructable;

/**
 * An object implementing this interface will have a way of serializing and deserializing itself. This
 * serialization must deterministically generate the same output bytes every time. If the bytes generated
 * by the serialization algorithm change due to code changes then then this must be captured via a protocol
 * version increase. SerializableDet objects are required to maintain the capability of deserializing objects
 * serialized using old protocols.
 */
public interface SerializableDet extends RuntimeConstructable, Versioned {

	/**
	 * Any version lower than this is not supported and will cause
	 * an exception to be thrown if it is attempted to be used.
	 *
	 * @return minimum supported version number
	 */
	default int getMinimumSupportedVersion() {
		return 1;
	}

}
