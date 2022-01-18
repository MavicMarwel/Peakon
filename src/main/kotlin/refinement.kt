import Score.*
import com.github.nwillc.poink.workbook
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

fun main() = runBlocking {
    //1. Specify working directory
    val root = Paths.get("C:\\Users\\mtereshc\\Sandbox\\Pulse_Action\\review\\01_2022")
    //where to write results
    val destinationFile = root.resolve("refined.xlsx").toString()

    //2. Pick-up the file to be evaluated
    val file = root.resolve("Comments.xlsx")

    //3. Sort feedbacks by 'driver' for easy review. Exclude 'short' answers.
    val feedbacks = readComments(file).sortedBy { feedback -> feedback.driver }
        .filter { f -> f.comment.isNotEmpty() && f.comment.length > 4 }
        .toList()

    //4. Teach model only on feedbacks with scores. Here can be used different subset of feedbacks.
    // Now the same source used for 'teach' and for 'evaluate'
    val overAll = Peakon(feedbacks.filter { f -> f.score != NotEvaluated })

    //predict ALL
    with(overAll.predict(feedbacks)) {

        val (detractors, passives, promoters, notEvaluated) = this

        workbook {
            createSheet(Detractors, detractors)
            createSheet(Passive, passives)
            createSheet(Promoters, promoters)
            createSheet(NotEvaluated, notEvaluated)
        }.write(destinationFile)
    }

    //Exercise 2.
    //Look for potential detractors in Passive and Open-ended feedbacks
    with(

        Peakon(
            feedbacks.filter { f -> f.score == Detractors } //baseline is  'detractors'
        ).predict(
            feedbacks.filter { f -> f.score == Passive || f.score == NotEvaluated }//evaluate on passives and open-ended
        )
    ) {
        val ryzykoFile = root.resolve("risk.xlsx").toString()
        val (detractors, _, _, _) = this

        workbook {
            createSheet(
                "Risk",
                listOf(
                    "Category",
                    "Probability",
                    "Date",
                    "Question",
                    "Comment",
                    "Score",
                    "Group",
                    "Driver",
                    "Language"
                ),
                detractors.map { (feedback, score) ->
                    listOf(score.category, score.probability)
                        .plus(feedback.toList())
                })
        }.write(ryzykoFile)
    }
}
