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

import br.com.guiabolso.s3filelinerewriter.internal.S3ExtensionListener
import com.amazonaws.services.s3.model.GetObjectTaggingRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.ObjectTagging
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.SetObjectTaggingRequest
import com.amazonaws.services.s3.model.Tag
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage

@ExperimentalStdlibApi
class S3FileLineRewriterTest : FunSpec() {

    private val s3Client by lazy { s3Listener.client("bucket") }

    init {
        test("Rewriting a single line with intended modification") {
            createFile("abc\nXXX\n123")

            rewriteLines { seq -> 
                seq.map { it.replace("XXX", "YYY") }
            }

            getFile().contentString shouldBe "abc\nYYY\n123"
        }
        
        test("Rewriting multiple lines with intended modification") {
            createFile("abc\nXXX\nXXX\n123")
            
            rewriteLines { seq ->
                seq.map { it.replace("XXX", "YYY") }
            }
            
            getFile().contentString shouldBe "abc\nYYY\nYYY\n123"
        }
        
        test("Rewriting should preserve metadata") {
           createFile("A", ObjectMetadata().also { it.addUserMetadata("a", "b") })

            rewriteLines { it }
            
            getFile().objectMetadata.getUserMetaDataOf("a") shouldBe "b"
        }
        
        test("Rewriting should preserve tags") {
            createFile("A", ObjectMetadata(), listOf(Tag("a", "b"), Tag("C", "D")))

            rewriteLines { it }
            
            getFileTags().shouldContainAll(Tag("a", "b"), Tag("C", "D"))
        }
        
        test("Rewriting should remove a line if it's empty") {
            createFile("A\nB\nC")

            rewriteLines { seq ->
                seq.map { it.replace("B", "") } 
            }
            
            getFile().contentString shouldBe "A\nC"
        }
        
        test("Should run (and do nothing) in an empty file") {
            createFile("")
            
            rewriteLines { seq -> 
                seq.map { it.replace("Foo", "BAR") } 
            }
            
            getFile().contentString shouldBe ""
        }
        
        test("Should allow rewriting of more than one file") {
            // Need >= 1000 objects to ensure we make enough requests for all of them
            repeat(501) { 
                createFile("A\nB\nC", key="$it", directory = "dir/subdir/")
                createFile("A\nB\nC", key = "$it", directory = "dir/subdir/otherdir/")
            }
            
            rewriteAll("dir/subdir/") { seq ->
                seq.map { it.replace("B", "") }
            }
            
            repeat(501) { 
                getFile("dir/subdir/$it").contentString shouldBe "A\nC"
                getFile("dir/subdir/otherdir/$it").contentString shouldBe "A\nC"
            }
        }
        
        test("Should throw an error when trying to rewrite with empty parameters") {
            val rewriter = S3FileLineRewriter(s3Client)
                        
            shouldThrow<IllegalArgumentException> { 
                rewriter.rewriteAll("", "nonEmpty") { it }
            }.shouldHaveMessage("Bucket must be non-empty string, but was.")
            
            shouldThrow<IllegalArgumentException> { 
                rewriter.rewriteAll("nonEmpty", "") { it }
            }.shouldHaveMessage("Prefix must be non-empty string, but was.")
            
            shouldThrow<IllegalArgumentException> { 
                rewriter.rewriteFile("", "nonEmpty") { it } 
            }.shouldHaveMessage("Bucket must be non-empty string, but was.")
            
            shouldThrow<IllegalArgumentException> { 
                rewriter.rewriteFile("nonEmpty", "") { it } 
            }.shouldHaveMessage("Key must be non-empty string, but was.")
        }

        test("Should allow rewriting line sequences") {
            createFile("abc\nXXX\nXXX\n123")

            rewriteLines { seq: Sequence<String> ->
                seq.map { it.replace("XXX", "YYY") }
            }

            getFile().contentString shouldBe "abc\nYYY\nYYY\n123"
        }
    }
    
    private val S3Object.contentString get() = objectContent.readBytes().decodeToString()

    @JvmName("rewriteLinesSequence") 
    private fun rewriteLines(change: (Sequence<String>) -> Sequence<String>) =
        S3FileLineRewriter(s3Client).rewriteFile("bucket", "key", change)
    
    private fun rewriteAll(directory: String, change: (Sequence<String>) -> Sequence<String>) =
        S3FileLineRewriter(s3Client).rewriteAll("bucket", directory, change)
    
    private fun createFile(
        content: String,
        metadata: ObjectMetadata = ObjectMetadata(),
        tags: List<Tag> = emptyList(),
        key: String = "key",
        directory: String = ""
    ) {
        s3Client.putObject("bucket", directory + key, content.byteInputStream(), metadata)
        s3Client.setObjectTagging(SetObjectTaggingRequest("bucket", directory + key, ObjectTagging(tags)))
    }

    private fun getFile(key: String = "key")= s3Client.getObject("bucket", key)
    private fun getFileTags() = s3Client.getObjectTagging(GetObjectTaggingRequest("bucket", "key")).tagSet

    private val s3Listener = S3ExtensionListener()
    override fun listeners() = listOf(s3Listener)
}
