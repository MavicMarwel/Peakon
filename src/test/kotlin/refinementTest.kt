import io.kotest.assertions.assertSoftly
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class PeakonSurvey : WordSpec({

    val peakon = Peakon(detractors() + passive() + promoters())
    val allPredictions = runBlocking { peakon.predict(detractors() + promoters() + passive()) }

    "Detractors detected with high probability" should {
        with(peakon.predict(detractors())) {

            this.passives shouldHaveSize 0
            this.promoters shouldHaveSize 0
            this.notEvaluated shouldHaveSize 0

            detractors.forEach {
                val (score, probability) = it.second
                it.first.comment {
                    assertSoftly {
                        score shouldBe Score.Detractors
                        probability shouldBeGreaterThan peakon.targetProbability
                    }
                }
            }
        }
    }


    "Passives detected with high probability" should {
        with(peakon.predict(passive())) {

            this.detractors shouldHaveSize 0
            this.promoters shouldHaveSize 0
            this.notEvaluated shouldHaveSize 0

            passives.forEach { (feedback, prediction) ->
                feedback.comment {
                    val (score, probability) = prediction
                    assertSoftly {
                        score shouldBe Score.Passive
                        probability shouldBeGreaterThan 0.2
                    }
                }
            }
        }
    }

    "Promoters detected with high probability" should {
        with(peakon.predict(promoters())) {

            this.detractors shouldHaveSize 0
            this.passives shouldHaveSize 0
            this.notEvaluated shouldHaveSize 0

            promoters.forEach {
                val (score, probability) = it.second
                it.first.comment {
                    assertSoftly {
                        score shouldBe Score.Promoters
                        probability shouldBeGreaterThan peakon.targetProbability
                    }
                }
            }
        }
    }

    "Detractors high probability in average metric" should {
        with(allPredictions) {

            detractors.forEach {
                val (score, probability) = it.second
                it.first.comment {
                    assertSoftly {
                        score shouldBe Score.Detractors
                        probability shouldBeGreaterThan peakon.targetProbability
                    }
                }
            }
        }
    }

    "Passives high probability in average metric" should {
        with(allPredictions) {

            passives.forEach {
                val (score, probability) = it.second
                it.first.comment {
                    assertSoftly {
                        score shouldBe Score.Passive
                        probability shouldBeGreaterThan 0.2
                    }
                }
            }
        }
    }

    "Promoters high probability in average metric" should {
        with(allPredictions) {

            promoters.forEach {
                val (score, probability) = it.second
                it.first.comment {
                    assertSoftly {
                        score shouldBe Score.Promoters
                        probability shouldBeGreaterThan peakon.targetProbability
                    }
                }
            }
        }
    }

})


fun promoters(): List<Feedback> = listOf(
    "The Xbox is broken ;) The rest is fine.",
    "yes!! One team, one spirit",
    "I can count on HR Team and my PDL",
    "my PDL makes everything to keep me motivated",
    "My manager always leads & supports our team, treat each member individually.",
    "My manager is one of my closest coworkers",
    "I am happy working with him.",
    "Great team. Great people",
    "I have fun in my work and  this bring great meaningfulness to my life",
    "Flexibility is quite important to me and I feel Capgemini does a great job with that",
    "better then i expected",
    "The manager from Capgemini really cares about the people. Always ready to help and discuss.",
    "I have as much communication as needed. With some persons i even communicate more than it was before covid.",
    "As a team member i always try to do things in a best way and this is a red line in our team performance. I'm able to suggest own decisions and fulfill them after they are adopted by team.",
    "Absolutely. Client communications is through usual channels & professional whether WFH or WFO",
    "My manager tries to find balance between bussines needs and my development needs",
    "From my and clients perspective working remote is much more efficient. I can concentrate fully on them and clients are giving only possitive feedbeck.",
    "I get constant feedback from my manager and coworkers",
    "My potential has been recognized by customer, and always in cap as well",
    "My manager is very supportive and helping in case of any problems.",
    "Always, he does it almost perfect, understands all my priorities and individual situation",
    "Different projects, trainings, certifications, pdp to control it. I like it.",
    "Absolutely. That's what yearly goal sheet is for. On a minute scope, communications are clear through varied channels - email, chat, meetings, reviews. And we work in a continuous flow of stream wherein progress is apparent.",
    "Well, I can count on them"
).map { it asFeedback Score.Promoters }

fun passive(): List<Feedback> = listOf(
    "Can't say yet.",
    "Sometimes I do.",
    "Hard to answer",
    "Definitely",
    "No difference",
    "Partially agree",
    "HR service should be improved",
    "Depends on salary",
    "It's hard to say after a few months with new Company. I will see in next few.",
    "I would say yes, most likely. Personally I'm not so up to date with all the details on that. It's not that they are not public, but I actually didn't look into this topic very carefully yet.",
    "Generally yes, except from tiny laptop with tiny screen.",
    "Unfortunately there are situations that even I have more technical knowledge I can't decide what path should we choose, also I would change the way of working.",
    "depending on conditions"
).map { it asFeedback Score.Passive }


fun detractors(): List<Feedback> = listOf(
    "This pulse makes no sense",
    "It's to soon to judge.",
    "I just don't know, so choosing middle option",
    "No soap in the toilet...",
    "We are in a bad open space now",
    "Overload with work and no time for learning",
    "Lack of clear career path",
    "I have a lot on me. I feel overloaded with client queries",
    "Project (tower) dependent. Hard to say.",
    "The project I'm in is boring and does not provide any challenges or opportunities for growth. Moreover, since it is considered 'low-priority\" project the management along with business side could not care less what is done in said project. It is hard to feel any kind of \"accomplishment\" working in such environment.",
    "Bigger problem is timezone difference between me and client, than remote working. Most of my co-workers/clients are remote to me since beginig of my work in this company - recent changes COVID related makes no difference in this area.",
    "I see the path but there is not much benefit advancing it.",
    "I doesn't but I don't think many cares",
    "I'm not getting any feedback about my work.",
    "I don't fully understand this sentence. What does it actually mean to unleash human energy through technology?"
).map { it asFeedback Score.Detractors }

private infix fun String.asFeedback(score: Score) = Feedback("1/1/70", "", this, score, "", "", "")

