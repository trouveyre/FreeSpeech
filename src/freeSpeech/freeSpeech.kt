package freeSpeech

import freeSpeech.textStrip.TextStrip
import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.util.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.net.URI


fun toDoAlert() = Alert(Alert.AlertType.INFORMATION).apply {
    title = "Devs dialog"
    headerText = "Working on progress..."
}.showAndWait()


fun Node.setOnMouseMoveWhenPressed(
        action: (eventOnPress: MouseEvent, eventOnDrag: MouseEvent, deltaX: Double, deltaY: Double) -> Unit
) {
    var x: Double? = null
    var y: Double? = null
    var onPress: MouseEvent? = null
    val manage: (MouseEvent) -> Unit = {
        val x0 = x
        val y0 = y
        val eventOnPress = onPress
        if (x0 != null && y0 != null && eventOnPress != null)
            action(eventOnPress, it, it.x - x0, it.y - y0)
        x = it.x
        y = it.y
    }
    setOnMousePressed {
        x = it.x
        y = it.y
        onPress = it
    }
    setOnMouseDragged { manage(it) }
}


class FreeSpeech : Application() {

    companion object {
        const val TITLE: String = "FreeSpeech"
        const val EXTENSION: String = "fsw"
        const val DEFAULT_WIDTH: Double = 1200.0
        const val DEFAULT_HEIGHT: Double = 800.0
        const val MIN_WIDTH: Double = 200.0
        const val MIN_HEIGHT: Double = 400.0
        val MAX_WIDTH: Double = Double.MAX_VALUE
        val MAX_HEIGHT: Double = Double.MAX_VALUE
        const val STRIP_MIN_HEIGHT: Double = 200.0

        const val FILE_ERROR_OPENING_TITLE = "File ERROR"
        const val FILE_ERROR_OPENING_HEADER = "Opening desired file has failed."
        const val FILE_ERROR_OPENING_VIDEO = "Video not found."
        const val FILE_ERROR_SAVING_TITLE = "File ERROR"
        const val FILE_ERROR_SAVING_HEADER = "Saving file has failed."
        const val FILE_FORMAT_SEPARATOR: String = "::"
        const val FILE_SUCCESS_SAVING_TITLE: String = "File saved"
        const val FILE_SUCCESS_SAVING_HEADER: String = "The file has been properly saved."
    }


    //FIELDS
    private lateinit var _rootStage: Stage

    private val _textStrip: TextStrip = TextStrip(STRIP_MIN_HEIGHT)
    private val _video = VideoView()

    private val _mainPane: SplitPane = SplitPane().apply {
        background = Background(BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))
        orientation = Orientation.VERTICAL
        items.addAll(_video, _textStrip)
        setDividerPositions(0.8)
        VBox.setVgrow(this, Priority.ALWAYS)
    }

    private var _currentFile: File? = null
        set(value) {
            field = value
            _rootStage.title = TITLE
            if (value != null)
                _rootStage.title += " *${value.name}*"
        }


    //METHODS
    private fun openVideoQuickly(reference: File, paths: Array<String>) = runBlocking { //TODO should be faster
        val success = Channel<Boolean>()
        val opening = launch {
            paths.map {
                launch {
                    val t = reference.toURI().resolve(it)
                    if (_video.openVideo(t)) {
                        success.send(true)
                    }
                }
            }.joinAll()
            success.send(false)
        }
        if (success.receive())
            opening.cancel()
        else
            Alert(Alert.AlertType.ERROR).apply {
                title = FILE_ERROR_OPENING_TITLE
                headerText = FILE_ERROR_OPENING_VIDEO
            }.showAndWait()
    }
    fun open(file: File) {
        _textStrip.clear()
        var isFirstLine = true
        try {
            file.forEachLine {
                if (isFirstLine) {
                    openVideoQuickly(file, it.split(FILE_FORMAT_SEPARATOR).toTypedArray())
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
            width = DEFAULT_WIDTH
            height = DEFAULT_HEIGHT
            centerOnScreen()
            title = TITLE
            scene = Scene(VBox().apply {
                alignment = Pos.TOP_LEFT
                isFillWidth = true
                children.addAll(
                        TopMenu(),
                        _mainPane
                )
            })
        }
        _video.apply {
            fitWidth = width
            fitWidthProperty.bind(_rootStage.widthProperty())   //TODO
//            fitHeight = height
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
                                                    "*.$EXTENSION"
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
                                                    "*.$EXTENSION"
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
    Application.launch(FreeSpeech::class.java, *args)
}