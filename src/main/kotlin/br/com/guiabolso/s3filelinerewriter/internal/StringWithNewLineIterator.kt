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

internal fun Sequence<String>.withSeparatingNewlines() = StringWithNewLineIterator(this).asSequence()

@Suppress("IteratorNotThrowingNoSuchElementException")
private class StringWithNewLineIterator(
    sequence: Sequence<String>
) : Iterator<String> {

    private val iterator = sequence.iterator()

    override fun hasNext() = iterator.hasNext()

    override fun next(): String {
        val next = iterator.next()
        return next + if (hasNext()) NEW_LINE else BLANK
    }
}

private const val BLANK = ""
