package freeSpeech.view.component

import freeSpeech.model.TimedText
import freeSpeech.model.millisecondsToPixels
import freeSpeech.model.pixelsToMilliseconds
import freeSpeech.view.setOnMouseMoveWhenPressed
import freeSpeech.view.view.EditTextView
import freeSpeech.view.view.PrimaryView
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.geometry.Insets
import javafx.scene.Cursor
import javafx.scene.input.MouseButton
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import tornadofx.*
import tornadofx.FX.Companion.find
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class TextBox(val timedText: TimedText, heightObservable: ReadOnlyDoubleProperty): HBox() {

    private val _textArea = anchorpane {
        cursor = TEXT_AREA_CURSOR
        setOnMouseMoveWhenPressed { eventOnPress, eventOnDrag, deltaX, _ ->
            when (eventOnPress.button) {
                MouseButton.PRIMARY -> timedText.startTime += deltaX.pixelsToMilliseconds()
                else -> {
                }
            }
            eventOnPress.consume()
            eventOnDrag.consume()
        }
    }

    private val _stretchingArea = rectangle {
        cursor = STRETCHING_AREA_CURSOR
        fill = Color.TRANSPARENT
        stroke= Color.TRANSPARENT
        width = STRETCHING_AREA_WIDTH
        heightProperty().bind(heightObservable)
        setOnMouseMoveWhenPressed { eventOnPress, eventOnDrag, deltaX, _ ->
            when (eventOnPress.button) {
                MouseButton.PRIMARY -> timedText.duration += deltaX.pixelsToMilliseconds()
                else -> {}
            }
            eventOnPress.consume()
            eventOnDrag.consume()
        }
    }


    init {
        timedText.onTextChanged = { _, _ ->
            drawText()
        }
        timedText.onDurationChanged = { _, _ ->
            drawText()
        }

        timedText.onStartTimeChanged = { _, newValue ->
            layoutX = newValue.millisecondsToPixels()
        }
        layoutX = timedText.startTime.millisecondsToPixels()

        onHover { isHovering ->
            if (isHovering)
                showFrame()
            else
                hideFrame()
        }
        setOnMouseClicked {
            when (it.button) {
                MouseButton.SECONDARY -> {
                    find<PrimaryView>().openInternalWindow(
                            EditTextView(this)
                    )
                }
                else -> {}
            }
            it.consume()
        }

        drawText()
    }


    private fun drawText() {
        _textArea.children.clear()
        val totalWidth = timedText.duration.millisecondsToPixels()
        val defaultFontSize = timedText.text.getFontSizeForWidth(totalWidth)
        val spaceSize = " ".getWidthForFontSize(defaultFontSize)
        val wordPerSizeFactorUnit = timedText.count() / timedText.fold(0.0) { acc, word -> acc + word.sizeFactor }
        var x = 0.0
        timedText.forEach { word ->
            val wordFontSize = defaultFontSize * word.sizeFactor * wordPerSizeFactorUnit
            _textArea.add(WordBox(word).apply {
                fill = TEXT_DEFAULT_COLOR
                font = Font.font(wordFontSize)
                this.x = x
                this.y = _stretchingArea.height / 2
                decoratedWord.onSizeFactorChanged = { _, _ -> drawText() }
            })
            x += word.text.getWidthForFontSize(wordFontSize) + spaceSize
        }
    }

    fun showFrame(color: Color = FRAME_DEFAULT_COLOR) {
        if (background?.equals(Background.EMPTY) != false || background?.fills?.first()?.fill == FRAME_DEFAULT_COLOR.deriveColor(0.0, 1.0, 1.0, FRAME_FILL_COLOR_OPACITY))
            _textArea.background = Background(BackgroundFill(
                    color.deriveColor(0.0, 1.0, 1.0, FRAME_FILL_COLOR_OPACITY),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            ))
    }
    fun hideFrame() {
        _textArea.background = Background.EMPTY
    }


    companion object {
        val FRAME_DEFAULT_COLOR: Color = Color.FORESTGREEN
        const val FRAME_FILL_COLOR_OPACITY: Double = 0.6

        val STRETCHING_AREA_CURSOR: Cursor = Cursor.H_RESIZE
        const val STRETCHING_AREA_WIDTH: Double = 5.0

        val TEXT_AREA_CURSOR: Cursor = Cursor.MOVE

        val TEXT_DEFAULT_COLOR: Color = Color.BLACK
    }
}


fun String.getFontSizeForWidth(width: Double, reference: Double = 100.0): Double {
    return width * reference / getWidthForFontSize(reference)
}

fun String.getWidthForFontSize(fontSize: Double): Double {
    return Text(this).apply { font = Font(fontSize) }.layoutBounds.width
}
