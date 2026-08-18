package com.jason.mypersonalai.tools.impl

import com.jason.mypersonalai.tools.RiskLevel
import com.jason.mypersonalai.tools.Tool
import com.jason.mypersonalai.tools.ToolError
import com.jason.mypersonalai.tools.ToolExecutionResult
import com.jason.mypersonalai.tools.ToolInput

/**
 * Evaluates a basic arithmetic expression: + - * / with parentheses
 * and decimals, standard operator precedence.
 *
 * Deliberately minimal for Milestone 3 — no exponents, no functions,
 * no unit conversion. This tool exists primarily to prove the agent ->
 * router -> registry -> tool -> structured result pipeline works, not
 * to be a full calculator.
 */
class CalculatorTool : Tool {
    override val id: String = "calculator"
    override val description: String = "Evaluates a basic arithmetic expression."
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(input: ToolInput): ToolExecutionResult {
        return try {
            val result = ExpressionParser(input.raw).parse()
            ToolExecutionResult.Success(output = formatResult(result))
        } catch (e: ArithmeticException) {
            ToolExecutionResult.Failure(
                ToolError(displayMessage = "Calculator error: division by zero.", cause = e)
            )
        } catch (e: IllegalArgumentException) {
            ToolExecutionResult.Failure(
                ToolError(displayMessage = "Calculator error: invalid expression.", cause = e)
            )
        }
    }

    private fun formatResult(value: Double): String {
        // Show whole numbers without a trailing ".0".
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}

/**
 * Minimal recursive-descent parser/evaluator for + - * / and parentheses.
 *
 * Grammar:
 *   expression = term (('+' | '-') term)*
 *   term       = factor (('*' | '/') factor)*
 *   factor     = number | '(' expression ')' | '-' factor
 */
private class ExpressionParser(private val text: String) {
    private var pos = 0

    fun parse(): Double {
        val value = parseExpression()
        skipWhitespace()
        require(pos == text.length) { "Unexpected trailing characters." }
        return value
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            skipWhitespace()
            when (peek()) {
                '+' -> { pos++; value += parseTerm() }
                '-' -> { pos++; value -= parseTerm() }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (true) {
            skipWhitespace()
            when (peek()) {
                '*' -> { pos++; value *= parseFactor() }
                '/' -> {
                    pos++
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    value /= divisor
                }
                else -> return value
            }
        }
    }

    private fun parseFactor(): Double {
        skipWhitespace()
        return when (peek()) {
            '-' -> { pos++; -parseFactor() }
            '(' -> {
                pos++
                val value = parseExpression()
                skipWhitespace()
                require(peek() == ')') { "Missing closing parenthesis." }
                pos++
                value
            }
            else -> parseNumber()
        }
    }

    private fun parseNumber(): Double {
        skipWhitespace()
        val start = pos
        while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
        require(pos > start) { "Expected a number at position $pos." }
        return text.substring(start, pos).toDoubleOrNull()
            ?: throw IllegalArgumentException("Invalid number.")
    }

    private fun skipWhitespace() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }

    private fun peek(): Char? = if (pos < text.length) text[pos] else null
}
