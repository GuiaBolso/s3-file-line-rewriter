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

package br.com.guiabolso.s3filelinerewriter.internal

import alex.mojaki.s3upload.StreamTransferManager
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GetObjectTaggingRequest
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest
import com.amazonaws.services.s3.model.ObjectTagging
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.SetObjectTaggingRequest

internal class S3FileUploader(
    private val originalObject: S3Object,
    s3Client: AmazonS3
) : StreamTransferManager(originalObject.bucketName, originalObject.key, s3Client) {

    private val originalTags = originalObject.tags
    private val outputStream = multiPartOutputStreams.single()

    override fun customiseInitiateRequest(request: InitiateMultipartUploadRequest) {
        request.setObjectMetadata(originalObject.objectMetadata)
    }

    fun upload(lineSequence: Sequence<String>) {
        outputStream.use {
            lineSequence.forEach {
                outputStream.write(it.toByteArray())
            }
        }
        complete()
        s3Client.setObjectTagging(SetObjectTaggingRequest(originalObject.bucketName, originalObject.key, originalTags))
    }


    private val S3Object.tags
        get() = ObjectTagging(
            s3Client.getObjectTagging(GetObjectTaggingRequest(bucketName, key)).tagSet
        )
}
