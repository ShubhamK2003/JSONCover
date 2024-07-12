
import coverage.LineCoverage
import parser.Parser
import java.io.File

fun main(){
    val schema = Parser.parseFile("/Users/shadowmonarch/Desktop/amazon_ps1/json-kotlin-schema/src/main/kotlin/Schema.json")
    val json = File("/Users/shadowmonarch/Desktop/amazon_ps1/json-kotlin-schema/src/main/kotlin/Payload.json").readText()
    val output = schema.validateBasic(json)

    val violatedProperties = mutableSetOf<String?>()
    output.errors?.forEach {
        println("${it.error} - ${it.absoluteKeywordLocation} | ${it.instanceLocation}")
        if(it.error!= "A subschema had errors")
        {
            val propertyPath = it.absoluteKeywordLocation
            val propertyPath1 = propertyPath?.substringAfter("http://pwall.net/test#/")
            violatedProperties.add(propertyPath1)
        }
    }

    val validatedConstraints = LineCoverage.getTotalConstraints() - violatedProperties.size
    LineCoverage.setValidatedConstraint(validatedConstraints)
    LineCoverage.printLineCoverage()
}


