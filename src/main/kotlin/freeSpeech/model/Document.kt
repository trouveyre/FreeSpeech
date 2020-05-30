package freeSpeech.model

import freeSpeech.javafx.FreeSpeech
import javafx.scene.control.Alert
import java.io.File

data class Document(
        var name: String = DEFAULT_NAME,
        var path: String? = null,
        val video: MutableSet<String> = mutableSetOf(),
        val lines: MutableList<MutableList<TimedText>> = ArrayList()
) {

    //PROPERTIES
    val texts: List<TimedText>
        get() = lines.reduce { acc, mutableList -> (acc + mutableList).toMutableList() }

    var pathname: String
        get() = "$path$name.$EXTENSION"
        set(value) {
            val result = Regex("(.+\\\\)?([^\\\\]+?)(\\.$EXTENSION)?$").find(value)
            println(result?.groupValues)
            if (result != null) {
                name = result.groupValues[2]
                path = result.groupValues[1]
            }
        }
    var content: String
        get() {
            val result = StringBuilder(video.reduce { acc, str -> acc + SEPARATOR + str })
            lines.foldIndexed(mutableListOf<Pair<TimedText, Int>>()) { index, acc, mutableList ->
                acc.apply { mutableList.forEach { add(it to index) } }
            }.forEach {
                result.append("\n${it.second}${SEPARATOR}${it.first}")
            }
            return result.toString()
        }
        set(value) {
            setLines(value.split("\n"))
        }


    //METHODS
    fun setLines(stringLines: Collection<String>) {
        lines.clear()
        var isFirstLine = true
        val lineFormat = Regex("^[0-9]+${SEPARATOR}.+$")
        stringLines.forEach { line ->
            if (isFirstLine) {
                video.apply {
                    clear()
                    addAll(line.split(SEPARATOR))
                }
                isFirstLine = false
            }
            else {
                var lineNumber = 0
                val timedText = if (lineFormat.matches(line)) {
                    val strings = line.split(SEPARATOR)
                    lineNumber = strings[0].toInt()
                    strings[1].toTimedText()
                }
                else
                    line.toTimedText()
                lines.getOrElse(lineNumber) { _ ->
                    repeat(lineNumber - lines.size) { index ->
                        mutableListOf<TimedText>().also { lines.add(lines.size + index, it) }
                    }
                    mutableListOf<TimedText>().also { lines.add(lineNumber, it) }
                }.add(timedText)
            }
        }
    }

    override fun toString(): String {
        return "Document($pathname) {\n\t${content.replace("\n", "\n\t")}\n}"
    }


    companion object {
        const val DEFAULT_NAME = "new_file"
        const val EXTENSION: String = "fsw"

        const val SEPARATOR: String = "~~"
    }
}

fun Document.load() {
    try {
        setLines(File(pathname).readLines())
    }
    catch (e: Exception) {
        e.printStackTrace()
        Alert(Alert.AlertType.ERROR).apply {
            title = FreeSpeech.FILE_ERROR_OPENING_TITLE
            headerText = FreeSpeech.FILE_ERROR_OPENING_HEADER
            contentText = e.message
        }.showAndWait()
    }
}

fun Document.save() {
    val file: File? = try {
        File(pathname)
    }
    catch (e: Exception) {
        e.printStackTrace()
        null
    }
    if (file != null) {
        file.writeText(content)
        Alert(Alert.AlertType.INFORMATION).apply {
            title = FreeSpeech.FILE_SUCCESS_SAVING_TITLE
            headerText = FreeSpeech.FILE_SUCCESS_SAVING_HEADER
        }.showAndWait()
    }
}