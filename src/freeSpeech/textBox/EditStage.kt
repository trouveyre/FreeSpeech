package freeSpeech.textBox

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Spinner
import javafx.scene.control.TextArea
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle

class EditStage(val textBox: TextBox): Stage(StageStyle.UNDECORATED) {

    companion object {
        const val BUTTON_APPLY_TEXT: String = "APPLY"
        const val BUTTON_CANCEL_TEXT: String = "CANCEL"
        const val BUTTON_DELETE_TEXT: String = "DELETE"
        const val BUTTON_SPACING: Double = 20.0

        val TEXTBOX_EDITING_COLOR: Color = Color.STEELBLUE
        const val TEXTBOX_EDITING_MIN_SIZE: Double = 10.0
    }


    //FIELDS
    private var _savedText: String = textBox.text
    private var _savedWidth: Double = textBox.width

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
                textBox.text = _savedText
                textBox.width = _savedWidth
            }
            else {
                textBox.text = _textArea.text
                textBox.width = _spinnerSizeText.value
            }
        }
        setOnAction {
            close()
        }
    }
    private val _buttonDelete: Button = Button(BUTTON_DELETE_TEXT).apply {
        hoverProperty().addListener { _, _, newValue ->
            textBox.isStrikethrough = newValue
        }
        setOnAction {
            val parent = textBox.parent as? AnchorPane
            parent?.children?.remove(textBox.apply { frame.hide() })
            close()
        }
    }
    private val _spinnerSizeText: Spinner<Double> = Spinner<Double>(0.0, Double.MAX_VALUE, textBox.width).apply {
        isEditable = true
        editor.textProperty().addListener { _, _, newValue ->
            val size = newValue.toDoubleOrNull()
            if (size != null && size >= TEXTBOX_EDITING_MIN_SIZE)
                textBox.width = size
        }
        valueProperty().addListener { _, _, newValue ->
            if (newValue >= TEXTBOX_EDITING_MIN_SIZE)
                textBox.width = newValue
        }
    }
    private val _textArea: TextArea = TextArea(textBox.text).apply {
        isWrapText = false
        textProperty().addListener { _, _, newValue ->
            textBox.text = newValue
        }
        selectAll()
    }


    init {
        scene = Scene(VBox().apply {
            alignment = Pos.TOP_CENTER
            background = Background(BackgroundFill(Color.STEELBLUE, CornerRadii.EMPTY, Insets.EMPTY))
            children.addAll(
                    _spinnerSizeText,
                    _textArea,
                    HBox().apply {
                        alignment = Pos.CENTER
                        spacing = BUTTON_SPACING
                        children.addAll(_buttonApply, _buttonCancel, _buttonDelete)
                    }
            )
        })
        centerOnScreen()
        setOnCloseRequest { onClose() }
        show()

        textBox.frame.show(TEXTBOX_EDITING_COLOR)
        _textArea.requestFocus()
    }


    //METHODS
    override fun close() {
        onClose()
        super.close()
    }

    private fun onClose() {
        textBox.frame.hide()
    }
}