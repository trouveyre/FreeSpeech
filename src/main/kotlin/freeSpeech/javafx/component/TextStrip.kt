package freeSpeech.javafx.component

import freeSpeech.controler.pixelsToMilliseconds
import freeSpeech.controler.millisecondsToPixels
import freeSpeech.javafx.FreeSpeech
import freeSpeech.javafx.setOnMouseMoveWhenPressed
import freeSpeech.javafx.stage.EditStage
import freeSpeech.model.TimedText
import freeSpeech.view.DocumentSynchronised
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class TextStrip(
        height: Double,
        timeLinePosition: Double
): StackPane(), Iterable<Triple<String, Duration, Duration>>, DocumentSynchronised {


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
    override var currentTime: Duration = Duration.ZERO
        set(value) {
            field = value
            _background.translateX = offset - value.millisecondsToPixels()
        }

    val offset: Double = timeLinePosition


    init {
        alignment = Pos.TOP_LEFT
        background = Background(BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY))
        cursor = CURSOR_NEW_TEXT

        children.addAll(_background, _foreground)

        setOnMouseClicked {
            when (it.button) {
                MouseButton.SECONDARY -> {
                    val x = _background.sceneToLocal(it.sceneX, it.sceneY).x
                    EditStage(write(TimedText(x.pixelsToMilliseconds())))
                }
                else -> {}
            }
        }
        setOnMouseMoveWhenPressed { eventOnPress, eventOnMove, deltaX, _ ->
            when (eventOnPress.button) {
                MouseButton.PRIMARY -> {
                    FreeSpeech.DOCUMENT_OPERATOR.setCurrentTime(currentTime - deltaX.pixelsToMilliseconds(), this)
                }
                else -> {}
            }
            eventOnPress.consume()
            eventOnMove.consume()
        }
    }


    //METHODS
    fun write(timedText: TimedText): TextBox {
        val result = TextBox(timedText, heightProperty())
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
            return Triple(textBox.timedText.text, textBox.timedText.duration, textBox.timedText.startTime)
        }
    }


    companion object {
        const val MIN_WIDTH: Double = FreeSpeech.MIN_WIDTH
        const val MIN_HEIGHT: Double = 1.0
        val MAX_WIDTH: Double = FreeSpeech.MAX_WIDTH
        val MAX_HEIGHT: Double = FreeSpeech.MAX_HEIGHT

        val BACKGROUND_COLOR: Color = Color.WHITE
        val TIME_LINE_COLOR: Color = Color.RED

        val CURSOR_NEW_TEXT: Cursor = Cursor.TEXT
    }
}