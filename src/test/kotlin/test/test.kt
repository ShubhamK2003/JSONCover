import org.jetbrains.annotations.TestOnly

// this is a testing file

// file paths : src/main/resources/test
// it has files:
// 1. valid_schema_1.json
// 2. valid_schema_2.json
// 3. invalid_schema_1.json
// 4. invalid_schema_2.json
// 5. valid_payloads_1.json
// 6. valid_payloads_2.json
// 7. invalid_payloads_1.json
// 8. invalid_payloads_2.json


//test function
//@TestOnly
fun test(){
    val schema = Parser.parseFile("src/main/resources/test/valid_schema_1.json")
    val json = File("src/main/resources/test/valid_payloads_1.json").readText()
    val output = schema.validateBasic(json)

    val violatedProperties = mutableSetOf<String?>()
    output.errors?.forEach {
        println("${it.error} - ${it.absoluteKeywordLocation} | ${it.instanceLocation}")
        if(it.error!= "A subschema had errors") {
//            val propertyPath = it.absoluteKeywordLocation
//            val propertyPath1 = propertyPath?.substringAfter("http://pwall.net/test#/")
            violatedProperties.add(it.instanceLocation)

        }
    }

    CheckedConstraints.printAllConstraints()

    println("Violated Properties: $violatedProperties")

    val validatedConstraints = LineCoverage.getTotalConstraints() - violatedProperties.size
    LineCoverage.setValidatedConstraint(validatedConstraints)
    LineCoverage.printLineCoverage()
}

fun main(){
    test()
}