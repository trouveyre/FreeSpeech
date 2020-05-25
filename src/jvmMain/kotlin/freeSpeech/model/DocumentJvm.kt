package freeSpeech.model

import freeSpeech.javafx.FreeSpeech
import javafx.scene.control.Alert
import java.io.File


actual fun Document.load() {
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

actual fun Document.save() {
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