package freeSpeech.view.view

import freeSpeech.view.FreeSpeech
import freeSpeech.view.toDoAlert
import freeSpeech.model.Document
import freeSpeech.model.save
import javafx.scene.Parent
import javafx.stage.FileChooser
import tornadofx.*
import java.io.File

class ApplicationMenuView : View() {

    override val root: Parent = menubar {

        menu("File") {

            item("New").action { FreeSpeech.DOCUMENT_OPERATOR.newDocument() }
            item("Open...").action {
                val file = FileChooser().apply {
                    title = "Open ${FreeSpeech.TITLE} file"
                    try {
                        initialDirectory = File(System.getProperty("user.dir"))
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                    extensionFilters.add(FileChooser.ExtensionFilter(
                            "${FreeSpeech.TITLE} file",
                            "*.${Document.EXTENSION}"
                    ))
                }.showOpenDialog(primaryStage)
                if (file != null)
                    FreeSpeech.DOCUMENT_OPERATOR.openDocument(file.absolutePath)
            }
            item("Open Recent").action { toDoAlert() }
            separator()
            item("Save").action { FreeSpeech.DOCUMENT_OPERATOR.document?.save() }
            item("Save As...").action {
                val file = FileChooser().apply {
                    title = "Save As ${FreeSpeech.TITLE} file"
                    try {
                        initialDirectory = File(System.getProperty("user.dir"))
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                    extensionFilters.add(FileChooser.ExtensionFilter(
                            "${FreeSpeech.TITLE} file",
                            "*.${Document.EXTENSION}"
                    ))
                }.showSaveDialog(primaryStage)
                if (file != null)
                    FreeSpeech.DOCUMENT_OPERATOR.document?.apply {
                        pathname = file.absolutePath
                        save()
                    }
            }
            separator()
            item("Preferences").action { toDoAlert() }
            separator()
            item("Exit").action { primaryStage.close() }
        }
        menu("Edit") {

            item("Undo").action { toDoAlert() }
            item("Redo").action { toDoAlert() }
            separator()
            item("Cut").action { toDoAlert() }
            item("Copy").action { toDoAlert() }
            item("Paste").action { toDoAlert() }
            item("Delete").action { toDoAlert() }
            separator()
            item("Select All").action { toDoAlert() }
        }
        menu("View") {

            menu("theme") {

                // TODO
            }
        }
        menu("Navigate") {

            item("Next Text").action { FreeSpeech.DOCUMENT_OPERATOR.nextText() }
            item("Previous Text").action { FreeSpeech.DOCUMENT_OPERATOR.previousText() }
        }
    }
}