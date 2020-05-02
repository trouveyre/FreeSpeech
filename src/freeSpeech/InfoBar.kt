package freeSpeech

import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Insets
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.util.Duration
import javafx.util.StringConverter
import kotlin.time.*


class InfoBar(height: Double, timeLinePosition: Double): HBox() {

    companion object {
        const val SPINNER_TIME_STEP: Double = 5.0
        val SPINNER_DEFAULT_TIME: Duration = Duration.ZERO
    }


    //FIELDS
    private val _spinnerTime: Spinner<Duration> by lazy {
        Spinner(object : SpinnerValueFactory<Duration>() {
            init {
                value = SPINNER_DEFAULT_TIME
                converter = object : StringConverter<Duration>() {

                    override fun fromString(string: String): Duration {
                        val (h, min, s, ms) = string.split(":").map { it.toInt() }
                        return Duration((h.hours + min.minutes + s.seconds + ms.milliseconds).inMilliseconds)
                    }

                    override fun toString(that: Duration): String {
                        return that.toMillis().milliseconds.toComponents { hours, minutes, seconds, nanoseconds ->
                            "%02d:%02d:%02d:%03d".format(hours, minutes, seconds, nanoseconds.nanoseconds.inMilliseconds.toInt())
                        }
                    }
                }
            }

            override fun increment(steps: Int) {
                value = Duration(value.toMillis() + (SPINNER_TIME_STEP * steps))
            }

            override fun decrement(steps: Int) {
                value = Duration(value.toMillis() - (SPINNER_TIME_STEP * steps))
            }
        }).apply {
            minHeight = height
            maxHeight = height
            editor.textProperty().addListener { _, _, newValue ->
                val duration = _spinnerTime.valueFactory.converter.fromString(newValue)
                if (currentTime != duration)
                    currentTime = duration
            }
        }
    }


    //PROPERTIES
    var currentTime: Duration
        get() = _spinnerTime.value
        set(value) {
            _spinnerTime.valueFactory.value = value
        }
    val currentTimeProperty: ReadOnlyProperty<Duration>
        get() = _spinnerTime.valueProperty()

    var offset: Double
        get() = offsetProperty.value
        set(value) {
            offsetProperty.value = value
        }
    val offsetProperty: DoubleProperty = SimpleDoubleProperty().apply {
        addListener { _, _, newValue ->
            _spinnerTime.translateX = newValue.toDouble()
        }
    }


    init {
        minWidth = FreeSpeech.MIN_WIDTH
        maxWidth = FreeSpeech.MAX_WIDTH
        minHeight = height
        maxHeight = height

        children.addAll(_spinnerTime)

        background = Background(BackgroundFill(Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY))
        isFillHeight = true
        offset = timeLinePosition
    }


    //METHODS
}