package freeSpeech.javafx.stage

import freeSpeech.controler.millisecondsToPixels
import freeSpeech.controler.pixelsToMilliseconds
import freeSpeech.javafx.component.TextBox
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.control.TextArea
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class EditStage(val textBox: TextBox): Stage(StageStyle.UNDECORATED) {

    //FIELDS
    private val _savedText: String = textBox.timedText.text
    private val _savedDuration: Duration = textBox.timedText.duration
    private val _savedStartTime: Duration = textBox.timedText.startTime

    private val _buttonApply: Button = Button(BUTTON_APPLY_TEXT).apply {
        isDefaultButton = true
        setOnAction {
            close()
        }
    }
    private val _buttonCancel: Button = Button(BUTTON_CANCEL_TEXT).apply {
        isCancelButton = true
        hoverProperty().addListener { _, _, newValue ->
            if (newValue) {
                textBox.timedText.text = _savedText
                textBox.timedText.duration = _savedDuration
                textBox.timedText.startTime = _savedStartTime
            }
            else {
                textBox.timedText.text = _textArea.text
                textBox.timedText.duration = _spinnerSizeText.value.pixelsToMilliseconds()
                textBox.timedText.startTime = _spinnerLocationText.value.pixelsToMilliseconds()
            }
        }
        setOnAction {
            close()
            textBox.timedText.text = _savedText
            textBox.timedText.duration = _savedDuration
            textBox.timedText.startTime = _savedStartTime
        }
    }
    private val _buttonDelete: Button = Button(BUTTON_DELETE_TEXT).apply {
        hoverProperty().addListener { _, _, newValue ->
            textBox.opacity = if (newValue) 0.3 else 1.0
            if (newValue)
                textBox.hideFrame()
            else
                textBox.showFrame(TEXTBOX_EDITING_COLOR)
        }
        setOnAction {
            val parent = textBox.parent as? AnchorPane
            parent?.children?.remove(textBox)
            close()
        }
    }
    private val _spinnerLocationText: Spinner<Double> = Spinner<Double>(
            0.0,
            Double.MAX_VALUE,
            textBox.timedText.startTime.millisecondsToPixels()
    ).apply {
        isEditable = true
        editor.textProperty().addListener { _, _, newValue ->
            val x = newValue.toDoubleOrNull()
            if (x != null)
                textBox.timedText.startTime = x.pixelsToMilliseconds()
        }
        valueProperty().addListener { _, _, newValue ->
            textBox.timedText.startTime = newValue.pixelsToMilliseconds()
        }
    }
    private val _spinnerSizeText: Spinner<Double> = Spinner<Double>(
            0.0,
            Double.MAX_VALUE,
            textBox.timedText.duration.millisecondsToPixels()
    ).apply {
        isEditable = true
        editor.textProperty().addListener { _, _, newValue ->
            val size = newValue.toDoubleOrNull()
            if (size != null && size >= TEXTBOX_EDITING_MIN_SIZE)
                textBox.timedText.duration = size.pixelsToMilliseconds()
        }
        valueProperty().addListener { _, _, newValue ->
            if (newValue >= TEXTBOX_EDITING_MIN_SIZE)
                textBox.timedText.duration = newValue.pixelsToMilliseconds()
        }
    }
    private val _textArea: TextArea = TextArea(textBox.timedText.text).apply {
        isWrapText = false
        textProperty().addListener { _, _, newValue ->
            textBox.timedText.text = newValue
        }
        selectAll()
    }


    init {
        scene = Scene(VBox().apply {
            alignment = Pos.TOP_CENTER
            background = Background(BackgroundFill(Color.STEELBLUE, CornerRadii.EMPTY, Insets.EMPTY))
            children.addAll(
                    HBox(Label("start time:"), _spinnerLocationText, Label("duration:"), _spinnerSizeText).apply {
                        alignment = Pos.BOTTOM_CENTER
                        spacing = 10.0
                    },
                    _textArea,
                    HBox(_buttonApply, _buttonCancel, _buttonDelete).apply {
                        alignment = Pos.CENTER
                        spacing = BUTTON_SPACING
                    }
            )
        })
        centerOnScreen()
        setOnCloseRequest { onClose() }
        show()
        _textArea.requestFocus()

        textBox.showFrame(TEXTBOX_EDITING_COLOR)
    }


    //METHODS
    override fun close() {
        onClose()
        super.close()
    }

    private fun onClose() {
        textBox.hideFrame()
    }


    companion object {
        const val BUTTON_APPLY_TEXT: String = "APPLY"
        const val BUTTON_CANCEL_TEXT: String = "CANCEL"
        const val BUTTON_DELETE_TEXT: String = "DELETE"
        const val BUTTON_SPACING: Double = 20.0

        val TEXTBOX_EDITING_COLOR: Color = Color.STEELBLUE
        const val TEXTBOX_EDITING_MIN_SIZE: Double = 10.0
    }
}