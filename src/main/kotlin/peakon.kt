import Score.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.nield.kotlinstatistics.CategoryProbability
import org.nield.kotlinstatistics.NaiveBayesClassifier
import org.nield.kotlinstatistics.toNaiveBayesClassifier
import java.lang.Math.abs
import java.util.*

data class Feedback(
    val date: String,           //A | Date
    val question: String,       //B | Question
    val comment: String,        //C | Comment
    val score: Score,           //D | Score
    val group: String,          //E | Group
    val driver: String,         //F | Driver
    val language: String        //G | Language
) {
    constructor(
        date: String,
        question: String,
        comment: String,
        score: String?, //Open questions can be with no Score
        group: String,
        driver: String,
        language: String
    ) : this(date, question, comment, score.toScore(), group, driver, language)
}

private fun String?.toScore(): Score {
    if ((null == this) || this.isEmpty())
        return NotEvaluated
    return when (toInt()) {
        in 9..10 -> Promoters
        in 7..8 -> Passive
        in 0..6 -> Detractors
        else -> NotEvaluated
    }
}

enum class Score {
    Promoters,
    Passive,
    Detractors,
    NotEvaluated;
}

class Peakon(val baseData: Collection<Feedback>) {
    val targetProbability = 0.8 //magic '0.8' based on observation
    val feedbackMessageWindowRate = 0.25 //25% gives the best prediction based on input tests

    val dictionary: NaiveBayesClassifier<String, Score> by lazy { baseData.teach() }

    suspend fun predict(feedbacks: Collection<Feedback>) = coroutineScope {
        val mutex = Mutex()
        //run predictions async
        val predictions = feedbacks.filter { f -> f.comment.isNotEmpty() && f.comment.length > 4 }
            .pmap { feedback ->
                val tokens = feedback.comment.splitWords()
                feedback to predict(tokens, dictionary).also { score ->
                    //if prediction is high-scored
                    if ((!score.probability.isNaN()) && abs( score.probability / targetProbability - 1) < 0.4) {
                        //teach global dictionary
                        mutex.withLock {
                            dictionary.addObservation(score.category, tokens)
                        }
                    }
                }
            }

        return@coroutineScope Output(
            predictions.filter { (_, score) -> score.category == Detractors },
            predictions.filter { (_, score) -> score.category == Passive },
            predictions.filter { (_, score) -> score.category == Promoters },
            predictions.filter { (_, score) -> score.category == NotEvaluated }
        )
    }

    suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
        map { async { f(it) } }.awaitAll()
    }

    private fun Collection<Feedback>.teach(): NaiveBayesClassifier<String, Score> = this.toNaiveBayesClassifier(
        featuresSelector = { it.comment.splitWords() },
        categorySelector = { it.score }
    )

    private fun predict(
        tokens: Iterable<String>,
        model: NaiveBayesClassifier<String, Score>
    ): CategoryProbability<Score> =
        model.predictWithProbability(tokens) ?: CategoryProbability(NotEvaluated, Double.NaN)

    private fun String.splitWords(
        windowSize: Double = feedbackMessageWindowRate
    ): Set<String> {
        val tokens = split(Regex("\\s")).asSequence()
            .map { it.replace(Regex("[^A-Za-z]"), "").lowercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }.toList()

        fun optimalWindowSize(tokens: List<String>): Int {
            val rate = (tokens.size * windowSize).toInt()
            return if (0 == rate) tokens.size else rate
        }

        val wSize = if (tokens.isEmpty()) 1 else optimalWindowSize(tokens)

        return tokens.windowed(wSize, wSize, true)
            .map { window -> window.joinToString(separator = " ") }
            .toSet()
    }

    data class Output(
        val detractors: Collection<Pair<Feedback, CategoryProbability<Score>>> = emptyList(),
        val passives: Collection<Pair<Feedback, CategoryProbability<Score>>> = emptyList(),
        val promoters: Collection<Pair<Feedback, CategoryProbability<Score>>> = emptyList(),
        val notEvaluated: Collection<Pair<Feedback, CategoryProbability<Score>>> = emptyList()
    )

}
