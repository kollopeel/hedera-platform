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

package com.swirlds.platform.network.connectivity;

import com.swirlds.common.system.address.AddressBook;
import com.swirlds.common.system.NodeId;
import com.swirlds.common.threading.interrupt.InterruptableConsumer;
import com.swirlds.platform.SettingsProvider;
import com.swirlds.platform.SocketConnection;
import com.swirlds.platform.Connection;
import com.swirlds.platform.network.ByteConstants;
import com.swirlds.platform.network.ConnectionTracker;
import com.swirlds.platform.network.NetworkUtils;
import com.swirlds.platform.sync.SyncInputStream;
import com.swirlds.platform.sync.SyncOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static com.swirlds.logging.LogMarker.EXCEPTION;
import static com.swirlds.logging.LogMarker.SOCKET_EXCEPTIONS;
import static com.swirlds.logging.LogMarker.SYNC;

/**
 * Accept inbound connections and executes the platform handshake. This class is thread-safe
 */
public class InboundConnectionHandler {
	private static final Logger LOG = LogManager.getLogger();

	private final ConnectionTracker connectionTracker;
	private final NodeId selfId;
	private final AddressBook addressBook;
	private final InterruptableConsumer<Connection> newConnectionConsumer;
	private final SettingsProvider settings;

	public InboundConnectionHandler(
			final ConnectionTracker connectionTracker,
			final NodeId selfId,
			final AddressBook addressBook,
			final InterruptableConsumer<Connection> newConnectionConsumer,
			final SettingsProvider settings) {
		this.connectionTracker = connectionTracker;
		this.selfId = selfId;
		this.addressBook = addressBook;
		this.newConnectionConsumer = newConnectionConsumer;
		this.settings = settings;
	}

	/**
	 * Authenticate the peer that has just established a new connection and create a {@link Connection}
	 *
	 * @param clientSocket
	 * 		the newly created socket
	 */
	public void handle(final Socket clientSocket) {
		DataInputStream dis = null;
		DataOutputStream dos = null;
		long otherId = -1;
		long acceptTime = 0;
		try {
			acceptTime = System.currentTimeMillis();
			clientSocket.setTcpNoDelay(settings.isTcpNoDelay());
			clientSocket.setSoTimeout(settings.getTimeoutSyncClientSocket());
			dis = new DataInputStream(clientSocket.getInputStream());

			dos = new DataOutputStream(clientSocket.getOutputStream());

			final String otherKey = dis.readUTF();

			otherId = addressBook.getId(otherKey);

			dos.writeInt(ByteConstants.COMM_CONNECT);// send an ACK for creating connection
			dos.flush();

			final SyncInputStream sis = SyncInputStream.createSyncInputStream(
					clientSocket.getInputStream(), settings.connectionStreamBufferSize());
			final SyncOutputStream sos = SyncOutputStream.createSyncOutputStream(
					clientSocket.getOutputStream(), settings.connectionStreamBufferSize());

			final SocketConnection sc = SocketConnection.create(
					selfId,
					NodeId.createMain(otherId),
					connectionTracker,
					false,
					clientSocket,
					sis,
					sos
			);
			newConnectionConsumer.accept(sc);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.warn(SOCKET_EXCEPTIONS.getMarker(),
					"Inbound connection from {} to {} was interrupted:", selfId, otherId, e);
			NetworkUtils.close(dis, dos, clientSocket);
		} catch (final IOException e) {
			LOG.warn(SOCKET_EXCEPTIONS.getMarker(),
					"Inbound connection from {} to {} had IOException:", selfId, otherId, e);
			NetworkUtils.close(dis, dos, clientSocket);
		} catch (final RuntimeException e) {
			LOG.error(EXCEPTION.getMarker(),
					"Inbound connection error, remote IP: {}\n" +
							"Time from accept to exception: {} ms",
					clientSocket.getInetAddress().toString(),
					acceptTime == 0 ? "N/A" : (System.currentTimeMillis() - acceptTime), e);
			LOG.error(SYNC.getMarker(),
					"Listener {} hearing {} had general Exception:", selfId, otherId, e);
			NetworkUtils.close(dis, dos, clientSocket);
		}
	}
}
