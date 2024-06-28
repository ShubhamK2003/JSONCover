package org.jsoncover.coverage

object LineCoverage {
    private var totalConstraints: Int = 0
    private var validatedConstraints: Int = 0

    fun incrementTotalConstraints() {
        totalConstraints++
    }

    fun incrementTotalConstraints(value: Int) {
        totalConstraints+=value
    }

    fun setValidatedConstraint(value: Int) {
        validatedConstraints = value
    }

    fun getTotalConstraints(): Int {
        return totalConstraints
    }

    fun getValidatedConstraints(): Int {
        return validatedConstraints
    }

    fun getCoveragePercentage(): Double {
        return if (totalConstraints == 0) {
            0.0
        } else {
            (validatedConstraints.toDouble() / totalConstraints) * 100
        }
    }

    fun printLineCoverage() {
        println("---------------------------------------------------------------------")
        println("Total Constraints: $totalConstraints")
        println("Validated Constraints: $validatedConstraints")
        println("Violated Constraints: ${totalConstraints - validatedConstraints}")
        println("Line Coverage: ${"%.2f".format(getCoveragePercentage())}%")
        println("---------------------------------------------------------------------")
    }

    fun reset() {
        totalConstraints = 0
        validatedConstraints = 0
    }

    override fun toString(): String {
        return "Total Constraints: $totalConstraints, Validated Constraints: $validatedConstraints, Coverage: ${getCoveragePercentage()}%"
    }
}