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

package com.swirlds.virtualmap.internal.merkle;

import com.swirlds.common.constructable.ConstructableIgnored;
import com.swirlds.common.io.streams.SerializableDataInputStream;
import com.swirlds.common.io.streams.SerializableDataOutputStream;
import com.swirlds.common.merkle.MerkleLeaf;
import com.swirlds.common.merkle.impl.PartialMerkleLeaf;
import com.swirlds.virtualmap.VirtualKey;
import com.swirlds.virtualmap.VirtualValue;
import com.swirlds.virtualmap.datasource.VirtualInternalRecord;
import com.swirlds.virtualmap.datasource.VirtualLeafRecord;
import com.swirlds.virtualmap.datasource.VirtualRecord;

import java.io.IOException;
import java.util.Objects;

/**
 * Implementation of a VirtualLeaf
 */
@ConstructableIgnored
public final class VirtualLeafNode<K extends VirtualKey<? super K>, V extends VirtualValue>
		extends PartialMerkleLeaf
		implements MerkleLeaf, VirtualNode {

	public static final long CLASS_ID = 0x499677a326fb04caL;

	private static class ClassVersion {

		public static final int ORIGINAL = 1;
	}

	/**
	 * The {@link VirtualRecord} is the backing data for this node. There are different types
	 * of records, {@link VirtualInternalRecord} for internal nodes and
	 * {@link com.swirlds.virtualmap.datasource.VirtualLeafRecord} for leaf nodes.
	 */
	private final VirtualRecord virtualRecord;

	public VirtualLeafNode(final VirtualLeafRecord<K, V> virtualRecord) {
		this.virtualRecord = Objects.requireNonNull(virtualRecord);
		setHash(virtualRecord.getHash());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public VirtualRecord getVirtualRecord() {
		return virtualRecord;
	}

	/**
	 * Get the key represented held within this leaf.
	 *
	 * @return the key
	 */
	@SuppressWarnings("unchecked")
	public K getKey() {
		return ((VirtualLeafRecord<K, V>) virtualRecord).getKey();
	}

	/**
	 * Get the value held within this leaf.
	 *
	 * @return the value
	 */
	@SuppressWarnings("unchecked")
	public V getValue() {
		return ((VirtualLeafRecord<K, V>) virtualRecord).getValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public VirtualLeafNode<K, V> copy() {
		throw new UnsupportedOperationException("Don't use this");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getClassId() {
		return CLASS_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getVersion() {
		return ClassVersion.ORIGINAL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "VirtualLeafNode{" + virtualRecord + "}";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serialize(final SerializableDataOutputStream out) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deserialize(final SerializableDataInputStream in, final int version) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final VirtualLeafNode<?, ?> that = (VirtualLeafNode<?, ?>) o;
		return virtualRecord.equals(that.getVirtualRecord());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return virtualRecord.hashCode();
	}
}
