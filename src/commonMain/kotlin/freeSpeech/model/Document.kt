package freeSpeech.model

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
            val result = Regex("(.+/)?(.*?)(\\.$EXTENSION)?$").find(value)
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

expect fun Document.load()

expect fun Document.save()