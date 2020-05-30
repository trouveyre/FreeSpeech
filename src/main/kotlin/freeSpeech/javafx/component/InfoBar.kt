package freeSpeech.javafx.component

import freeSpeech.javafx.FreeSpeech
import freeSpeech.view.DocumentListener
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Insets
import javafx.scene.control.Spinner
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import kotlin.time.*


@ExperimentalTime
class InfoBar(height: Double, timeLinePosition: Double): HBox(), DocumentListener {


    //FIELDS
    private val _spinnerTime: Spinner<Duration> by lazy {
        Spinner(SpinnerDurationValueFactory).apply {
            minHeight = height
            maxHeight = height
            editor.textProperty().addListener { _, _, newValue ->
                val duration = _spinnerTime.valueFactory.converter.fromString(newValue)
                if (FreeSpeech.DOCUMENT_OPERATOR.currentTime != duration)
                    FreeSpeech.DOCUMENT_OPERATOR.setCurrentTime(duration, this@InfoBar)
            }
        }
    }


    //PROPERTIES
    override var currentTime: Duration
        get() = _spinnerTime.value
        set(value) {
            _spinnerTime.valueFactory.value = value
        }

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