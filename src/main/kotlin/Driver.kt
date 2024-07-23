import coverage.LineCoverage
import parser.Parser
import java.io.File

@SuppressWarnings("unused")
object JSONCover {
    @JvmStatic
    fun main(args: Array<String>) {
        val schemaPath = "/Users/shadowmonarch/Downloads/json-kotlin-schema 2/src/main/kotlin/Schema.json"
        val folderPath = "/Users/shadowmonarch/Downloads/json-kotlin-schema 2/src/main/kotlin/Payload.json"

        validateSchema(schemaPath, folderPath, 90.00)
    }

    fun validateSchema(schemaPath: String, folderPath: String, coverageThreshold: Double) {
        val files = File(folderPath).listFiles { _, name -> name.endsWith(".json") } ?: arrayOf()

        val lowCoverageFiles = mutableListOf<String>()
        files.forEach { file ->
            val json = file.readText()
            val schema = Parser.parseFile(schemaPath, folderPath+"/"+file.name)
            println()
            println()
            println("------ ${file.name} -------")
            println()

            val output = schema.validateBasic(json)

            val violatedProperties = mutableSetOf<String?>()
            output.errors?.forEach {
                println("${it.error} - ${it.absoluteKeywordLocation} | ${it.instanceLocation}")
                if (it.error != "A subschema had errors") {
                    violatedProperties.add(it.instanceLocation)
                }
            }

            val validatedConstraints = LineCoverage.getTotalConstraints() - violatedProperties.size - LineCoverage.getUnvalidatedConstraints()
            LineCoverage.setValidatedConstraint(validatedConstraints)
            LineCoverage.printLineCoverage()

            if (LineCoverage.getCoveragePercentage() < coverageThreshold) {
                lowCoverageFiles.add(file.name)
            }
            LineCoverage.reset()

        }
        if (lowCoverageFiles.isNotEmpty()) {
            throw Exception("The following files have coverage below the ${coverageThreshold}% threshold: ${lowCoverageFiles.joinToString(", ")}")
        }
    }
}