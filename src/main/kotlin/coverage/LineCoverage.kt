package coverage

object LineCoverage {
    private var totalConstraints: Int = 0
    private var validatedConstraints: Int = 0
    private var unvalidatedConstraints: Int = 0

    fun incrementTotalConstraints() {
        totalConstraints++
    }

    fun incrementTotalConstraints(value: Int) {
        totalConstraints += value
    }

    fun setValidatedConstraint(value: Int) {
        validatedConstraints = value
    }

    fun incrementUnvalidatedConstraints() {
        unvalidatedConstraints++
    }

    fun getTotalConstraints(): Int {
        return totalConstraints
    }

    fun getUnvalidatedConstraints(): Int {
        return unvalidatedConstraints
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
        println("Violated Constraints: ${totalConstraints - validatedConstraints- unvalidatedConstraints}")
        println("Unvalidated Constraints: $unvalidatedConstraints")
        println("Line Coverage: ${"%.2f".format(getCoveragePercentage())}%")
        println("---------------------------------------------------------------------")
    }

    @Suppress("unused")
    fun reset() {
        totalConstraints = 0
        validatedConstraints = 0
        unvalidatedConstraints = 0
    }

    override fun toString(): String {
        return "Total Constraints: $totalConstraints, Validated Constraints: $validatedConstraints, " +
                "Unvalidated Constraints: $unvalidatedConstraints, Coverage: ${getCoveragePercentage()}%"
    }
}