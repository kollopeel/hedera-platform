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

package com.swirlds.common.test.merkle.util;

import com.swirlds.common.io.streams.MerkleDataInputStream;
import com.swirlds.common.io.streams.MerkleDataOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Utility class for generating paired streams for synchronization tests.
 */
public class PairedStreams implements AutoCloseable {

	protected BufferedOutputStream teacherOutputBuffer;
	protected MerkleDataOutputStream teacherOutput;

	protected BufferedInputStream teacherInputBuffer;
	protected MerkleDataInputStream teacherInput;

	protected BufferedOutputStream learnerOutputBuffer;
	protected MerkleDataOutputStream learnerOutput;
	protected BufferedInputStream learnerInputBuffer;
	protected MerkleDataInputStream learnerInput;

	protected Socket teacherSocket;
	protected Socket learnerSocket;
	protected ServerSocket server;

	public PairedStreams() throws IOException {

		server = new ServerSocket(0);
		teacherSocket = new Socket("127.0.0.1", server.getLocalPort());
		learnerSocket = server.accept();

		teacherOutputBuffer = new BufferedOutputStream(teacherSocket.getOutputStream());
		teacherOutput = new MerkleDataOutputStream(teacherOutputBuffer);

		teacherInputBuffer = new BufferedInputStream(teacherSocket.getInputStream());
		teacherInput = new MerkleDataInputStream(teacherInputBuffer);

		learnerOutputBuffer = new BufferedOutputStream(learnerSocket.getOutputStream());
		learnerOutput = new MerkleDataOutputStream(learnerOutputBuffer);

		learnerInputBuffer = new BufferedInputStream(learnerSocket.getInputStream());
		learnerInput = new MerkleDataInputStream(learnerInputBuffer);
	}

	public MerkleDataOutputStream getTeacherOutput() {
		return teacherOutput;
	}

	public MerkleDataInputStream getTeacherInput() {
		return teacherInput;
	}

	public MerkleDataOutputStream getLearnerOutput() {
		return learnerOutput;
	}

	public MerkleDataInputStream getLearnerInput() {
		return learnerInput;
	}

	@Override
	public void close() throws IOException {
		teacherOutput.close();
		teacherInput.close();
		learnerOutput.close();
		learnerInput.close();

		teacherOutputBuffer.close();
		teacherInputBuffer.close();
		learnerOutputBuffer.close();
		learnerInputBuffer.close();

		server.close();
		teacherSocket.close();
		learnerSocket.close();
	}

	/**
	 * Do an emergency shutdown of the sockets. Intentionally pulls the rug out from
	 * underneath all streams reading/writing the sockets.
	 */
	public void disconnect() throws IOException {
		server.close();
		teacherSocket.close();
		learnerSocket.close();
	}
}
