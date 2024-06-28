import org.jsoncover.JSONSchema
import org.jsoncover.coverage.LineCoverage
import java.io.File

fun main(){
    val schema = org.jsoncover.JSONSchema.parseFile("")
    val json = File("").readText()
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
