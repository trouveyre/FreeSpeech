package freeSpeech.textBox

import freeSpeech.FreeSpeech
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text

class TextBox(
        text: String,
        width: Double,
        x: Double,
        y: Double
): Text(x, y, text) {

    companion object {
        val FRAME_DEFAULT_COLOR: Color = Color.FORESTGREEN
        const val FRAME_FILL_COLOR_OPACITY: Double = 0.4
    }


    //FIELDS
    private val _frame: Rectangle = Rectangle(x, 0.0, width, FreeSpeech.STRIP_HEIGHT).apply {
        fill = Color.TRANSPARENT
        stroke = Color.TRANSPARENT
        hoverProperty().addListener { _, _, newValue ->
            if (!(newValue || _frameLocked))
                hideFrame()
        }
        setOnMouseClicked {
            when (it.button) {
                MouseButton.SECONDARY -> {
                    if (!_frameLocked)
                        EditStage(this@TextBox)
                }
                else -> {}
            }
        }
    }
    private var _frameLocked: Boolean = false

    private var _savedWidth: Double = width


    //PROPERTIES

    var width: Double
        get() = layoutBounds.width
        set(value) {
            font = Font(font.name, value * font.size / layoutBounds.width)
            _frame.width = value
            _savedWidth = value
        }


    init {
        hoverProperty().addListener { _, _, newValue ->
            if (!_frameLocked && newValue) {
                showFrame()
                _frameLocked = false
            }
        }
        textProperty().addListener { _, _, _ ->
            this.width = _savedWidth
        }
        this.width = width
    }


    //METHODS
    fun showFrame(color: Color = FRAME_DEFAULT_COLOR, height: Double = FreeSpeech.STRIP_HEIGHT) {
        if (!_frameLocked) {
            val parent = parent as? AnchorPane
            if (parent?.children?.contains(_frame) == false)
                parent.children.add(_frame)
            if (color != _frame.stroke || height != _frame.height) {
                _frame.also {
                    it.x = x
                    it.y = y - height / 2
                    it.width = width
                    it.height = height
                }.apply {
                    fill = Color(color.red, color.green, color.blue, FRAME_FILL_COLOR_OPACITY)
                    stroke = color
                }
            }
            _frameLocked = true
        }
    }

    fun hideFrame() {
        (parent as? AnchorPane)?.children?.remove(_frame)
        _frameLocked = false
    }
}