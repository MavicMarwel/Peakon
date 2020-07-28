import Score.*
import com.github.nwillc.poink.workbook
import java.nio.file.Paths

fun main() {

    val file = Paths.get("C:\\Users\\mtereshc\\Sandbox\\Pulse_Action\\review\\07_2020\\Comments_13.07.2020.xlsx")
    val feedbacks = readComments(file).sortedBy { feedback -> feedback.driver }.toList()

    val overAll = Peakon(
        feedbacks.filter { f -> f.score == Detractors },
        feedbacks.filter { f -> f.score == Passive },
        feedbacks.filter { f -> f.score == Promoters }
    )

    with(overAll.predict(feedbacks)) {

        val (detractors, passives, promoters, notEvaluated) = this

        workbook {
            createSheet(Detractors, detractors)
            createSheet(Passive, passives)
            createSheet(Promoters, promoters)
            createSheet(NotEvaluated, notEvaluated)
        }.write("C:\\Users\\mtereshc\\Sandbox\\Pulse_Action\\review\\07_2020\\refined.xlsx")
    }

    //look for potential detractors
    with(Peakon(
        detractors = feedbacks.filter { f -> f.score == Detractors },
        promoters = feedbacks.filter { f -> f.score == Promoters }
    ).predict(feedbacks.filter { f -> f.score == Passive || f.score == NotEvaluated })
    ) {

        val (detractors, _, _, _) = this

        workbook {
            createSheet(
                "Risk",
                listOf("Category", "Probability", "Date", "Question", "Comment", "Score", "Group", "Driver", "Language"),
                detractors.map { (feedback, score) ->
                    listOf(score.category, score.probability)
                        .plus(feedback.toList())
                })
        }.write("C:\\Users\\mtereshc\\Sandbox\\Pulse_Action\\review\\07_2020\\risk.xlsx")
    }
}
