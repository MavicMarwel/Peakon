import Score.*
import com.github.nwillc.poink.PSheet
import com.github.nwillc.poink.PWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.nield.kotlinstatistics.CategoryProbability
import java.io.FileInputStream
import java.nio.file.Path

fun readComments(filePath: Path, sheetName: String = "Comments"): Sequence<Feedback> {
    val df = DataFormatter()

    fun Row.toFeedback(): Feedback {
        fun cellValue(indx: Int) = df.formatCellValue(getCell(indx))
        return Feedback(
            date = cellValue(0),
            question = cellValue(1),
            comment = cellValue(2),
            score = cellValue(3),
            group = cellValue(4),
            driver = cellValue(5),
            language = cellValue(6)
        )
    }
    return FileInputStream(filePath.toString()).use { PWorkbook(XSSFWorkbook(it)) }.iterator().asSequence()
        .filter { sheet -> sheet.sheetName == sheetName }
        .flatMap { sheet -> sheet.iterator().asSequence() }
        .drop(1) //drop headers
        .map(Row::toFeedback)
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

