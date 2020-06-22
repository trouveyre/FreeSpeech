package freeSpeech.model

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds


@OptIn(ExperimentalTime::class)
class TimedText(
        start: Duration,
        time: Duration = DEFAULT_DURATION,
        private val words: MutableList<DecoratedWord> = defaultWords
): AbstractMutableList<DecoratedWord>() {

    //PROPERTIES
    var duration: Duration = time
        set(value) {
            val oldValue = field
            field = value
            onDurationChanged?.invoke(oldValue, value)
        }
    var startTime: Duration = start
        set(value) {
            val oldValue = field
            field = value
            onStartTimeChanged?.invoke(oldValue, value)
        }
    var stopTime: Duration
        get() = startTime + duration
        set(value) {
            val oldValue = stopTime
            duration = value - startTime
            onStopTimeChanged?.invoke(oldValue, value)
        }

    var text: String
        get() = words.fold("") { acc, word -> "$acc${word.text} " }.dropLast(1)
        set(value) {
            val oldValue = text
            val words = value.split(Regex(" +")).filter { it.isNotEmpty() }
            removeAll(drop(words.size))
            words.forEachIndexed { index, word ->
                val box = getOrNull(index)
                if (box == null)
                    add(index, DecoratedWord(word))
                else if (box.text != word)
                    box.text = word
            }
            onTextChanged?.invoke(oldValue, value)
        }

    var onDurationChanged: ((oldValue: Duration, newValue: Duration) -> Unit)? = null
    var onStartTimeChanged: ((oldValue: Duration, newValue: Duration) -> Unit)? = null
    var onStopTimeChanged: ((oldValue: Duration, newValue: Duration) -> Unit)? = null
    var onTextChanged: ((oldValue: String, newValue: String) -> Unit)? = null

    override val size: Int
        get() = words.size


    //METHODS
    override fun add(index: Int, element: DecoratedWord) = words.add(index, element)
    override fun removeAt(index: Int) = words.removeAt(index)
    override fun get(index: Int) = words[index]
    override fun set(index: Int, element: DecoratedWord) = words.set(index, element)

    override fun toString(): String {
        val result = StringBuilder()
        val size = words.size - 1
        for (i in 0 until size)
            result.append("${words[i]} ")
        result.append("${words[size]}$SEPARATOR${duration.inMilliseconds}$SEPARATOR${startTime.inMilliseconds}")
        return result.toString()
    }


    companion object {
        val DEFAULT_DURATION = 2.seconds
        val defaultWords: MutableList<DecoratedWord>
            get() = mutableListOf(DecoratedWord("new"), DecoratedWord("text"), DecoratedWord("here"))

        const val SEPARATOR = ">>"
    }
}

@OptIn(ExperimentalTime::class)
fun String.toTimedText(): TimedText {
    val elements = split(TimedText.SEPARATOR)
    return TimedText(elements[2].toDouble().milliseconds, elements[1].toDouble().milliseconds).also {
        it.clear()
        it.addAll(elements[0].split(" ").map { word -> word.toDecoratedWord() })
    }
}