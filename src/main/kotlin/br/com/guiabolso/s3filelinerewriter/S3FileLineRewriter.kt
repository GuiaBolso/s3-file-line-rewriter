/*
 *    Copyright 2020 Guiabolso
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package br.com.guiabolso.s3filelinerewriter

import br.com.guiabolso.s3filelinerewriter.internal.REMOVE_EMPTY_LINES
import br.com.guiabolso.s3filelinerewriter.internal.S3FileUploader
import br.com.guiabolso.s3filelinerewriter.internal.withSeparatingNewlines
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.amazonaws.services.s3.model.S3Object


public class S3FileLineRewriter(
    private val s3Client: AmazonS3
) {
    public fun rewriteAll(bucket: String, prefix: String, transform: (String) -> String) {
        validateRequirementsAll(bucket, prefix)

        listFiles(bucket, prefix).forEach { rewriteFile(bucket, it, transform) }
    }

    private fun validateRequirementsAll(bucket: String, prefix: String) {
        require(bucket.isNotEmpty()) { "Bucket must be non-empty string, but was." }
        require(prefix.isNotEmpty()) { "Prefix must be non-empty string, but was." }
    }

    private fun listFiles(bucket: String, prefix: String, token: String? = null): List<String> {
        val request = listObjecstV2Request(bucket, prefix, token)

        val result = s3Client.listObjectsV2(request)
        val keys = result.objectSummaries.map { it.key }

        return if (!result.isTruncated) keys else keys + listFiles(bucket, prefix, result.nextContinuationToken)
    }

    public fun rewriteFile(bucket: String, key: String, transform: (String) -> String) {
        validateRequrimentsIndividual(bucket, key)

        using(bucket, key) { lines, s3Object ->
            val newLines = lines.map(transform).withNoEmptyLines().withSeparatingNewlines()

            S3FileUploader(s3Object, s3Client).upload(newLines)
        }
    }

    private fun validateRequrimentsIndividual(bucket: String, key: String) {
        require(bucket.isNotEmpty()) { "Bucket must be non-empty string, but was." }
        require(key.isNotEmpty()) { "Key must be non-empty string, but was." }
    }

    private fun using(bucket: String, key: String, block: (Sequence<String>, S3Object) -> Unit) {
        val s3Object = s3Client.getObject(bucket, key)
        s3Object.objectContent.bufferedReader().useLines { block(it, s3Object) }
    }

    private fun Sequence<String>.withNoEmptyLines() =
        if (REMOVE_EMPTY_LINES) filterNot { it.isBlank() } else this
}

private fun listObjecstV2Request(bucket: String, prefix: String, token: String?): ListObjectsV2Request =
    ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix).withContinuationToken(token)

