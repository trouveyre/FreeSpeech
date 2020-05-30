package freeSpeech.javafx.component

import javafx.scene.control.SpinnerValueFactory
import javafx.util.StringConverter
import kotlin.time.*

@OptIn(ExperimentalTime::class)
object SpinnerDurationValueFactory : SpinnerValueFactory<Duration>() {


    val DEFAULT_TIME: Duration = Duration.ZERO
    val TIME_STEP: Duration = 5.milliseconds


    init {
        value = DEFAULT_TIME
        converter = object : StringConverter<Duration>() {

            @OptIn(ExperimentalTime::class)
            override fun fromString(string: String): Duration {
                val (h, min, s, ms) = string.split(":").map { it.toInt() }
                return h.hours + min.minutes + s.seconds + ms.milliseconds
            }

            @OptIn(ExperimentalTime::class)
            override fun toString(that: Duration): String {
                return that.toComponents { hours, minutes, seconds, nanoseconds ->
                    "%02d:%02d:%02d:%03d".format(hours, minutes, seconds, nanoseconds.nanoseconds.inMilliseconds.toInt())
                }
            }
        }
    }

    override fun increment(steps: Int) {
        value += TIME_STEP * steps
    }

    override fun decrement(steps: Int) {
        value -= TIME_STEP * steps
    }
}