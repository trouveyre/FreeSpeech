package freeSpeech

import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.util.Duration
import java.io.File
import java.lang.Exception
import java.net.URI


const val APPLICATION_TITLE: String = "FreeSpeech"
const val APPLICATION_WIDTH: Double = 1200.0
const val APPLICATION_HEIGHT: Double = 700.0
const val STRIP_HEIGHT: Double = 200.0


fun toDoAlert() = Alert(Alert.AlertType.INFORMATION).apply {
    title = "Devs dialog"
    headerText = "Working on progress..."
}.showAndWait()


class FreeRead : Application() {

    companion object {
        const val FILE_ERROR_OPENING_TITLE = "File ERROR"
        const val FILE_ERROR_OPENING_HEADER = "Opening desired file has failed."
        const val FILE_ERROR_OPENING_VIDEO = "Video not found."
        const val FILE_ERROR_SAVING_TITLE = "File ERROR"
        const val FILE_ERROR_SAVING_HEADER = "Saving file has failed."
        const val FILE_EXTENSION: String = "frd"
        const val FILE_FORMAT_SEPARATOR: String = "::"
        const val FILE_SUCCESS_SAVING_TITLE: String = "File saved"
        const val FILE_SUCCESS_SAVING_HEADER: String = "The file has been properly saved."
    }


    //FIELDS
    private lateinit var _rootStage: Stage

    private val _textStrip: TextStrip = TextStrip(STRIP_HEIGHT)
    private val _video = VideoView(APPLICATION_WIDTH, APPLICATION_HEIGHT - STRIP_HEIGHT)

    private var _currentFile: File? = null
        set(value) {
            field = value
            _rootStage.title = APPLICATION_TITLE
            if (value != null)
                _rootStage.title += " *${value.name}*"
        }


    //METHODS
    private fun openVideoSafely(reference: File, paths: Array<String>, testNumber: Int = 0) {
        try {
            _video.openVideo(reference.toURI().resolve(paths[testNumber]))
        }
        catch (e: Exception) {
            if (testNumber < paths.size - 1)
                openVideoSafely(reference, paths, testNumber + 1)
            else
                Alert(Alert.AlertType.ERROR).apply {
                    title = FILE_ERROR_OPENING_TITLE
                    headerText = FILE_ERROR_OPENING_VIDEO
                    contentText = e.message
                }.showAndWait()
        }
    }
    fun open(file: File) {
        _textStrip.clear()
        var isFirstLine = true
        try {
            file.forEachLine {
                if (isFirstLine) {
                    openVideoSafely(file, it.split(FILE_FORMAT_SEPARATOR).toTypedArray())
                    isFirstLine = false
                }
                else {
                    val (text, width, position) = it.split(FILE_FORMAT_SEPARATOR).apply { it.trim() }
                    _textStrip.write(text, Duration.valueOf(width), Duration.valueOf(position))
                }
            }
            _currentFile = file
        }
        catch (e: Exception) {
            _currentFile = null
            Alert(Alert.AlertType.ERROR).apply {
                title = FILE_ERROR_OPENING_TITLE
                headerText = FILE_ERROR_OPENING_HEADER
                contentText = e.message
            }.showAndWait()
        }
    }

    fun save(file: File) {
        val source = _video.source
        if (source != null) {
            try {
                val fileText = StringBuilder()
                fileText.append(source)
                try {
                    fileText.append(FILE_FORMAT_SEPARATOR, File(URI(source)).toRelativeString(File(file.parent)))
                }
                catch (e: Exception) {}
                fileText.append("\n")
                _textStrip.forEach {
                    fileText.append(
                            it.first, FILE_FORMAT_SEPARATOR,
                            it.second.toMillis(), "ms", FILE_FORMAT_SEPARATOR,
                            it.third.toMillis(), "ms", "\n"
                    )
                }
                file.writeText(fileText.toString())
                _currentFile = file
                Alert(Alert.AlertType.INFORMATION).apply {
                    title = FILE_SUCCESS_SAVING_TITLE
                    headerText = FILE_SUCCESS_SAVING_HEADER
                }.showAndWait()
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR).apply {
                    title = FILE_ERROR_SAVING_TITLE
                    headerText = FILE_ERROR_SAVING_HEADER
                    contentText = e.message
                }.showAndWait()
            }
        }
    }

    override fun start(primaryStage: Stage) {
        _rootStage = primaryStage.apply {
            isResizable = false
            width = APPLICATION_WIDTH
            height = APPLICATION_HEIGHT
            x = (Screen.getPrimary().bounds.width - APPLICATION_WIDTH) / 2
            y = 0.0
            title = APPLICATION_TITLE
            scene = Scene(VBox().apply {
                alignment = Pos.TOP_LEFT
                background = Background(BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))
                children.addAll(
                        TopMenu(),
                        _video,
                        _textStrip
                )
            })
        }
        _video.apply {
            onCloseVideo = {
                _textStrip.currentTimeProperty.unbind()
            }
            onOpenVideo = { _, newPlayer ->
                if (newPlayer != null) {
                    _textStrip.currentTimeProperty.bind(currentTimeProperty)
                }
            }
            _rootStage.show()
        }
        open(File("resources\\demo.frd"))
    }

    //NESTED CLASSES
    inner class TopMenu: MenuBar() {

        init {
            menus.addAll(
                    Menu("File").apply {
                        items.addAll(
                                MenuItem("New").apply {
                                    setOnAction {
                                        _currentFile = null
                                        _video.closeVideo()
                                        _textStrip.clear()
                                    }
                                },
                                MenuItem("Open...").apply {
                                    setOnAction {
                                        val file = FileChooser().apply {
                                            title = "Open FreeRead file"
                                            try {
                                                initialDirectory = File(System.getProperty("user.dir"))
                                            }
                                            catch (e: Exception) {}
                                            extensionFilters.add(FileChooser.ExtensionFilter(
                                                    "FreeRead file",
                                                    "*.$FILE_EXTENSION"
                                            ))
                                        }.showOpenDialog(_rootStage)
                                        if (file != null)
                                            open(file)
                                    }
                                },
                                MenuItem("Open Recent").apply {
                                    setOnAction {
                                        toDoAlert()
                                    }
                                },
                                SeparatorMenuItem(),
                                MenuItem("Save").apply {
                                    setOnAction {
                                        val file = _currentFile
                                        if (file != null)
                                            save(file)
                                    }
                                },
                                MenuItem("Save As...").apply {
                                    setOnAction {
                                        val file = FileChooser().apply {
                                            title = "Save As FreeRead file"
                                            try {
                                                initialDirectory = File(System.getProperty("user.dir"))
                                            }
                                            catch (e: Exception) {}
                                            extensionFilters.add(FileChooser.ExtensionFilter(
                                                    "FreeRead file",
                                                    "*.$FILE_EXTENSION"
                                            ))
                                        }.showSaveDialog(_rootStage)
                                        if (file != null)
                                            save(file)
                                    }
                                },
                                SeparatorMenuItem(),
                                MenuItem("Preferences").apply {
                                    setOnAction {
                                        toDoAlert()
                                    }
                                },
                                SeparatorMenuItem(),
                                MenuItem("Quit").apply {
                                    setOnAction {
                                        _rootStage.close()
                                    }
                                }
                        )
                    },
                    Menu("Edit").apply {
                        items.addAll(
                                MenuItem("Undo").apply {
                                    setOnAction {
                                        toDoAlert()
                                    }
                                },
                                MenuItem("Redo").apply {
                                    setOnAction {
                                        toDoAlert()
                                    }
                                },
                                SeparatorMenuItem(),
                                MenuItem("Cut").apply {
                                    setOnAction {
                                        toDoAlert()
                                    }
                                },
                                MenuItem("Copy").apply {
                                    setOnAction {
                                        toDoAlert()
                                    }
                                },
                                MenuItem("Paste").apply {
                                    setOnAction {
                                        toDoAlert()
                                    }
                                },
                                MenuItem("Delete").apply {
                                    setOnAction {
                                        toDoAlert()
                                    }
                                },
                                SeparatorMenuItem(),
                                MenuItem("Select All").apply {
                                    setOnAction {
                                        toDoAlert()
                                    }
                                }
                        )
                    }
            )
        }
    }
}

fun main(vararg args: String) {
    Application.launch(FreeRead::class.java, *args)
}