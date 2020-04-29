package freeSpeech

import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.util.Duration


class TextStrip(
        height: Double,
        val offset: Double = APPLICATION_WIDTH / 5,
        val baseline: Double = height / 2
): StackPane(), Iterable<Triple<String, Duration, Duration>> {

    companion object {
        const val DEFAULT_PIXEL_PER_MILLIS: Double = 0.3

        const val TEXTBOX_DEFAULT_TEXT: String = "new text here"

        val LINE_CURRENT_TIME_COLOR: Color = Color.RED

        const val RECTANGLE_NEW_TEXT_WIDTH: Double = 300.0
    }


    //FIELDS
    private val _background: AnchorPane = AnchorPane()
    private val _foreground: Canvas = Canvas(APPLICATION_WIDTH, height).apply {
        background = Background(BackgroundFill(Color(0.0, 1.0, 0.0, 0.5), CornerRadii.EMPTY, Insets.EMPTY))
        graphicsContext2D.also {
            it.stroke = LINE_CURRENT_TIME_COLOR
            it.strokeLine(offset, 0.0, offset, height)
        }
        isMouseTransparent = true
    }

    private var recordStartPoint: Duration? = null


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

    var pixelPerMillis: Double = DEFAULT_PIXEL_PER_MILLIS


    init {
        alignment = Pos.TOP_LEFT
        background = Background(BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY))

        children.addAll(_background, _foreground)
        currentTime = Duration.ZERO

        hoverProperty().addListener { _, _, newValue ->
            scene.cursor = if (newValue) Cursor.TEXT else Cursor.DEFAULT
        }
        setOnMouseClicked {
            val x = _background.sceneToLocal(it.sceneX, it.sceneY).x
            EditStage(write(
                    TEXTBOX_DEFAULT_TEXT,
                    duration(RECTANGLE_NEW_TEXT_WIDTH),
                    duration(x)
            ))
        }
    }


    //METHODS
    fun duration(atX: Double): Duration = Duration(atX / pixelPerMillis)
    fun pixelLength(of: Duration): Double = of.toMillis() * pixelPerMillis

    fun write(text: String, of: Duration, at: Duration): TextBox {
        val result = TextBox(text, pixelLength(of), pixelLength(at), baseline)
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