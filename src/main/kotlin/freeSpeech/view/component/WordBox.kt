package freeSpeech.view.component

import freeSpeech.model.DecoratedWord
import javafx.scene.text.Text

class WordBox(val decoratedWord: DecoratedWord) : Text(decoratedWord.text) {

    init {
        setOnScroll {
            decoratedWord.sizeFactor += it.deltaY * SCROLL_SPEED
        }
    }


    companion object {

        const val SCROLL_SPEED: Double = 0.005
    }
}