package freeSpeech.javafx.component

import freeSpeech.javafx.setOnMouseMoveWhenPressed
import freeSpeech.javafx.view.EditTextView
import freeSpeech.javafx.view.PrimaryView
import freeSpeech.model.DecoratedWord
import freeSpeech.model.TimedText
import freeSpeech.model.millisecondsToPixels
import freeSpeech.model.pixelsToMilliseconds
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import tornadofx.FX.Companion.find
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class TextBox(val timedText: TimedText, heightObservable: ReadOnlyDoubleProperty): Canvas() {


    //FIELDS
    private val _decorators = mutableSetOf<WordDecorator>()
    private var _shownFrameColor: Color? = null


    init {
        timedText.onTextChanged = { _, _ ->
            repaint()
        }

        timedText.onDurationChanged = { _, newValue ->
            width = newValue.millisecondsToPixels()
            repaint()
        }
        width = timedText.duration.millisecondsToPixels()
        height = heightObservable.value
        heightObservable.addListener { _, _, newValue ->
            height = newValue.toDouble()
            repaint()
        }
        DecoratedWord.onAnySizeFactorChanged = { _, _ ->
            repaint()
        }

        timedText.onStartTimeChanged = { _, newValue ->
            layoutX = newValue.millisecondsToPixels()
        }
        layoutX = timedText.startTime.millisecondsToPixels()

        repaint()

        hoverProperty().addListener { _, _, newValue ->
            if (newValue && _shownFrameColor == null)
                showFrame()
            else if (!newValue && _shownFrameColor == FRAME_DEFAULT_COLOR)
                hideFrame()
        }
        cursor = CURSOR

        setOnMouseClicked {
            when (it.button) {
                MouseButton.SECONDARY -> {
                    find<PrimaryView>().openInternalWindow(EditTextView(this))
                }
                else -> {}
            }
            it.consume()
        }
        setOnMouseMoveWhenPressed { eventOnPress, eventOnDrag, deltaX, _ ->
            when (eventOnPress.button) {
                MouseButton.PRIMARY -> {
                    if (eventOnDrag.x < width - FRAME_BORDER_SIZE) {
                        timedText.startTime += deltaX.pixelsToMilliseconds()
                        _decorators.forEach { it.x += deltaX }
                    }
                    else {
                        val result = eventOnDrag.x + FRAME_BORDER_SIZE / 2
                        if (result > EditTextView.TEXTBOX_EDITING_MIN_SIZE)
                            timedText.duration = result.pixelsToMilliseconds()
                    }
                }
                else -> {}
            }
            eventOnPress.consume()
            eventOnDrag.consume()
        }
    }


    //METHODS
    fun repaint() {
        graphicsContext2D.apply {
            (parent as? AnchorPane)?.children?.removeAll(_decorators)
            _decorators.clear()
            clearRect(0.0, 0.0, width, height)
            val defaultFontSize = timedText.text.getFontSizeForWidth(width)
            val spaceSize = " ".getWidthForFontSize(defaultFontSize)
            val wordPerSizeFactorUnit = timedText.count() / timedText.fold(0.0) { acc, word -> acc + word.sizeFactor }
            var x = 0.0
            fill = TEXT_DEFAULT_COLOR
            timedText.forEach { word ->
                val defaultWidth = word.text.getWidthForFontSize(defaultFontSize)
                val wordWidth = defaultWidth * word.sizeFactor * wordPerSizeFactorUnit
                font = Font.font(word.text.getFontSizeForWidth(wordWidth))
                fillText(word.text, x, height / 2, wordWidth)
                _decorators.add(WordDecorator(this@TextBox, word, layoutX + x, wordWidth))
                x += wordWidth + spaceSize
            }
            val frameColor = _shownFrameColor
            if (frameColor != null)
                showFrame(frameColor)
        }
    }
    fun showFrame(color: Color = FRAME_DEFAULT_COLOR) {
        graphicsContext2D.apply {
            fill = color.deriveColor(0.0, 1.0, 1.0, FRAME_FILL_COLOR_OPACITY)
            fillRect(0.0, 0.0, width, height)
            stroke = color
            strokeRect(0.0, 0.0, width, height)
        }
        _shownFrameColor = color
        val parent = (parent as? AnchorPane)
        parent?.children?.addAll(_decorators.filterNot { it in parent.children })
    }
    fun hideFrame() {
        _shownFrameColor = null
        repaint()
    }


    companion object {
        val TEXT_DEFAULT_COLOR: Color = Color.BLACK
        val FRAME_DEFAULT_COLOR: Color = Color.FORESTGREEN
        const val FRAME_FILL_COLOR_OPACITY: Double = 0.4
        const val FRAME_BORDER_SIZE: Double = 24.0

        val CURSOR: Cursor = Cursor.MOVE
    }
}


fun String.getFontSizeForWidth(width: Double): Double {
    return width * 25 / getWidthForFontSize(25.0)
}

fun String.getWidthForFontSize(fontSize: Double): Double {
    return Text(this).apply { font = Font(fontSize) }.layoutBounds.width
}