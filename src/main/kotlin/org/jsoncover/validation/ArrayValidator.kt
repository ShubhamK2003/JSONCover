/*
 * @(#) ArrayValidator.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jsoncover.validation

import java.net.URI

import net.pwall.json.JSONSequence
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import org.jsoncover.JSONSchema
import org.jsoncover.output.BasicErrorEntry

class ArrayValidator(uri: URI?, location: JSONPointer, val condition: ValidationType, val value: Int) :
        org.jsoncover.JSONSchema.Validator(uri, location) {

    enum class ValidationType(val keyword: String) {
        MAX_ITEMS("maxItems"),
        MIN_ITEMS("minItems")
    }

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child(condition.keyword)

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        return instance !is JSONSequence<*> || validNumberOfItems(instance)
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = instanceLocation.eval(json)
        return if (instance !is JSONSequence<*> || validNumberOfItems(instance)) null else
                createBasicErrorEntry(relativeLocation, instanceLocation,
                        "Array fails number of items check: ${condition.keyword} $value, was ${instance.size}")
    }

    private fun validNumberOfItems(instance: JSONSequence<*>): Boolean = when (condition) {
        ValidationType.MAX_ITEMS -> instance.size <= value
        ValidationType.MIN_ITEMS -> instance.size >= value
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is ArrayValidator && super.equals(other) && condition == other.condition && value == other.value

    override fun hashCode(): Int = super.hashCode() xor condition.hashCode() xor value.hashCode()

}