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

package com.swirlds.jasperdb;

import com.swirlds.common.io.SelfSerializable;

import java.util.function.Supplier;

/**
 * A supplier that is also self serializable.
 *
 * @param <T>
 * 		the type of object that is supplied
 */
public interface SelfSerializableSupplier<T> extends Supplier<T>, SelfSerializable {

}
