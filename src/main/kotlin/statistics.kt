import com.github.nwillc.poink.workbook
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths

fun main() {
    val root = Paths.get("C:\\Users\\mtereshc\\Sandbox\\Pulse_Action\\review")

    buildLanguageAlertsReport(
        root.resolve("06_2020\\Peakon Comments June 2020.xlsx"),
        root.resolve("06_2020\\language_alerts.xlsx")
    )

    buildLanguageAlertsReport(
        root.resolve("07_2020\\Comments_13.07.2020.xlsx"),
        root.resolve("07_2020\\language_alerts.xlsx")
    )
}

private fun buildLanguageAlertsReport(fileToProceed: Path, outputFile: Path) {
    createWorkbook(
        findLanguageAlerts(
            readComments(
                fileToProceed
            ).sortedBy { feedback -> feedback.driver }
        )
    ).write(
        outputFile.toAbsolutePath().toString()
    )
}

private fun findLanguageAlerts(feedbacks: Sequence<Feedback>) = feedbacks
    .filter { feedback ->
        feedback.comment.contains(
            Regex(
                pattern = ".*(language|english|polish)",
                option = RegexOption.IGNORE_CASE
            )
        )
    }

private fun createWorkbook(languageAlerts: Sequence<Feedback>) = workbook {
    createSheet(
        name = "alerts",
        columns = listOf("Date", "Question", "Comment", "Score", "Group", "Driver", "Language"),
        data = languageAlerts.map { feedback -> feedback.toList() }.toList()
    )
}

