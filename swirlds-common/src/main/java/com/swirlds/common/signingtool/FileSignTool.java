/*
 * (c) 2016-2022 Swirlds, Inc.
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

package com.swirlds.common.signingtool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swirlds.common.CommonUtils;
import com.swirlds.common.constructable.ConstructableRegistry;
import com.swirlds.common.constructable.ConstructableRegistryException;
import com.swirlds.common.crypto.Hash;
import com.swirlds.common.crypto.SignatureType;
import com.swirlds.common.internal.SettingsCommon;
import com.swirlds.common.stream.InvalidStreamFileException;
import com.swirlds.common.stream.StreamType;
import com.swirlds.common.stream.StreamTypeFromJson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.swirlds.common.CommonUtils.hex;
import static com.swirlds.common.stream.EventStreamType.EVENT;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.computeEntireHash;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.computeMetaHash;
import static com.swirlds.common.stream.LinkedObjectStreamUtilities.readFirstIntFromFile;
import static com.swirlds.common.stream.TimestampStreamFileWriter.writeSignatureFile;

/**
 * This is a standalone utility tool to generate signature files for event/record stream,
 * and account balance files generated by stream server.
 *
 * It can also be used to sign any single files with any extension.
 * For files except .evts and .rcd,
 * the Hash in the signature file is a SHA384 hash of all bytes in the file to be signed.
 *
 * For .evts and .rcd files, it generate version 5 signature files.
 *
 * Please see README.md for format details
 */
public class FileSignTool {
	public static final String CSV_EXTENSION = ".csv";
	public static final String ACCOUNT_BALANCE_EXTENSION = ".pb";
	public static final String SIG_FILE_NAME_END = "_sig";
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker MARKER = MarkerManager.getMarker("FILE_SIGN");

	private static final int BYTES_COUNT_IN_INT = 4;

	/**
	 * next bytes are signature
	 */
	public static final byte TYPE_SIGNATURE = 3;
	/**
	 * next 48 bytes are hash384 of content of the file to be signed
	 */
	public static final byte TYPE_FILE_HASH = 4;
	/**
	 * default log4j2 file name
	 */
	private static final String DEFAULT_LOG_CONFIG = "log4j2.xml";
	/**
	 * supported stream version file
	 */
	private static final int VERSION_5 = 5;
	/**
	 * type of the keyStore
	 */
	private static final String KEYSTORE_TYPE = "pkcs12";
	/**
	 * name of RecordStreamType
	 */
	private static final String RECORD_STREAM_EXTENSION = "rcd";

	public static final String STREAM_TYPE_JSON_PROPERTY = "streamTypeJson";

	public static final String LOG_CONFIG_PROPERTY = "logConfig";

	public static final String FILE_NAME_PROPERTY = "fileName";

	public static final String KEY_PROPERTY = "key";

	public static final String DEST_DIR_PROPERTY = "destDir";

	public static final String ALIAS_PROPERTY = "alias";

	public static final String PASSWORD_PROPERTY = "password";

	public static final String DIR_PROPERTY = "dir";


	/**
	 * Digitally sign the data with the private key. Return null if anything goes wrong (e.g., bad private
	 * key).
	 * <p>
	 * The returned signature will be at most SIG_SIZE_BYTES bytes, which is 104 for the CNSA suite
	 * parameters.
	 *
	 * @param data
	 * 		the data to be signed
	 * @param sigKeyPair
	 * 		the keyPair used for signing
	 * @return the signature
	 * @throws NoSuchAlgorithmException
	 * 		if an implementation of the required algorithm cannot be located or loaded
	 * @throws NoSuchProviderException
	 * 		thrown if the specified provider is not registered in the security provider list
	 * @throws InvalidKeyException
	 * 		thrown if the key is invalid
	 * @throws SignatureException
	 * 		thrown if this signature object is not initialized properly
	 */
	public static byte[] sign(final byte[] data,
			final KeyPair sigKeyPair) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException,
			SignatureException {
		Signature signature;
		signature = Signature.getInstance(SignatureType.RSA.signingAlgorithm(), SignatureType.RSA.provider());
		signature.initSign(sigKeyPair.getPrivate());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(MARKER,
					"data is being signed, publicKey={}",
					hex(sigKeyPair.getPublic().getEncoded()));
		}

		signature.update(data);
		return signature.sign();
	}

	/**
	 * check whether the given signature is valid
	 *
	 * @param data
	 * 		the data that was signed
	 * @param signature
	 * 		the claimed signature of that data
	 * @param publicKey
	 * 		publicKey in the sign keyPair
	 * @param sigFilePath
	 * 		the signature file
	 * @return true if the signature is valid
	 */
	public static boolean verifySignature(final byte[] data, final byte[] signature,
			final PublicKey publicKey, final String sigFilePath) {
		try {
			Signature sig = Signature.getInstance(SignatureType.RSA.signingAlgorithm(), SignatureType.RSA.provider());
			sig.initVerify(publicKey);
			sig.update(data);
			return sig.verify(signature);
		} catch (NoSuchAlgorithmException | NoSuchProviderException |
				InvalidKeyException | SignatureException e) {
			LOGGER.error(MARKER, "Failed to verify Signature: {}, PublicKey: {}, File: {}",
					hex(signature),
					hex(publicKey.getEncoded()),
					sigFilePath,
					e);
		}
		return false;
	}

	/**
	 * Loads a pfx key file and return a KeyPair object
	 *
	 * @param keyFileName
	 * 		a pfx key file
	 * @param password
	 * 		password
	 * @param alias
	 * 		alias of the key
	 * @return a KeyPair
	 */
	public static KeyPair loadPfxKey(final String keyFileName, final String password, final String alias) {
		KeyPair sigKeyPair = null;
		try (FileInputStream fis = new FileInputStream(keyFileName)) {
			KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
			keyStore.load(fis, password.toCharArray());

			sigKeyPair = new KeyPair(
					keyStore.getCertificate(alias).getPublicKey(),
					(PrivateKey) keyStore.getKey(alias, password.toCharArray()));
			LOGGER.info(MARKER, "keypair has loaded successfully from file {}", keyFileName);
		} catch (NoSuchAlgorithmException | KeyStoreException |
				UnrecoverableKeyException | IOException | CertificateException e) {
			LOGGER.error(MARKER, "loadPfxKey :: ERROR ", e);
		}
		return sigKeyPair;
	}

	/**
	 * builds a signature file path from a destination directory and stream file name
	 *
	 * @param destDir
	 * 		the directory to which the signature file is saved
	 * @param streamFile
	 * 		stream file to be signed
	 * @return signature file path
	 */
	public static String buildDestSigFilePath(final File destDir, final File streamFile) {
		String sigFileName = streamFile.getName() + SIG_FILE_NAME_END;
		return new File(destDir, sigFileName).getPath();
	}

	/**
	 * creates a signature file for an event stream version3 file, or a record stream v2 file, or an account balance
	 * file
	 * This signature file contains the Hash of the file to be signed, and a signature signed by the node's Key
	 *
	 * @param filePath
	 * 		file name for the new signature file
	 * @param signature
	 * 		signature bytes generated for event file
	 * @param fileHash
	 * 		event file hash value
	 */
	public static void generateSigFileOldVersion(final String filePath, final byte[] signature, final byte[] fileHash) {
		try (FileOutputStream output = new FileOutputStream(filePath, false)) {
			output.write(TYPE_FILE_HASH);
			output.write(fileHash);
			output.write(TYPE_SIGNATURE);
			output.write(integerToBytes(signature.length));
			output.write(signature);
		} catch (IOException e) {
			LOGGER.error(MARKER,
					"generateSigFile :: Fail to generate signature file for {}. Exception: {}", filePath, e);
		}
		System.out.println("generate sig file: " + filePath);
	}

	/**
	 * convert an int to a byte array
	 *
	 * @param number
	 * 		an int number
	 * @return a byte array
	 */
	public static byte[] integerToBytes(final int number) {
		ByteBuffer b = ByteBuffer.allocate(BYTES_COUNT_IN_INT);
		b.putInt(number);
		return b.array();
	}

	/**
	 * generates signature file for the given stream file with the given KeyPair
	 * for event stream / record stream v5 file, generates v5 signature file which contains
	 * a EntireHash, a EntireSignature, a MetaHash, and a MetaSignature
	 * for other files, generate old signature file
	 *
	 * @param sigKeyPair
	 * 		the keyPair used for signing
	 * @param streamFile
	 * 		the stream file to be signed
	 * @param destDir
	 * 		the directory to which the signature file will be saved
	 * @param streamType
	 * 		type of the stream file
	 */
	public static void signSingleFile(final KeyPair sigKeyPair, final File streamFile,
			final File destDir, final StreamType streamType) {
		final String destSigFilePath = buildDestSigFilePath(destDir, streamFile);
		try {
			if (streamType.isStreamFile(streamFile)) {
				final int version = readFirstIntFromFile(streamFile);
				if (version != VERSION_5) {
					LOGGER.error(MARKER,
							"Failed to sign file {} with unsupported version {} ", streamFile.getName(), version);
					return;
				}

				// get entire Hash for this stream file
				Hash entireHash = computeEntireHash(streamFile);
				// get metaData Hash for this stream file
				Hash metaHash = computeMetaHash(streamFile, streamType);

				// generate signature for entire Hash
				com.swirlds.common.crypto.Signature entireSignature = new com.swirlds.common.crypto.Signature(
						SignatureType.RSA, sign(entireHash.getValue(), sigKeyPair));
				// generate signature for metaData Hash
				com.swirlds.common.crypto.Signature metaSignature = new com.swirlds.common.crypto.Signature(
						SignatureType.RSA, sign(metaHash.getValue(), sigKeyPair));
				writeSignatureFile(entireHash, entireSignature, metaHash, metaSignature,
						destSigFilePath, streamType);
			} else {
				signSingleFileOldVersion(sigKeyPair, streamFile, destSigFilePath);
			}
		} catch (NoSuchAlgorithmException | NoSuchProviderException |
				InvalidKeyException | SignatureException |
				InvalidStreamFileException | IOException e) {
			LOGGER.error(MARKER,
					"Failed to sign file {} ", streamFile.getName(), e);
		}
		LOGGER.info(MARKER, "Finish generating signature file {}", destSigFilePath);
	}

	/**
	 * generate old version signature file for a single file: for account balance files
	 *
	 * @param sigKeyPair
	 * 		keyPair used for signing
	 * @param streamFile
	 * 		stream file to be signed
	 * @param destSigFilePath
	 * 		path of the signature file
	 * @throws IOException
	 * 		thrown if fail to read the stream file
	 * @throws NoSuchAlgorithmException
	 * 		thrown if the specified algorithm is not available from the specified provider
	 * @throws NoSuchProviderException
	 * 		thrown if the specified provider is not registered in the security provider list
	 * @throws InvalidKeyException
	 * 		thrown if the key is invalid
	 * @throws SignatureException
	 * 		thrown if this signature object is not initialized properly
	 */
	public static void signSingleFileOldVersion(final KeyPair sigKeyPair, final File streamFile,
			final String destSigFilePath) throws IOException, NoSuchAlgorithmException, NoSuchProviderException,
			InvalidKeyException, SignatureException {
		byte[] fileHash = computeEntireHash(streamFile).getValue();
		byte[] signature = sign(fileHash, sigKeyPair);
		generateSigFileOldVersion(destSigFilePath, signature, fileHash);
	}

	/**
	 * Loads a StreamTypeFromJson object from a json file
	 *
	 * @param jsonPath
	 * 		path of the json file
	 * @return a StreamType object
	 * @throws IOException
	 * 		thrown if there are any problems during the operation
	 */
	public static StreamType loadStreamTypeFromJson(final String jsonPath) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();

		File file = new File(jsonPath);

		return objectMapper.readValue(file, StreamTypeFromJson.class);
	}

	public static void prepare(final StreamType streamType) throws ConstructableRegistryException {
		ConstructableRegistry.registerConstructables("com.swirlds.common");

		if (streamType.getExtension().equalsIgnoreCase(RECORD_STREAM_EXTENSION)) {
			LOGGER.info(MARKER,
					"registering Constructables for parsing record stream files");
			// if we are parsing new record stream files,
			// we need to add HederaNode.jar and hedera-protobuf-java-*.jar into class path,
			// so that we can register for parsing RecordStreamObject
			ConstructableRegistry.registerConstructables("com.hedera.services.stream");
		}

		// set the settings so that when deserialization we would not have transactionMaxBytes be 0
		// Todo: should remove these later when we refactor the Settings implementation
		SettingsCommon.maxTransactionCountPerEvent = 245760;
		SettingsCommon.maxTransactionBytesPerEvent = 245760;
		SettingsCommon.transactionMaxBytes = 6144;
		//set a relatively large value since the signing tool could not tell the address book size
		//or the number of nodes in the work
		SettingsCommon.maxAddressSizeAllowed = 1024;
	}

	public static void main(String[] args) {
		final String streamTypeJsonPath = System.getProperty(STREAM_TYPE_JSON_PROPERTY);
		// load StreamType from json file, if such json file doesn't exist, use EVENT as streamType
		StreamType streamType = EVENT;
		if (streamTypeJsonPath != null) {
			try {
				streamType = loadStreamTypeFromJson(streamTypeJsonPath);
			} catch (IOException e) {
				LOGGER.error(MARKER, "fail to load StreamType from {}.", streamTypeJsonPath, e);
				return;
			}
		}

		// register constructables and set settings
		try {
			prepare(streamType);
		} catch (ConstructableRegistryException e) {
			LOGGER.error(MARKER, "fail to register constructables.", e);
			return;
		}

		String logConfigPath = System.getProperty(LOG_CONFIG_PROPERTY);
		final File logConfigFile = logConfigPath == null ?
				CommonUtils.canonicalFile(".", DEFAULT_LOG_CONFIG) : new File(logConfigPath);
		if (logConfigFile.exists()) {
			LoggerContext context = (LoggerContext) LogManager
					.getContext(false);
			context.setConfigLocation(logConfigFile.toURI());
		}

		String fileName = System.getProperty(FILE_NAME_PROPERTY);
		String keyFileName = System.getProperty(KEY_PROPERTY);
		String destDirName = System.getProperty(DEST_DIR_PROPERTY);
		String alias = System.getProperty(ALIAS_PROPERTY);
		String password = System.getProperty(PASSWORD_PROPERTY);

		KeyPair sigKeyPair = loadPfxKey(keyFileName, password, alias);

		String fileDirName = System.getProperty(DIR_PROPERTY);

		try {
			// create directory if necessary
			final File destDir = new File(Files.createDirectories(Paths.get(destDirName)).toUri());

			if (fileDirName != null) {
				File folder = new File(fileDirName);
				final StreamType finalStreamType = streamType;
				File[] streamFiles = folder.listFiles((dir, name) -> finalStreamType.isStreamFile(name));
				File[] accountBalanceFiles = folder.listFiles(
						(dir, name) -> {
							String lowerCaseName = name.toLowerCase();
							return lowerCaseName.endsWith(CSV_EXTENSION) || lowerCaseName.endsWith(
									ACCOUNT_BALANCE_EXTENSION);
						});
				List<File> totalList = new ArrayList<>();
				totalList.addAll(Arrays.asList(streamFiles));
				totalList.addAll(Arrays.asList(accountBalanceFiles));
				for (File item : totalList) {
					signSingleFile(sigKeyPair, item, destDir, streamType);
				}
			} else {
				signSingleFile(sigKeyPair, new File(fileName), destDir, streamType);
			}
		} catch (IOException e) {
			LOGGER.error(MARKER, "Got IOException", e);
		}
	}
}
