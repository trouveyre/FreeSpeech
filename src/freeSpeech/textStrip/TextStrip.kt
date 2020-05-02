package freeSpeech.textStrip

import freeSpeech.FreeSpeech
import freeSpeech.setOnMouseMoveWhenPressed
import freeSpeech.textBox.EditStage
import freeSpeech.textBox.TextBox
import javafx.beans.property.DoubleProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.util.Duration


class TextStrip(
        height: Double,
        timeLinePosition: Double
): StackPane(), Iterable<Triple<String, Duration, Duration>> {

    companion object {
        const val MIN_WIDTH: Double = FreeSpeech.MIN_WIDTH
        const val MIN_HEIGHT: Double = 1.0
        val MAX_WIDTH: Double = FreeSpeech.MAX_WIDTH
        val MAX_HEIGHT: Double = FreeSpeech.MAX_HEIGHT

        val BACKGROUND_COLOR: Color = Color.WHITE
        val TIME_LINE_COLOR: Color = Color.RED

        const val DEFAULT_PIXEL_PER_MILLIS: Double = 0.3

        val CURSOR_NEW_TEXT: Cursor = Cursor.TEXT

        const val TEXTBOX_DEFAULT_TEXT: String = "new text here"
        const val TEXTBOX_DEFAULT_WIDTH: Double = 300.0
    }


    //FIELDS
    private val _background: AnchorPane = AnchorPane()
    private val _foreground: Canvas = Canvas(FreeSpeech.DEFAULT_WIDTH, height).apply {
        setMinSize(MIN_WIDTH, MIN_HEIGHT)
        setMaxSize(MAX_WIDTH, MAX_HEIGHT)
        isMouseTransparent = true
        graphicsContext2D.apply {
            stroke = TIME_LINE_COLOR
            strokeLine(offset, 0.0, offset, height)
        }
        heightProperty().apply {
            addListener { _, _, _ ->
                graphicsContext2D.also {
                    it.clearRect(0.0, 0.0, width, this@TextStrip.height)
                    it.stroke = TIME_LINE_COLOR
                    it.strokeLine(offset, 0.0, offset, this@TextStrip.height)
                }
            }
            bind(this@TextStrip.heightProperty())
        }
    }


    //PROPERTIES
    var currentTime: Duration
        get() = currentTimeProperty.value
        set(value) {
            currentTimeProperty.value = value
        }
    val currentTimeProperty: Property<Duration> = SimpleObjectProperty<Duration>().apply {
        addListener { _, _, newValue ->
            _background.translateX = offset - pixelLength(newValue)
        }
    }

    val offset: Double = timeLinePosition

    var pixelPerMillis: Double = DEFAULT_PIXEL_PER_MILLIS


    init {
        alignment = Pos.TOP_LEFT
        background = Background(BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY))
        cursor = CURSOR_NEW_TEXT

        children.addAll(_background, _foreground)
        currentTime = Duration.ZERO

        setOnMouseClicked {
            when (it.button) {
                MouseButton.PRIMARY -> {
                    val x = _background.sceneToLocal(it.sceneX, it.sceneY).x
                    EditStage(write(TEXTBOX_DEFAULT_TEXT, duration(TEXTBOX_DEFAULT_WIDTH), duration(x)))
                }
                else -> {}
            }
        }
        setOnMouseMoveWhenPressed { eventOnPress, eventOnMove, deltaX, _ ->
            when (eventOnPress.button) {
                MouseButton.MIDDLE -> {
                    currentTime = currentTime.subtract(duration(deltaX))
                }
                else -> {}
            }
            eventOnPress.consume()
            eventOnMove.consume()
        }
    }


    //METHODS
    fun duration(atX: Double): Duration = Duration(atX / pixelPerMillis)
    fun pixelLength(of: Duration): Double = of.toMillis() * pixelPerMillis

    fun write(text: String, of: Duration, at: Duration): TextBox {
        val result = TextBox(text, pixelLength(of), pixelLength(at), { it.height / 2 },this)
        _background.children.add(result)
        return result
    }

    fun clear() = _background.children.clear()

    override fun iterator() = object : Iterator<Triple<String, Duration, Duration>> {
        private val textBoxes = _background.childrenUnmodifiable.filterIsInstance<TextBox>().toMutableList()

        override fun hasNext() = textBoxes.isNotEmpty()

        override fun next(): Triple<String, Duration, Duration> {
            val textBox = textBoxes.first()
            textBoxes.remove(textBox)
            return Triple(textBox.text, duration(textBox.width), duration(textBox.x))
        }
    }
}