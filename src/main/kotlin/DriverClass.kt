import coverage.LineCoverage
import parser.Parser
import java.io.File

class DriverClass {
    fun get_line_coverage(schemaFilename : String, payloadFilename : String):Double{
        // Parse the schema and check unvalidated properties
        val schema = Parser.parseFile(schemaFilename, payloadFilename)

        // Read the payload file
        val json = File(payloadFilename).readText()

        // Validate the payload against the schema
        val output = schema.validateBasic(json)

        val violatedProperties = mutableSetOf<String?>()
        output.errors?.forEach {
//        println("${it.error} - ${it.absoluteKeywordLocation} | ${it.instanceLocation}")
            if (it.error != "A subschema had errors") {
                violatedProperties.add(it.instanceLocation)
            }
        }

        // Calculate validated constraints
        val validatedConstraints = LineCoverage.getTotalConstraints() - violatedProperties.size - LineCoverage.getUnvalidatedConstraints()
        LineCoverage.setValidatedConstraint(validatedConstraints)

        // Print the coverage information
//    LineCoverage.printLineCoverage()
        return LineCoverage.getCoveragePercentage()
    }

    fun print_report(schemaFilename: String, payloadFilename: String){
        // Parse the schema and check unvalidated properties
        val schema = Parser.parseFile(schemaFilename, payloadFilename)

        // Read the payload file
        val json = File(payloadFilename).readText()

        // Validate the payload against the schema
        val output = schema.validateBasic(json)

        val violatedProperties = mutableSetOf<String?>()
        output.errors?.forEach {
            println("${it.error} - ${it.absoluteKeywordLocation} | ${it.instanceLocation}")
            if (it.error != "A subschema had errors") {
                violatedProperties.add(it.instanceLocation)
            }
        }

        // Calculate validated constraints
        val validatedConstraints = LineCoverage.getTotalConstraints() - violatedProperties.size - LineCoverage.getUnvalidatedConstraints()
        LineCoverage.setValidatedConstraint(validatedConstraints)

        // Print the coverage information
        LineCoverage.printLineCoverage()
    }


}