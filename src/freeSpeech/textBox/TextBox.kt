package freeSpeech.textBox

import freeSpeech.setOnMouseMoveWhenPressed
import freeSpeech.textStrip.TextStrip
import javafx.scene.Cursor
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
        y: (TextStrip) -> Double,
        var strip: TextStrip
): Text(x, y(strip), text) {

    companion object {
        val FRAME_DEFAULT_COLOR: Color = Color.FORESTGREEN
        const val FRAME_FILL_COLOR_OPACITY: Double = 0.4
        const val FRAME_BORDER_SIZE: Double = 24.0

        val CURSOR_MOVE: Cursor = Cursor.MOVE
        val CURSOR_SIZE: Cursor = Cursor.H_RESIZE
    }


    //FIELDS
    private var _savedWidth: Double = width


    //PROPERTIES
    val frame: Frame = Frame()

    var width: Double
        get() = layoutBounds.width
        set(value) {
            font = Font(font.name, value * font.size / layoutBounds.width)
            frame.width = value
            _savedWidth = value
        }


    init {
        hoverProperty().addListener { _, _, newValue ->
            if (newValue) {
                frame.show(lock = false)
            }
        }
        textProperty().addListener { _, _, _ ->
            this.width = _savedWidth
        }
        this.width = width
        strip.heightProperty().addListener { _, _, _ ->
            this.y = y(strip)
        }
    }


    //INNER CLASSES
    inner class Frame: Rectangle(x, 0.0, width, strip.height) {


        //FIELDS
        private val _rightBorder: Rectangle = Rectangle(FRAME_BORDER_SIZE, computeRightBorderHeight()).apply {
            cursor = CURSOR_SIZE
            fill = Color.TRANSPARENT
            stroke = Color.TRANSPARENT
            setOnMouseMoveWhenPressed { eventOnPress, eventOnMove, deltaX, _ ->
                if (deltaX > FRAME_BORDER_SIZE - frame.width) {
                    this@TextBox.width += deltaX
                }
                eventOnPress.consume()
                eventOnMove.consume()
            }
            hoverProperty().addListener { _, _, newValue ->
                if (!(newValue || _isLocked))
                    hide()
            }
        }
        private var _isLocked: Boolean = false


        //PROPERTIES
        val isLocked: Boolean
            get() = _isLocked


        init {
            xProperty().bind(this@TextBox.xProperty())
            widthProperty().addListener { _, _, newValue ->
                if (newValue.toDouble() > FRAME_BORDER_SIZE - width)
                    _rightBorder.x = computeRightBorderX()
            }
            heightProperty().addListener { _, _, _ ->
                _rightBorder.height = computeRightBorderHeight()
            }
            xProperty().addListener { _, _, _ ->
                _rightBorder.x = computeRightBorderX()
            }
            yProperty().addListener { _, _, _ ->
                _rightBorder.y = computeRightBorderY()
            }
            _rightBorder.apply {
                x = computeRightBorderX()
                y = computeRightBorderY()
            }
            hoverProperty().addListener { _, _, newValue ->
                if (!(newValue || _isLocked || _rightBorder.isHover))
                    hide()
            }
            cursor = CURSOR_MOVE
            setOnMouseClicked {
                when (it.button) {
                    MouseButton.SECONDARY -> {
                        if (!frame.isLocked)
                            EditStage(this@TextBox)
                    }
                    else -> {}
                }
                it.consume()
            }
            setOnMouseMoveWhenPressed { eventOnPress, eventOnMove, deltaX, _ ->
                this@TextBox.x += deltaX
                eventOnPress.consume()
                eventOnMove.consume()
            }
        }


        //METHODS
        fun show(color: Color = FRAME_DEFAULT_COLOR, lock: Boolean = true) {
            if (!_isLocked) {
                _isLocked = lock
                if (color != stroke) {
                    fill = Color(color.red, color.green, color.blue, FRAME_FILL_COLOR_OPACITY)
                    stroke = color
                }
                if (width != this@TextBox.width)
                    width = this@TextBox.width
                if (height != this@TextBox.strip.height)
                    height = this@TextBox.strip.height
                val parent = this@TextBox.parent as? AnchorPane
                if (parent?.children?.contains(this) == false)
                    parent.children?.addAll(this, _rightBorder)
            }
        }

        fun hide() {
            (this@TextBox.parent as? AnchorPane)?.children?.removeAll(_rightBorder, this)
            _isLocked = false
            if (this@TextBox.isHover)
                show(lock = false)
        }

        private fun computeRightBorderHeight() = height
        private fun computeRightBorderX() = x + width - _rightBorder.width / 2
        private fun computeRightBorderY() = y
    }
}