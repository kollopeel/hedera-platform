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

package com.swirlds.common.test.stream;

import com.swirlds.common.constructable.ConstructableRegistry;
import com.swirlds.common.constructable.ConstructableRegistryException;
import com.swirlds.common.crypto.Hash;
import com.swirlds.common.stream.LinkedObjectStreamUtilities;
import com.swirlds.common.stream.LinkedObjectStreamValidateUtils;
import com.swirlds.common.stream.StreamType;
import com.swirlds.common.stream.StreamValidationResult;
import com.swirlds.common.test.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.swirlds.common.io.utility.FileUtils.deleteDirectory;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.generateStreamFileNameFromInstant;
import static com.swirlds.common.stream.StreamValidationResult.OK;
import static com.swirlds.common.stream.StreamValidationResult.SIG_FILE_COUNT_MISMATCH;
import static com.swirlds.common.stream.StreamValidationResult.START_HASH_NOT_MATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamObjectTest {
	static {
		System.setProperty("log4j.configurationFile", "log4j2ForTest.xml");
	}

	private static final Logger log = LogManager.getLogger();
	private static final Marker LOGM_EXCEPTION = MarkerManager.getMarker("EXCEPTION");
	private static final String dirPath = "src/test/resources/stream/writeDir";
	private static final StreamType streamType = TestStreamType.TEST_STREAM;
	/**
	 * number of objects to be generated
	 */
	final int objectsTotalNum = 100;
	/**
	 * interval in ms between each two adjacent objects to be generated
	 */
	final int intervalMs = 1;
	/**
	 * period of generating object stream files in ms
	 */
	final int logPeriodMs = 20;
	static Hash initialHash = RandomUtils.randomHash();

	static StreamFileSigner signer = new StreamFileSigner();
	static PublicKey publicKey = signer.getPublicKey();

	@BeforeAll
	static void setUp() throws ConstructableRegistryException, IOException {
		ConstructableRegistry.registerConstructables("com.swirlds.common");
	}

	@Test
	void generateFileRestartTest() throws InterruptedException, NoSuchAlgorithmException, IOException {
		// make dir if it doesn't exist
		Files.createDirectories(Paths.get(dirPath)).toUri();
		Instant firstTimestamp = Instant.now();
		StreamObjectWorker worker = new StreamObjectWorker(objectsTotalNum,
				intervalMs, dirPath, logPeriodMs,
				initialHash, false,
				firstTimestamp, signer);
		worker.work();
		// expected name of the file which contains the first object
		String expectedFirstFileName = generateStreamFileNameFromInstant(firstTimestamp, streamType);
		// there should be a file matches this name
		assertTrue(dirContainsFile(expectedFirstFileName));

		// validate files
		assertEquals(OK, verifyDirectory(true));

		// clear directory
		clearDir();
	}

	@Test
	void generateFileReconnectTest() throws InterruptedException, NoSuchAlgorithmException, IOException {
		// make dir if it doesn't exist
		Files.createDirectories(Paths.get(dirPath)).toUri();
		Instant firstTimestamp = Instant.now();
		StreamObjectWorker worker = new StreamObjectWorker(objectsTotalNum,
				intervalMs, dirPath, logPeriodMs,
				initialHash, true, firstTimestamp, signer);
		worker.work();
		// name of the file which contains the first object
		String fileContainsFirstObject = generateStreamFileNameFromInstant(firstTimestamp, streamType);
		// there should not be such a file
		assertFalse(dirContainsFile(fileContainsFirstObject));
		// validate files
		assertEquals(OK, verifyDirectory(false));
		// clear directory
		clearDir();
	}

	static void clearDir() throws IOException {
		// delete the dir if exists
		final Path dir = new File(dirPath).toPath();
		deleteDirectory(dir);
	}

	/**
	 * verify if generated stream file and signatures are valid
	 */
	static StreamValidationResult verifyDirectory(final boolean restart) {
		List<File> streamFiles = new ArrayList<>();
		List<File> sigFiles = new ArrayList<>();
		File dir = new File(dirPath);
		// put stream files and sig files to separate lists
		Arrays.stream(dir.listFiles())
				.sorted(Comparator.comparing(File::getName))
				.forEach((file) -> {
					if (streamType.isStreamFile(file)) {
						streamFiles.add(file);
					} else if (streamType.isStreamSigFile(file.getName())) {
						sigFiles.add(file);
					}
				});
		System.out.println(Arrays.asList(streamFiles));
		System.out.println(Arrays.asList(sigFiles));
		if (streamFiles.size() != sigFiles.size()) {
			log.error(LOGM_EXCEPTION, "streamFiles size: {} doesn't match sigFiles size: {}",
					streamFiles.size(), sigFiles.size());
			return SIG_FILE_COUNT_MISMATCH;
		}

		if (restart) {
			// check if startRunningHash read from the first stream file matches expected Hash
			Hash startRunningHash = LinkedObjectStreamUtilities.readStartRunningHashFromStreamFile(streamFiles.get(0),
					streamType);
			if (!startRunningHash.equals(initialHash)) {
				log.error(LOGM_EXCEPTION, "the first stream file {} its startRunningHash {} doesn't match expected: {}",
						streamFiles.get(0).getName(), startRunningHash, initialHash);
				return START_HASH_NOT_MATCH;
			}
		}

		for (int i = 0; i < streamFiles.size(); i++) {
			StreamValidationResult result = LinkedObjectStreamValidateUtils.validateFileAndSignature(streamFiles.get(i),
					sigFiles.get(i),
					publicKey,
					streamType);
			if (result != OK) {
				log.error(LOGM_EXCEPTION, "streamFile: {}, sigFile: {}, validateResult: {}",
						streamFiles.get(i), sigFiles.get(i), result);
				return result;
			}
		}
		return OK;
	}

	/**
	 * check if any file in the dirPath matches the fileName
	 */
	boolean dirContainsFile(String fileName) {
		File dir = new File(dirPath);
		return Arrays.stream(dir.listFiles())
				.filter((f) -> f.getName().equals(fileName))
				.count() > 0;
	}
}
