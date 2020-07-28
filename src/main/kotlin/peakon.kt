import Score.*
import org.nield.kotlinstatistics.CategoryProbability
import org.nield.kotlinstatistics.NaiveBayesClassifier
import org.nield.kotlinstatistics.toNaiveBayesClassifier

data class Feedback(
    val date: String,           //A | Date
    val question: String,       //B | Question
    val comment: String,        //C | Comment
    val score: Score,           //D | Score
    val group: String,          //E | Group
    val driver: String,         //F | Driver
    val language: String        //G | Language
)

enum class Score {
    Promoters,
    Passive,
    Detractors,
    NotEvaluated;
}

class Peakon(
    private val detractors: Collection<Feedback> = emptyList(),
    private val passive: Collection<Feedback> = emptyList(),
    private val promoters: Collection<Feedback> = emptyList()
) {

    val targetProbability = 0.999
    val feedbackMessageWindowRate = 0.25 //25% gives the best prediction based on input tests

    private val _detractors by lazy { detractors.teach() }
    private val _passives by lazy { passive.teach() }
    private val _promoters by lazy { promoters.teach() }

    private val _dictionary by lazy { (detractors + passive + promoters).teach() }

    fun predict(feedbacks: Collection<Feedback>): Output {
        val predictions = feedbacks.map { feedback -> predict(feedback) }

        return Output(
            predictions.filter { (_, score) -> score.category == Detractors },
            predictions.filter { (_, score) -> score.category == Passive },
            predictions.filter { (_, score) -> score.category == Promoters },
            predictions.filter { (_, score) -> score.category == NotEvaluated }
        )
    }

    private fun Collection<Feedback>.teach(): NaiveBayesClassifier<String, Score> = this.toNaiveBayesClassifier(
        featuresSelector = { it.comment.splitWords() },
        categorySelector = { it.score }
    )

    private fun predict(feedback: Feedback): Pair<Feedback, CategoryProbability<Score>> {

        val tokens = feedback.comment.splitWords()

        val prediction = _dictionary.predictWithProbability(tokens)
        if (prediction != null && prediction.probability > targetProbability)
            return feedback to prediction

        return feedback to listOf(
            _detractors.predictWithProbability(tokens),
            _passives.predictWithProbability(tokens),
            _promoters.predictWithProbability(tokens),
            prediction
        ).fold(
            CategoryProbability(
                NotEvaluated,
                Double.NaN
            )
        )
        { acc, r ->
            if (null == r) acc else if (acc.probability > r.probability) acc else r
        }.also {
            //if prediction based on input groups found
            if (it.probability > targetProbability * 0.8) {
                //teach global dictionary
                _dictionary.addObservation(it.category, tokens)
            }
        }
    }

    private fun String.splitWords(
        windowSize: Double = feedbackMessageWindowRate
    ): Set<String> {
        val tokens = split(Regex("\\s")).asSequence()
            .map { it.replace(Regex("[^A-Za-z]"), "").toLowerCase() }
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
        val detractors: Collection<Pair<Feedback, CategoryProbability<Score>>>,
        val passives: Collection<Pair<Feedback, CategoryProbability<Score>>>,
        val promoters: Collection<Pair<Feedback, CategoryProbability<Score>>>,
        val notEvaluated: Collection<Pair<Feedback, CategoryProbability<Score>>>
    )

}


