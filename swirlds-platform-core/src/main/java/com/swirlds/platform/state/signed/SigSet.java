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

package com.swirlds.platform.state.signed;

import com.swirlds.common.FastCopyable;
import com.swirlds.common.constructable.ConstructableIgnored;
import com.swirlds.common.internal.SettingsCommon;
import com.swirlds.common.io.SelfSerializable;
import com.swirlds.common.io.streams.SerializableDataInputStream;
import com.swirlds.common.io.streams.SerializableDataOutputStream;
import com.swirlds.common.system.address.AddressBook;
import com.swirlds.platform.Utilities;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

/** all the known signatures for a particular signed state */
@ConstructableIgnored /* has to be ignored, doesn't work with a no-args constructor at the moment */
public class SigSet implements FastCopyable, SelfSerializable {
	private static final long CLASS_ID = 0x756d0ee945226a92L;

	private static class ClassVersion {
		public static final int ORIGINAL = 1;
		public static final int MIGRATE_TO_SERIALIZABLE = 2;
	}

	/** the number of signatures collected */
	private volatile int count;
	/** total stake of all members whose signatures have been collected so far */
	private volatile long stakeCollected = 0;
	/** the number of members, each of which can sign */
	private int numMembers;
	/** have members with more than 1/2 of the total stake signed? */
	private volatile boolean complete = false;
	/** array element i is the signature for the member with ID i */
	private AtomicReferenceArray<SigInfo> sigInfos;
	/** the address book in force at the time of this state */
	private final AddressBook addressBook;

	/** get array where element i is the signature for the member with ID i */
	private SigInfo[] getSigInfosCopy() {
		SigInfo[] a = new SigInfo[numMembers];
		for (int i = 0; i < a.length; i++) {
			a[i] = sigInfos.get(i);
		}
		return a;
	}

	private SigSet(final SigSet sourceSigSet) {
		this.count = sourceSigSet.getCount();
		this.stakeCollected = sourceSigSet.getStakeCollected();
		this.numMembers = sourceSigSet.getNumMembers();
		this.complete = sourceSigSet.isComplete();
		this.sigInfos = sourceSigSet.sigInfos;
		this.addressBook = sourceSigSet.getAddressBook().copy();
	}

	/**
	 * create the signature set, taking the address book at this moment as the population
	 *
	 * @param addressBook
	 * 		the address book at this moment
	 */
	public SigSet(AddressBook addressBook) {
		this.addressBook = addressBook;
		this.numMembers = addressBook.getSize();
		sigInfos = new AtomicReferenceArray<>(numMembers);
		count = 0;
		stakeCollected = 0;
	}

	/**
	 * Register a new signature for one member. If this member already has a signed state here, then the new
	 * one that was passed in is ignored.
	 *
	 * @param sigInfo
	 * 		a sigInfo to be added
	 */
	public synchronized void addSigInfo(SigInfo sigInfo) {
		int id = (int) sigInfo.getMemberId();
		if (sigInfos.get(id) != null) {
			return; // ignore if we already have a signature for this member
		}
		sigInfos.set(id, sigInfo);
		count++;
		stakeCollected += addressBook.getAddress(id).getStake();
		calculateComplete();
	}

	/**
	 * Although &ge;1/3 is sufficient proof that at least 1 honest node has signed this state, in the presence
	 * of ISS bugs a &gt;1/2 threshold makes it very difficult to accidentally produce two different completely
	 * signed states from the same round.(Although this is not impossible in the theoretical sense,
	 * given that a malicious nodes could sign multiple versions of the state.)
	 */
	private void calculateComplete() {
		complete = Utilities.isMajority(stakeCollected, addressBook.getTotalStake());
	}

	/**
	 * Get the SigInfo for the given ID
	 *
	 * @param memberId
	 * 		the ID of the member
	 * @return returns the SigInfo object or null if we don't have this members sig
	 */
	public SigInfo getSigInfo(int memberId) {
		return sigInfos.get(memberId);
	}

	/**
	 * @return does this contain signatures from members with greater than 1/2 of the total stake?
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * @return true if this set has signatures from all the members, false otherwise
	 */
	boolean hasAllSigs() {
		if (SettingsCommon.enableBetaMirror) {
			// If beta mirror node logic is enabled then we should only consider nodes with stake in this validation
			// However, since mirror nodes themselves will still create their own signature in addition to the nodes
			// with actual stake we must also allow (numWithStake + 1) as a valid condition
			return count == addressBook.getNumberWithStake() || count == addressBook.getNumberWithStake() + 1;
		} else {
			return count == numMembers;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SigSet copy() {
		throwIfImmutable();
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeInt(numMembers);
		out.writeSerializableArray(getSigInfosCopy(), false, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
		numMembers = in.readInt();
		SigInfo[] sigInfoArr = in.readSerializableArray(
				SigInfo[]::new,
				addressBook.getSize(),
				false,
				SigInfo::new);
		processDeserializedSigInfoArray(sigInfoArr);
	}

	private synchronized void processDeserializedSigInfoArray(SigInfo[] sigInfoArr) {
		count = 0;
		stakeCollected = 0;
		sigInfos = new AtomicReferenceArray<>(numMembers);
		for (int id = 0; id < sigInfoArr.length; id++) {
			if (sigInfoArr[id] != null) {
				sigInfos.set(id, sigInfoArr[id]);
				count++;
				stakeCollected += addressBook.getAddress(id).getStake();
			}
		}

	}

	/**
	 * getter for the number of signatures collected
	 *
	 * @return number of signatures collected
	 */
	public int getCount() {
		return count;
	}

	/**
	 * getter for total stake of all members whose signatures have been collected so far
	 *
	 * @return total stake of members whose signatures have been collected
	 */
	public long getStakeCollected() {
		return stakeCollected;
	}

	/**
	 * getter for the number of members, each of which can sign
	 *
	 * @return number of members
	 */
	public int getNumMembers() {
		return numMembers;
	}

	public AddressBook getAddressBook() {
		return addressBook;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SigSet sigSet = (SigSet) o;

		if (sigInfos.length() != sigSet.sigInfos.length()) {
			return false;
		}

		for (int i = 0; i < sigInfos.length(); i++) {
			if (!Objects.equals(sigInfos.get(i), sigSet.sigInfos.get(i))) {
				return false;
			}
		}

		return new EqualsBuilder()
				.append(count, sigSet.count)
				.append(stakeCollected, sigSet.stakeCollected)
				.append(numMembers, sigSet.numMembers)
				.append(complete, sigSet.complete)
				//.append(sigInfos, sigSet.sigInfos) atomic array does not implement equals()
				.append(addressBook, sigSet.addressBook)
				.isEquals();

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(count)
				.append(stakeCollected)
				.append(numMembers)
				.append(complete)
				.append(sigInfos)
				.append(addressBook)
				.toHashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("count", count)
				.append("stakeCollected", stakeCollected)
				.append("numMembers", numMembers)
				.append("complete", complete)
				.append("sigInfos", sigInfos)
				.toString();
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
		return ClassVersion.MIGRATE_TO_SERIALIZABLE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMinimumSupportedVersion() {
		return ClassVersion.MIGRATE_TO_SERIALIZABLE;
	}
}


