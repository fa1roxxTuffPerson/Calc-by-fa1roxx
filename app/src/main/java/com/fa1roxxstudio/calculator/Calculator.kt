package com.fa1roxxstudio.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.math.MathContext

private const val OPS = "+−×÷"

class Calculator {
    var expr by mutableStateOf("")
        private set
    var prev by mutableStateOf("")
        private set
    var result by mutableStateOf("")
        private set
    var done by mutableStateOf(false)
        private set

    fun press(k: String) {
        when {
            k == "AC" -> { expr = ""; prev = ""; result = ""; done = false }
            k == "BS" -> backspace()
            k == "=" -> equals()
            k == "NEG" -> negate()
            k == "%" -> percent()
            k == "." -> dot()
            k.length == 1 && k[0] in OPS -> operator(k[0])
            else -> digit(k)
        }
    }

    private fun leaveDone(clear: Boolean = false) {
        if (done) {
            done = false
            prev = ""
            if (clear) expr = ""
        }
    }

    private fun lastNum(): String = Regex("(\\d*\\.?\\d*)$").find(expr)?.value ?: ""

    private fun backspace() {
        leaveDone()
        expr = expr.dropLast(1)
        if (expr == "−") expr = ""
        updatePreview()
    }

    private fun equals() {
        if (expr.isEmpty() || done) return
        val s = expr.trimEnd { it in OPS }
        try {
            val r = format(Parser(s).parse())
            prev = s
            expr = r
            result = r
        } catch (e: Exception) {
            prev = s
            expr = ""
            result = "Ошибка"
        }
        done = true
    }

    private fun operator(op: Char) {
        leaveDone()
        when {
            expr.isEmpty() -> if (op == '−') expr = "−"
            expr == "−" -> Unit
            expr.last() in OPS -> expr = expr.dropLast(1) + op
            else -> expr += op
        }
        updatePreview()
    }

    private fun percent() {
        leaveDone()
        if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')' || expr.last() == '%')) {
            expr += "%"
        }
        updatePreview()
    }

    private fun negate() {
        leaveDone()
        val wrapped = Regex("\\(−(\\d*\\.?\\d*)\\)$").find(expr)
        if (wrapped != null) {
            expr = expr.substring(0, wrapped.range.first) + wrapped.groupValues[1]
        } else {
            val n = lastNum()
            if (n.isNotEmpty() && n != ".") expr = expr.dropLast(n.length) + "(−$n)"
        }
        updatePreview()
    }

    private fun dot() {
        leaveDone(clear = true)
        if (expr.endsWith(")") || expr.endsWith("%")) return
        val n = lastNum()
        if ('.' in n) return
        expr += if (n.isEmpty()) "0." else "."
        updatePreview()
    }

    private fun digit(k: String) {
        leaveDone(clear = true)
        if (expr.endsWith(")") || expr.endsWith("%")) return
        val n = lastNum()
        expr = when {
            n == "0" -> expr.dropLast(1) + k
            n.replace(".", "").length >= 15 -> return
            else -> expr + k
        }
        updatePreview()
    }

    private fun updatePreview() {
        result = ""
        val withoutLoneNumber = expr.replace(Regex("^\\(−\\d*\\.?\\d*\\)?$"), "")
        if (!Regex("[+−×÷%]").containsMatchIn(withoutLoneNumber)) return
        result = try {
            format(Parser(expr.trimEnd { it in OPS }).parse())
        } catch (e: Exception) {
            ""
        }
    }
}

private fun format(v: Double): String {
    require(v.isFinite())
    return BigDecimal(v).round(MathContext(12)).stripTrailingZeros()
        .toPlainString().replace('-', '−')
}

fun display(s: String): String =
    Regex("\\d+(\\.\\d*)?").replace(s) { m ->
        val parts = m.value.split('.')
        val int = parts[0].replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), "\u202F")
        if (parts.size > 1) "$int,${parts[1]}" else int
    }

private class Parser(text: String) {
    private val src = text.replace('×', '*').replace('÷', '/').replace('−', '-')
    private var i = 0
    private var pct = false

    private fun peek(): Char? = src.getOrNull(i)

    fun parse(): Double {
        val v = expression()
        check(i == src.length) { "лишние символы" }
        return v
    }

    private fun expression(): Double {
        var v = term()
        while (peek() == '+' || peek() == '-') {
            val op = src[i++]
            var r = term()
            if (pct) r = v * r
            v = if (op == '+') v + r else v - r
        }
        return v
    }

    private fun term(): Double {
        var v = unary()
        while (peek() == '*' || peek() == '/') {
            val op = src[i++]
            val r = unary()
            v = if (op == '*') v * r else v / r
        }
        return v
    }

    private fun unary(): Double {
        pct = false
        if (peek() == '-') { i++; return -unary() }
        var v = primary()
        while (peek() == '%') { i++; v /= 100; pct = true }
        return v
    }

    private fun primary(): Double {
        if (peek() == '(') {
            i++
            val v = expression()
            if (peek() == ')') i++
            return v
        }
        val start = i
        while (i < src.length && (src[i].isDigit() || src[i] == '.')) i++
        val m = src.substring(start, i)
        check(m.isNotEmpty() && m != ".") { "ожидалось число" }
        return m.toDouble()
    }
}
