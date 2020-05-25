package freeSpeech.javafx

import freeSpeech.controler.DocumentOperator
import freeSpeech.javafx.component.InfoBar
import freeSpeech.javafx.component.TextStrip
import freeSpeech.javafx.component.VideoView
import freeSpeech.model.Document
import freeSpeech.model.save
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.nio.file.Paths
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds


fun toDoAlert() = Alert(Alert.AlertType.INFORMATION).apply {
    title = "Devs dialog"
    headerText = "Work in progress..."
}.showAndWait()


fun Node.setOnMouseMoveWhenPressed(
        action: (eventOnPress: MouseEvent, eventOnDrag: MouseEvent, deltaX: Double, deltaY: Double) -> Unit
) {
    lateinit var manage: (MouseEvent) -> Unit
    setOnMousePressed { mousePressedEvent ->
        var x = mousePressedEvent.screenX
        var y = mousePressedEvent.screenY
        manage = {
            action(mousePressedEvent, it, it.screenX - x, it.screenY - y)
            x = it.screenX
            y = it.screenY
        }
    }
    setOnMouseDragged {
        manage(it)
    }
}


@OptIn(ExperimentalTime::class)
class FreeSpeech : Application() {

    companion object {
        val DOCUMENT_OPERATOR: DocumentOperator = DocumentOperator()

        const val TITLE: String = "FreeSpeech"
        const val DEFAULT_WIDTH: Double = 1200.0
        const val DEFAULT_HEIGHT: Double = 800.0
        const val MIN_WIDTH: Double = 200.0
        const val MIN_HEIGHT: Double = 400.0
        val MAX_WIDTH: Double = Double.MAX_VALUE
        val MAX_HEIGHT: Double = Double.MAX_VALUE
        const val STRIP_DEFAULT_HEIGHT: Double = 200.0
        const val INFO_BAR_HEIGHT: Double = 30.0
        const val TIME_BAR_OFFSET: Double = MIN_WIDTH

        const val FILE_ERROR_OPENING_TITLE = "File ERROR"
        const val FILE_ERROR_OPENING_HEADER = "Opening desired file has failed."
        const val FILE_SUCCESS_SAVING_TITLE: String = "File saved"
        const val FILE_SUCCESS_SAVING_HEADER: String = "The file has been properly saved."
    }


    //FIELDS
    private lateinit var _rootStage: Stage

    private val _video = VideoView().apply {
        setMinSize(MIN_WIDTH, 0.0)
        setMaxSize(MAX_WIDTH, MAX_HEIGHT)
        widthProperty().addListener { _, _, newValue ->
            val ratio = ratio
            if (ratio != null && width < height * ratio)
                fitWidth = newValue.toDouble()
        }
        heightProperty().addListener { _, _, newValue ->
            val ratio = ratio
            if (ratio != null && width > height * ratio)
                fitHeight = newValue.toDouble()
        }
        DOCUMENT_OPERATOR.listeners.add(this)
        DOCUMENT_OPERATOR.leader = this
    }
    private val _textStrip: TextStrip = TextStrip(STRIP_DEFAULT_HEIGHT, TIME_BAR_OFFSET).also {
        DOCUMENT_OPERATOR.listeners.add(it)
    }
    private val _mainPane = SplitPane().apply {
        background = Background(BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))
        orientation = Orientation.VERTICAL
        items.addAll(_video, _textStrip)
        setDividerPositions(0.8)
        VBox.setVgrow(this, Priority.ALWAYS)
    }

    private val _infoBar = InfoBar(INFO_BAR_HEIGHT, TIME_BAR_OFFSET).also {
        DOCUMENT_OPERATOR.listeners.add(it)
    }


    //METHODS
    override fun init() {
        DOCUMENT_OPERATOR.apply {
            onOpenDocument = {
                it.video.first { path ->
                    try {
                        _video.openVideo(Paths.get("").toAbsolutePath().toUri().resolve(path))
                    }
                    catch (e: Exception) {
                        false
                    }
                }
                it.lines.first().apply {
                    forEach { timedText ->
                        Platform.runLater {
                            _textStrip.write(timedText)
                        }
                    }
                }
            }
            onCloseDocument = {
                _video.closeVideo()
                _textStrip.clear()
            }
        }
        _video.apply {
            onOpenVideo = { _, newPlayer ->
                if (newPlayer != null)
                    DOCUMENT_OPERATOR.setCurrentTime(newPlayer.currentTime.toMillis().milliseconds, _video)
            }
            onPlayVideo = { _ ->
                DOCUMENT_OPERATOR.followLeader()
            }
            onPauseVideo = { _ ->
                DOCUMENT_OPERATOR.stopFollowingLeader()
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
                        topMenu(),
                        _mainPane,
                        _infoBar
                )

                setOnDragOver {
                    if (it.dragboard.hasFiles() && it.dragboard.files.any { file -> file.extension == Document.EXTENSION })
                        it.acceptTransferModes(*TransferMode.ANY)
                    it.consume()
                }
                setOnDragDropped {
                    var success = false
                    val document = it.dragboard.files.firstOrNull { file ->
                        file.extension == Document.EXTENSION
                    }
                    if (document != null) {
                        DOCUMENT_OPERATOR.openDocument(document.absolutePath)
                        success = true
                    }
                    it.isDropCompleted = success
                    it.consume()
                }
            })
        }
        _rootStage.show()
    }

    override fun stop() {
        DOCUMENT_OPERATOR.stopFollowingLeader()
    }

    private fun topMenu() = MenuBar().apply {
        menus.addAll(
                Menu("File").apply {
                    items.addAll(
                            MenuItem("New").apply {
                                setOnAction {
                                    DOCUMENT_OPERATOR.newDocument()
                                }
                            },
                            MenuItem("Open...").apply {
                                setOnAction {
                                    val file = FileChooser().apply {
                                        title = "Open $TITLE file"
                                        try {
                                            initialDirectory = File(System.getProperty("user.dir"))
                                        }
                                        catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        extensionFilters.add(FileChooser.ExtensionFilter(
                                                "$TITLE file",
                                                "*.${Document.EXTENSION}"
                                        ))
                                    }.showOpenDialog(_rootStage)
                                    if (file != null)
                                        DOCUMENT_OPERATOR.openDocument(file.absolutePath)
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
                                    DOCUMENT_OPERATOR.document?.save()
                                }
                            },
                            MenuItem("Save As...").apply {
                                setOnAction {
                                    val file = FileChooser().apply {
                                        title = "Save As $TITLE file"
                                        try {
                                            initialDirectory = File(System.getProperty("user.dir"))
                                        }
                                        catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        extensionFilters.add(FileChooser.ExtensionFilter(
                                                "$TITLE file",
                                                "*.${Document.EXTENSION}"
                                        ))
                                    }.showSaveDialog(_rootStage)
                                    if (file != null)
                                        DOCUMENT_OPERATOR.document?.apply {
                                            pathname = file.absolutePath
                                            save()
                                        }
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
                },
                Menu("Navigate").apply {
                    items.addAll(
                            MenuItem("next line").apply {
                                setOnAction {
                                    DOCUMENT_OPERATOR.nextText()
                                }
                            },
                            MenuItem("previous line").apply {
                                setOnAction {
                                    DOCUMENT_OPERATOR.previousText()
                                }
                            }
                    )
                }
        )
    }
}

fun main(vararg args: String) {
    Application.launch(FreeSpeech::class.java, *args)
}