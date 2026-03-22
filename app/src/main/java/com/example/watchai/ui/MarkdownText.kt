package com.example.watchai.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 手表端 Markdown 渲染
 * 支持：**粗体** *斜体* `代码` # 标题 - 列表 > 引用
 * LaTeX：$$...$$  $...$  原样显示但用青色区分，不乱码
 */
@Composable
fun MarkdownText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        // 先将 $$块$$ 整体替换成特殊标记，避免跨行解析问题
        val processed = preprocess(text)
        val lines = processed.lines()
        lines.forEachIndexed { idx, raw ->
            when {
                raw.startsWith("### ") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color))
                    appendInline(raw.removePrefix("### "), color)
                    pop()
                }
                raw.startsWith("## ") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color))
                    appendInline(raw.removePrefix("## "), color)
                    pop()
                }
                raw.startsWith("# ") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color))
                    appendInline(raw.removePrefix("# "), color)
                    pop()
                }
                raw.startsWith("- ") || raw.startsWith("* ") && !raw.startsWith("**") -> {
                    append("• ")
                    appendInline(raw.substring(2), color)
                }
                raw.startsWith("> ") -> {
                    pushStyle(SpanStyle(color = Color(0xFF718096)))
                    append("│ ")
                    appendInline(raw.removePrefix("> "), color)
                    pop()
                }
                else -> appendInline(raw, color)
            }
            if (idx < lines.size - 1) append("\n")
        }
    }

    Text(
        text = annotated,
        modifier = modifier,
        style = TextStyle(
            fontSize   = fontSize,
            lineHeight = (fontSize.value * 1.5f).sp,
            color      = color
        )
    )
}

/**
 * 预处理：把 $$...$$ 块（可能跨行）替换为单行，便于逐行解析
 * 同时保留内容，只是去掉 $$ 标记，用特殊前缀标记它是公式
 */
private fun preprocess(text: String): String {
    var result = text
    // 1. 过滤 <think>...</think> 思考链（部分中转站混入content）
    result = result.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
    // 2. 块级公式 $$...$$
    val blockRegex = Regex("""\$\$(.+?)\$\$""", RegexOption.DOT_MATCHES_ALL)
    result = blockRegex.replace(result) { mr ->
        "§MATH§" + mr.groupValues[1].trim().replace("\n", " ") + "§END§"
    }
    // 3. 行内公式 $...$（排除 $$）
    val inlineRegex = Regex("""\$(?!\$)(.+?)(?<!\$)\$""")
    result = inlineRegex.replace(result) { mr ->
        "§IMATH§" + mr.groupValues[1] + "§END§"
    }
    return result
}

/**
 * 行内解析：**bold** *italic* `code` §MATH§ §IMATH§
 * 关键：必须先判断 ** 再判断 *，避免冲突
 */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInline(
    text: String,
    baseColor: Color
) {
    val mathColor   = Color(0xFF4DB6AC)  // 青色：表示公式
    val codeColor   = Color(0xFF80CBC4)  // 浅青：代码
    var i = 0
    while (i < text.length) {
        when {
            // 块级公式 §MATH§...§END§
            text.startsWith("§MATH§", i) -> {
                val end = text.indexOf("§END§", i + 6)
                if (end != -1) {
                    pushStyle(SpanStyle(color = mathColor))
                    append("[公式] ")
                    append(text.substring(i + 6, end))
                    pop()
                    i = end + 5
                } else { append(text[i]); i++ }
            }
            // 行内公式 §IMATH§...§END§
            text.startsWith("§IMATH§", i) -> {
                val end = text.indexOf("§END§", i + 7)
                if (end != -1) {
                    pushStyle(SpanStyle(color = mathColor))
                    append(text.substring(i + 7, end))
                    pop()
                    i = end + 5
                } else { append(text[i]); i++ }
            }
            // **粗体** — 必须在 *斜体* 前判断
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            // *斜体*
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && end > i + 1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            // `代码`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    pushStyle(SpanStyle(color = codeColor, fontWeight = FontWeight.Medium))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}
