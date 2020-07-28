import Score.*
import com.github.nwillc.poink.PSheet
import com.github.nwillc.poink.PWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.nield.kotlinstatistics.CategoryProbability
import java.io.FileInputStream
import java.nio.file.Path

fun readComments(filePath: Path): Sequence<Feedback> {
    fun Row.toFeedback(): Feedback {
        val df = DataFormatter()
        fun cellValue(indx: Int) = df.formatCellValue(getCell(indx))
        return Feedback(
            cellValue(0),
            cellValue(1),
            cellValue(2),
            cellValue(3).toScore(),
            cellValue(4),
            cellValue(5),
            cellValue(6)
        )
    }
    return FileInputStream(filePath.toString()).use { PWorkbook(XSSFWorkbook(it)) }.iterator().asSequence()
        .filter { sheet -> sheet.sheetName == "Comments" }
        .flatMap { sheet -> sheet.iterator().asSequence() }
        .drop(1) //drop headers
        .map { it.toFeedback() }
}

private fun String?.toScore(): Score {
    if (null == this || this.isEmpty())
        return NotEvaluated
    return when (toInt()) {
        in 9..10 -> Promoters
        in 7..8 -> Passive
        in 0..6 -> Detractors
        else -> NotEvaluated
    }
}

fun Feedback.toList() =
    listOf(this.date, this.question, this.comment, this.score, this.group, this.driver, this.language)

fun PWorkbook.createSheet(
    targetScore: Score,
    processed: Collection<Pair<Feedback, CategoryProbability<Score>>>
) {
    createSheet(
        targetScore.name,
        listOf("Category", "Probability", "Date", "Question", "Comment", "Score", "Group", "Driver", "Language"),
        processed.map { (feedback, score) ->
            listOf(score.category, score.probability)
                .plus(feedback.toList())
        }
    )
}

fun PWorkbook.createSheet(
    name: String,
    columns: List<String>,
    data: Collection<List<Any>>
) {
    // Create a named cell style
    val headerStyle = headerStyle()

    sheet(name) {
        addHeader(columns, headerStyle)
        data.forEach { cells -> row(cells) }
    }
}

private fun PSheet.addHeader(columns: List<String>, headerStyle: CellStyle) {
    // Add a row with a style
    row(
        columns,
        headerStyle
    )
}

fun PWorkbook.headerStyle(): CellStyle {
    return cellStyle("Header") {
        fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        fillPattern = FillPatternType.SOLID_FOREGROUND
    }
}

