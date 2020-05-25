package freeSpeech.model


class DecoratedWord(text: String) {

    //PROPERTIES
    var text: String = text
        set(value) {
            if (field != value) {
                val oldValue = field
                field = value
                onTextChanged?.invoke(oldValue, value)
            }
        }
    var sizeFactor: Double = 1.0
        set(value) {
            if (field != value) {
                val oldValue = field
                field = value
                onSizeFactorChanged?.invoke(oldValue, value)
                onAnySizeFactorChanged?.invoke(this, oldValue)
            }
        }

    var onTextChanged: ((oldValue: String, newValue: String) -> Unit)? = null
    var onSizeFactorChanged: ((oldValue: Double, newValue: Double) -> Unit)? = null


    //METHODS
    override fun toString() = "$text${if (sizeFactor == 1.0) "" else "$SEPARATOR$sizeFactor"}"


    companion object {
        const val SEPARATOR = "::"

        var onAnySizeFactorChanged: ((decoratedWord: DecoratedWord, oldValue: Double) -> Unit)? = null
    }
}


fun String.toDecoratedWord(): DecoratedWord {
    val elements = split(DecoratedWord.SEPARATOR)
    val result = DecoratedWord(elements.first())
    if (elements.size > 1) {
        val sizeFactor = elements[1].toDoubleOrNull()
        if (sizeFactor != null)
            result.sizeFactor = sizeFactor
    }
    return result
}