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

package com.swirlds.merkletree.internal;

import com.swirlds.common.merkle.MerkleNode;
import com.swirlds.common.merkle.iterators.MerkleDepthFirstIterator;
import com.swirlds.merkletree.MerkleTreeInternalNode;

public class MerkleTreeLeafIterator<T extends MerkleNode>
		extends MerkleDepthFirstIterator<MerkleNode, T> {

	public MerkleTreeLeafIterator(final MerkleTreeInternalNode root) {
		super(root);
	}

	@Override
	public boolean shouldChildBeConsidered(final MerkleNode parent, final MerkleNode child) {
		return parent.getClassId() == MerkleTreeInternalNode.CLASS_ID;
	}

	@Override
	public boolean shouldNodeBeReturned(final MerkleNode node) {
		return node != null && node.getClassId() != MerkleTreeInternalNode.CLASS_ID;
	}
}