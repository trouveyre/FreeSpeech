package freeSpeech.javafx.component

import freeSpeech.model.DecoratedWord
import javafx.scene.Cursor
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import kotlin.math.round

class WordDecorator(
        val textBox: TextBox,
        val decoratedWord: DecoratedWord,
        x: Double,
        wordWidth: Double
) : Text(computeText(decoratedWord)) {


    init {
        font = Font(FONT_SIZE)
        fill = COLOR_TEXT

        this.x = x + (wordWidth - layoutBounds.width) / 2
        this.y = layoutBounds.height

        cursor = CURSOR
        setOnScroll {
            decoratedWord.sizeFactor += it.deltaY * SCROLL_SPEED
        }

        hoverProperty().addListener { _, _, newValue ->
            if (newValue)
                textBox.showFrame()
            else
                textBox.hideFrame()
        }
    }


    companion object {
        const val FONT_SIZE: Double = 16.0
        val COLOR_TEXT: Color = Color(0.0, 0.0, 0.0, 0.7)
        val COLOR_BACKGROUND: Color = Color(0.0, 0.0, 0.0, 0.6)
        val CURSOR: Cursor = Cursor.V_RESIZE
        const val SCROLL_SPEED: Double = 0.005

        private fun computeText(word: DecoratedWord): String = round(word.sizeFactor * 10).div(10).toString()
    }
}