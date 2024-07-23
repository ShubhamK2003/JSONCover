package parser

import net.pwall.json.*
import java.io.File
import java.net.URI
import net.pwall.json.schema.*
import net.pwall.json.pointer.*
import net.pwall.json.schema.JSONSchema.Companion.booleanSchema
import net.pwall.json.schema.JSONSchema.Companion.toErrorDisplay
import net.pwall.json.schema.parser.JSONReader
import net.pwall.json.schema.parser.Parser.Companion.defaultURIResolver
import net.pwall.json.schema.parser.Parser.Companion.dropFragment
import net.pwall.json.schema.parser.Parser.Companion.getIdOrNull
import net.pwall.json.schema.parser.Parser.Companion.getInteger
import net.pwall.json.schema.parser.Parser.Companion.getNonNegativeInteger
import net.pwall.json.schema.parser.Parser.Companion.getStringOrNull
import net.pwall.json.schema.parser.Parser.Companion.isPositive
import net.pwall.json.schema.parser.Parser.Companion.withFragment
import net.pwall.json.schema.subschema.*
import net.pwall.json.schema.validation.*
import coverage.LineCoverage


object Parser {

    private val jsonReader = JSONReader(defaultURIResolver)

    fun parseFile(schemaFilename: String, payloadFilename: String): JSONSchema {
        val schemaFile = File(schemaFilename)
        if (!schemaFile.isFile)
            throw JSONSchemaException("Invalid schema file - $schemaFile")
        val uri = schemaFile.toURI()
        val schemaJson = jsonReader.readJSON(schemaFile)
        val pointer = JSONPointer.root
        val schema = parseSchema(schemaJson, pointer, uri)

        val payloadFile = File(payloadFilename)
        if (!payloadFile.isFile)
            throw JSONSchemaException("Invalid payload file - $payloadFile")
        val payloadJson = jsonReader.readJSON(payloadFile)

        checkUnvalidatedProperties(schema, payloadJson)

        return schema
    }

    private val anchors = mutableMapOf<String, JSONPointer>()
    private fun parseSchema(json: JSONValue, pointer: JSONPointer, parentUri: URI?): JSONSchema {
        val schemaJSON = pointer.eval(json)
        if (schemaJSON is JSONBoolean)
            return booleanSchema(schemaJSON.booleanValue(), parentUri, pointer)
        if (schemaJSON !is JSONMapping<*>)
            throw JSONSchemaException("Schema is not boolean or object - ${errorPointer(parentUri, pointer)}")
        val id = getIdOrNull(schemaJSON)
        val uri = when {
            id == null -> parentUri
            parentUri == null -> URI(id).dropFragment()
            else -> parentUri.resolve(id).dropFragment()
        }

        val title = schemaJSON.getStringOrNull(uri, "title")
        val description = getDescription(schemaJSON, uri, pointer)

        val children = mutableListOf<JSONSchema>()
        val result = JSONSchema.General("", title, description, uri, pointer, children)

        for ((key, value) in schemaJSON.entries) {
            val childPointer = pointer.child(key)
            when (key) {
                "\$schema" -> {
                    if (pointer != JSONPointer.root)
                        fatal("May only appear in the root of the document", uri, childPointer)
                    if (value !is JSONString)
                        fatal("String expected", uri, childPointer)
                }
                "\$id", "title", "description", "\$comment", "example", "examples" -> {}
                "\$defs" -> {
                    parseDefs(childPointer, uri, value)
                }
                "\$ref" -> {
                    children.add(parseRef(json, childPointer, uri, value))

                }
                "default" -> {
                    children.add(DefaultValidator(uri, childPointer, value))
                }
                "allOf" -> {
                    children.add(parseCombinationSchema(json, childPointer, uri, value, JSONSchema.Companion::allOf))
                    LineCoverage.incrementTotalConstraints()
                }
                "anyOf" -> {
                    children.add(parseCombinationSchema(json, childPointer, uri, value, JSONSchema.Companion::anyOf))
                    LineCoverage.incrementTotalConstraints()
                }
                "oneOf" -> {
                    children.add(parseCombinationSchema(json, childPointer, uri, value, JSONSchema.Companion::oneOf))
                    LineCoverage.incrementTotalConstraints()
                }
                "not" -> {
                    children.add(JSONSchema.Not(uri, childPointer, parseSchema(json, pointer.child("not"), uri)))
                    LineCoverage.incrementTotalConstraints()
                }
                "if" -> {
                    children.add(parseIf(json, pointer, uri))
                    LineCoverage.incrementTotalConstraints()
                }
                "type" -> {
                    children.add(parseType(childPointer, uri, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "enum" -> {
                    children.add(parseEnum(childPointer, uri, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "const" -> {
                    children.add(ConstValidator(uri, childPointer, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "properties" -> {
                    children.add(parseProperties(json, childPointer, uri, value))
                }
                "patternProperties" -> {
                    children.add(parsePatternProperties(json, childPointer, uri, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "propertyNames" -> {
                    children.add(parsePropertyNames(json, childPointer, uri))
                    LineCoverage.incrementTotalConstraints()
                }
                "minProperties" -> {
                    children.add(parsePropertiesSize(childPointer, uri, PropertiesValidator.ValidationType.MIN_PROPERTIES, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "maxProperties" -> {
                    children.add(parsePropertiesSize(childPointer, uri, PropertiesValidator.ValidationType.MAX_PROPERTIES, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "required" -> {
                    children.add(parseRequired(childPointer, uri, value))
                }
                "items" -> {
                    children.add(parseItems(json, childPointer, uri, value))
                }
                in NumberValidator.typeKeywords -> {
                    children.add(parseNumberLimit(childPointer, uri, NumberValidator.findType(key), value))
                    LineCoverage.incrementTotalConstraints()
                }
                "maxItems" -> {
                    children.add(parseArrayNumberOfItems(childPointer, uri, ArrayValidator.ValidationType.MAX_ITEMS, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "minItems" -> {
                    children.add(parseArrayNumberOfItems(childPointer, uri, ArrayValidator.ValidationType.MIN_ITEMS, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "uniqueItems" -> {
                    parseArrayUniqueItems(childPointer, uri, value)?.let {
                        children.add(it)
                        LineCoverage.incrementTotalConstraints()
                    }
                }
                "maxLength" -> {
                    children.add(parseStringLength(childPointer, uri, StringValidator.ValidationType.MAX_LENGTH, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "minLength" -> {
                    children.add(parseStringLength(childPointer, uri, StringValidator.ValidationType.MIN_LENGTH, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "pattern" -> {
                    children.add(parsePattern(childPointer, uri, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "format" -> {
                    children.add(parseFormat(childPointer, uri, value))
                    LineCoverage.incrementTotalConstraints()
                }
                "additionalProperties" -> {
                    children.add(AdditionalPropertiesSchema(result, uri, childPointer, parseSchema(json, childPointer, uri)))
                    LineCoverage.incrementTotalConstraints()
                }
                "additionalItems" -> {
                    children.add(AdditionalItemsSchema(result, uri, childPointer, parseSchema(json, childPointer, uri)))
                    LineCoverage.incrementTotalConstraints()
                }
                "contains" -> {
                    children.add(parseContains(json, pointer, uri))
                    LineCoverage.incrementTotalConstraints()
                }
            }
        }
        return result
    }

    private fun parseDefs(pointer: JSONPointer, uri: URI?, defs: JSONValue) {
        if (defs !is JSONMapping<*>)
            fatal("\$defs must be an object", uri, pointer)
        for ((key, value) in defs.entries) {
            val childPointer = pointer.child(key)
            if (value is JSONMapping<*>) {
                val anchor = value["\$anchor"]
                if (anchor is JSONString) {
                    anchors[anchor.value] = childPointer
                }
                else fatal("Anchor must be a string", uri, pointer)
            }
        }
    }

    private fun getDescription(schemaJSON: JSONMapping<*>, uri: URI?, pointer: JSONPointer): String? {
        val value = schemaJSON["description"] ?: return null
        if (value is JSONString)
            return value.value
        fatal("Invalid description", uri, pointer)
    }

    private fun parseIf(json: JSONValue, pointer: JSONPointer, uri: URI?): JSONSchema {
        val ifSchema = parseSchema(json, pointer.child("if"), uri)
        val thenSchema = pointer.child("then").let { if (it.exists(json)) parseSchema(json, it, uri) else null }
        val elseSchema = pointer.child("else").let { if (it.exists(json)) parseSchema(json, it, uri) else null }
        return IfThenElseSchema(uri, pointer, ifSchema, thenSchema, elseSchema)
    }

    private fun parseContains(json: JSONValue, pointer: JSONPointer, uri: URI?): ContainsValidator {
        val containsSchema = parseSchema(json, pointer.child("contains"), uri)
        val minContains = pointer.child("minContains").let {
            if (it.exists(json)) getNonNegativeInteger(json, uri, it) else null
        }
        val maxContains = pointer.child("maxContains").let {
            if (it.exists(json)) getNonNegativeInteger(json, uri, it) else null
        }
        return ContainsValidator(uri, pointer.child("contains"), containsSchema, minContains, maxContains)
    }

    private fun parseFormat(pointer: JSONPointer, uri: URI?, value: JSONValue?): JSONSchema.Validator {
        if (value !is JSONString)
            fatal("String expected", uri, pointer)
        value.value.let { keyword ->
            var nonstandardFormatHandler: (String) -> FormatValidator.FormatChecker? = { _ -> null }
            val checker = nonstandardFormatHandler(keyword) ?: FormatValidator.findChecker(keyword) ?:
            FormatValidator.NullFormatChecker(keyword)
            return FormatValidator(uri, pointer, checker)
        }
    }

    private fun parseCombinationSchema(json: JSONValue, pointer: JSONPointer, uri: URI?, array: JSONValue?,
                                       creator: (URI?, JSONPointer, List<JSONSchema>) -> JSONSchema
    ): JSONSchema {
        if (array !is JSONSequence<*>)
            fatal("Compound must take array", uri, pointer)
        return creator(uri, pointer, array.mapIndexed { i, _ -> parseSchema(json, pointer.child(i), uri) })
    }

    private fun parseRef(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?): RefSchema {
        if (value !is JSONString)
            fatal("\$ref must be string", uri, pointer)

        val refString = value.value
        val refURI: URI?
        val refJSON: JSONValue
        val refURIFragment: String?
        val refPointer: JSONPointer

        val hashIndex = refString.indexOf('#')
        when {
            hashIndex < 0 -> {
                // No fragment
                refURI = uri?.resolve(refString) ?: URI(refString)
                refJSON = jsonReader.readJSON(refURI)
                refURIFragment = null
                refPointer = JSONPointer.root
            }
            hashIndex == 0 -> {
                // Fragment only
                refURI = uri
                refJSON = json
                refURIFragment = refString.substring(1)
                refPointer = if (refURIFragment.startsWith("/\$defs/")) {
                    JSONPointer.fromURIFragment(refString)
                } else {
                    anchors[refURIFragment] ?: JSONPointer.fromURIFragment("#/\$defs/$refURIFragment")
                }
            }
            else -> {
                // URI with fragment
                val refStringWithoutFragment = refString.substring(0, hashIndex)
                if (uri != null && uri.toString() == refStringWithoutFragment) { // same base URI?
                    refURI = uri
                    refJSON = json
                } else {
                    refURI = uri?.resolve(refStringWithoutFragment) ?: URI(refStringWithoutFragment)
                    refJSON = jsonReader.readJSON(refURI)
                }
                refURIFragment = refString.substring(hashIndex + 1)
                refPointer = if (refURIFragment.startsWith("/\$defs/")) {
                    JSONPointer.fromURIFragment(refString)
                } else {
                    anchors[refURIFragment] ?: JSONPointer.fromURIFragment("#/\$defs/$refURIFragment")
                }
            }
        }

        if (!refPointer.exists(refJSON))
            fatal("\$ref not found $refString", uri, pointer)

        return RefSchema(
            uri = uri,
            location = pointer,
            target = parseSchema(refJSON, refPointer, refURI),
            fragment = refURIFragment
        )
    }



    private fun parseItems(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?): JSONSchema.SubSchema {
        return if (value is JSONSequence<*>)
            ItemsArraySchema(uri, pointer, value.mapIndexed { i, _ -> parseSchema(json, pointer.child(i), uri) })
        else
            ItemsSchema(uri, pointer, parseSchema(json, pointer, uri))
    }

    private fun parseProperties(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?): PropertiesSchema {
        if (value !is JSONMapping<*>)
            fatal("properties must be object", uri, pointer)
        return PropertiesSchema(uri, pointer, value.keys.map { it to parseSchema(json, pointer.child(it), uri) })
    }


    private fun checkUnvalidatedProperties(schema: JSONSchema, data: JSONValue?, path: String = "") {
        when (schema) {
            is JSONSchema.General -> {
                for (child in schema.children) {
                    checkUnvalidatedProperties(child, data, path)
                }
            }
            is PropertiesSchema -> {
                if (data is JSONMapping<*>) {
                    for ((propName, propSchema) in schema.properties) {
                        val fullPath = if (path.isEmpty()) propName else "$path.$propName"
                        if (data.containsKey(propName)) {
                            checkUnvalidatedProperties(propSchema, data[propName], fullPath)
                        } else {
                            LineCoverage.incrementUnvalidatedConstraints()
                            println("Incrementing unvalidated constraint for: $fullPath")
                        }
                    }
                } else {
                    println("Data is not a JSONMapping at $path")
                }
            }
            is RefSchema -> {
                checkUnvalidatedProperties(schema.target, data, path)
            }
            else -> {
            }
        }
    }

    private fun parsePatternProperties(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?):
            PatternPropertiesSchema {
        if (value !is JSONMapping<*>)
            fatal("patternProperties must be object", uri, pointer)
        return PatternPropertiesSchema(uri, pointer, value.keys.map { key ->
            val childPointer = pointer.child(key)
            val regex = try { Regex(key) } catch (e: Exception) {
                fatal("Invalid regex in patternProperties", uri, childPointer) }
            regex to parseSchema(json, childPointer, uri)
        })
    }

    private fun parsePropertyNames(json: JSONValue, pointer: JSONPointer, uri: URI?): PropertyNamesSchema {
        return PropertyNamesSchema(uri, pointer, parseSchema(json, pointer, uri))
    }

    private fun parsePropertiesSize(pointer: JSONPointer, uri: URI?, condition: PropertiesValidator.ValidationType,
                                    value: JSONValue?): PropertiesValidator {
        return PropertiesValidator(uri, pointer, condition, getInteger(value, uri, pointer))
    }

    private fun parseRequired(pointer: JSONPointer, uri: URI?, value: JSONValue?): RequiredSchema {
        if (value !is JSONSequence<*>)
            fatal("required must be array", uri, pointer)
        LineCoverage.incrementTotalConstraints(value.size)
        return RequiredSchema(uri, pointer, value.mapIndexed { i, entry ->
            if (entry !is JSONString)
                fatal("required item must be string", uri, pointer.child(i))
            entry.value
        })
    }

    private fun parseType(pointer: JSONPointer, uri: URI?, value: JSONValue?): TypeValidator {
        val types: List<JSONSchema.Type> = when (value) {
            is JSONString -> listOf(checkType(value, pointer, uri))
            is JSONSequence<*> -> value.mapIndexed { index, item -> checkType(item, pointer.child(index), uri) }
            else -> fatal("Invalid type", uri, pointer)
        }
        return TypeValidator(uri, pointer, types)
    }

    private fun checkType(item: JSONValue?, pointer: JSONPointer, uri: URI?): JSONSchema.Type {
        if (item is JSONString) {
            for (type in JSONSchema.Type.entries) {
                if (item.value == type.value)
                    return type
            }
        }
        fatal("Invalid type", uri, pointer)
    }

    private fun parseEnum(pointer: JSONPointer, uri: URI?, value: JSONValue?): EnumValidator {
        if (value !is JSONSequence<*>)
            fatal("enum must be array", uri, pointer)
        return EnumValidator(uri, pointer, value)
    }

    private fun parseNumberLimit(pointer: JSONPointer, uri: URI?, condition: NumberValidator.ValidationType,
                                 value: JSONValue?): NumberValidator {
        if (value !is JSONNumberValue)
            fatal("Must be number (was ${value.toErrorDisplay()})", uri, pointer)
        val number: Number = when (value) {
            is JSONDouble, // should not happen
            is JSONFloat,
            is JSONDecimal -> value.bigDecimalValue()
            is JSONLong -> value.toLong()
            else -> value.toInt() // includes JSONInteger, JSONZero
        }
        if (condition == NumberValidator.ValidationType.MULTIPLE_OF && !number.isPositive())
            fatal("multipleOf must be greater than 0", uri, pointer)
        return NumberValidator(uri, pointer, number, condition)
    }

    private fun parseStringLength(pointer: JSONPointer, uri: URI?, condition: StringValidator.ValidationType,
                                  value: JSONValue?): StringValidator {
        return StringValidator(uri, pointer, condition, getInteger(value, uri, pointer))
    }

    private fun parseArrayNumberOfItems(pointer: JSONPointer, uri: URI?, condition: ArrayValidator.ValidationType,
                                        value: JSONValue?): ArrayValidator {
        return ArrayValidator(uri, pointer, condition, getInteger(value, uri, pointer))
    }

    private fun parseArrayUniqueItems(pointer: JSONPointer, uri: URI?, value: JSONValue?): UniqueItemsValidator? {
        if (value !is JSONBoolean)
            fatal("Must be boolean", uri, pointer)
        return if (value.booleanValue()) UniqueItemsValidator(uri, pointer) else null
    }

    private fun parsePattern(pointer: JSONPointer, uri: URI?, value: JSONValue?): PatternValidator {
        if (value !is JSONString)
            fatal("Must be string", uri, pointer)
        val regex = try {
            Regex(value.value)
        } catch (e: Exception) {
            val msg = e.message?.let { "- ${it.substringBefore('\n').trim()}" } ?: ""
            fatal("Pattern invalid $msg (${value.toErrorDisplay()})", uri, pointer)
        }
        return PatternValidator(uri, pointer, regex)
    }

    private fun fatal(message: String, uri: URI?, pointer: JSONPointer): Nothing {
        throw JSONSchemaException("$message - ${errorPointer(uri, pointer)}")
    }

    private fun errorPointer(uri: URI?, pointer: JSONPointer): String =
        uri?.withFragment(pointer)?.toString() ?: pointer.pointerOrRoot()

    private fun JSONPointer.pointerOrRoot() = if (this == JSONPointer.root) "root" else toString()
}
