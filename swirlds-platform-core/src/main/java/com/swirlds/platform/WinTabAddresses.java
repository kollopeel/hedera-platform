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
package com.swirlds.platform;

import com.swirlds.common.system.Platform;

import javax.swing.JTextArea;
import java.util.Arrays;

/**
 * The tab in the Browser window that shows available apps, running swirlds, and saved swirlds.
 */
class WinTabAddresses extends WinBrowser.PrePaintableJPanel {
	private static final long serialVersionUID = 1L;
	/** the entire table is in this single Component */
	private JTextArea text;
	/** should the entire window be rebuilt? */
	private boolean redoWindow = true;

	/**
	 * Instantiate and initialize content of this tab.
	 */
	public WinTabAddresses() {
		text = WinBrowser.newJTextArea();
		add(text);
	}

	/** {@inheritDoc} */
	void prePaint() {
		if (!redoWindow) {
			return;
		}
		redoWindow = false;
		String s = "";
		synchronized (Browser.platforms) {
			for (Platform p : Browser.platforms) {
				s += "\n" + p.getAddress().getId() + "   " +//
						p.getAddress().getNickname() + "   " + //
						p.getAddress().getSelfName() + "   " +//
						Arrays.toString(p.getAddress().getAddressInternalIpv4())
						+ "   " +//
						p.getAddress().getPortInternalIpv4() + "   " +//
						Arrays.toString(p.getAddress().getAddressExternalIpv4())
						+ "   " +//
						p.getAddress().getPortExternalIpv4();
			}
		}
		s += Utilities.wrap(70, "\n\n" //
				+ "The above are all the member addresses. "
				+ "Each address includes the nickname, name, "
				+ "internal IP address/port and external IP address/port.");

		text.setText(s);
	}
}
