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

package com.swirlds.common.stream;

/**
 * Result of validating stream files and stream signature files
 */
public enum StreamValidationResult {
	/**
	 * Fail to parse stream file
	 */
	PARSE_STREAM_FILE_FAIL,
	/**
	 * stream file doesn't contain any content
	 */
	STREAM_FILE_EMPTY,
	/**
	 * stream file doesn't contain startRunningHash
	 */
	STREAM_FILE_MISS_START_HASH,
	/**
	 * stream file doesn't contain objects
	 */
	STREAM_FILE_MISS_OBJECTS,
	/**
	 * stream file doesn't contain endRunningHash
	 */
	STREAM_FILE_MISS_END_HASH,
	/**
	 * endRunningHash in the stream file doesn't match the calculated result from startRunningHash and objects
	 */
	CALCULATED_END_HASH_NOT_MATCH,

	/**
	 * fail to parse signature file
	 */
	PARSE_SIG_FILE_FAIL,
	/**
	 * signature bytes in the signature file doesn't match endRunningHash in the stream file
	 */
	SIG_NOT_MATCH_FILE,
	/**
	 * metaSignature in the signature file doesn't match metaHash
	 */
	INVALID_META_SIGNATURE,
	/**
	 * entireSignature in the signature file doesn't match entireHash
	 */
	INVALID_ENTIRE_SIGNATURE,
	/**
	 * entireHash saved in the signature file doesn't match the entireHash of the stream file
	 */
	SIG_HASH_NOT_MATCH_FILE,
	/**
	 * number of signature files is not equal to number of stream files
	 */
	SIG_FILE_COUNT_MISMATCH,
	/**
	 * startRunningHash read from stream file doesn't match expected Hash
	 */
	START_HASH_NOT_MATCH,
	/**
	 * pass validation
	 */
	OK,
	/**
	 * there is no file exists for validation
	 */
	NO_FILE_EXISTS,
	/**
	 * prevFileHash read from stream file doesn't match the hash calculated from the previous file
	 */
	PREV_FILE_HASH_NOT_MATCH,
	/**
	 * fail to calculate entire hash for the stream file
	 */
	FAIL_TO_CALCULATE_ENTIRE_HASH
}
