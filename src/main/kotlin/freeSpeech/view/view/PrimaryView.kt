package freeSpeech.view.view

import freeSpeech.view.FreeSpeech
import freeSpeech.view.component.InfoBar
import freeSpeech.view.component.TextStrip
import freeSpeech.view.component.VideoPlayer
import freeSpeech.model.Document
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.input.TransferMode
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import tornadofx.*
import java.nio.file.Paths
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
class PrimaryView: View() {

    //FIELDS
    private val _textStrip = TextStrip(FreeSpeech.STRIP_DEFAULT_HEIGHT, FreeSpeech.TIME_BAR_OFFSET).also {
        FreeSpeech.DOCUMENT_OPERATOR.listeners.add(it)
    }
    private val _videoPlayer = VideoPlayer().apply {
        setMinSize(FreeSpeech.MIN_WIDTH, 0.0)
        setMaxSize(FreeSpeech.MAX_WIDTH, FreeSpeech.MAX_HEIGHT)
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
        FreeSpeech.DOCUMENT_OPERATOR.listeners.add(this)
        FreeSpeech.DOCUMENT_OPERATOR.leader = this
    }


    //PROPERTIES
    override val root: Parent = vbox(alignment = Pos.TOP_LEFT) {

        add(find<ApplicationMenuView>())

        splitpane(orientation = Orientation.VERTICAL) {

            add(_videoPlayer)

            add(_textStrip)

            background = Background(BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))
            vboxConstraints {
                setDividerPositions(0.8)
                vGrow = Priority.ALWAYS
            }
        }

        add(InfoBar(FreeSpeech.INFO_BAR_HEIGHT, FreeSpeech.TIME_BAR_OFFSET).also {
            FreeSpeech.DOCUMENT_OPERATOR.listeners.add(it)
        })

        isFillWidth = true
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
                FreeSpeech.DOCUMENT_OPERATOR.openDocument(document.absolutePath)
                success = true
            }
            it.isDropCompleted = success
            it.consume()
        }
    }


    init {
        primaryStage.apply {
            width = FreeSpeech.DEFAULT_WIDTH
            height = FreeSpeech.DEFAULT_HEIGHT
            centerOnScreen()
            title = FreeSpeech.TITLE
        }

        FreeSpeech.DOCUMENT_OPERATOR.apply {
            onOpenDocument = {
                it.video.first { path ->
                    try {
                        val docPath = it.path ?: Paths.get(".").toString()
                        val resolvedPath = Paths.get(docPath).toAbsolutePath().toUri().resolve(path)
                        _videoPlayer.openVideo(resolvedPath)
                    }
                    catch (e: Exception) {
                        false
                    }
                }
                it.lines.first().apply {
                    forEach { timedText ->
                        _textStrip.write(timedText)
                    }
                }
            }
            onCloseDocument = {
                _videoPlayer.closeVideo()
                _textStrip.clear()
            }
        }
        _videoPlayer.apply {
            onOpenVideo = { _, newPlayer ->
                if (newPlayer != null)
                    FreeSpeech.DOCUMENT_OPERATOR.setCurrentTime(newPlayer.currentTime.toMillis().milliseconds, _videoPlayer)
            }
            onPlayVideo = { _ ->
                FreeSpeech.DOCUMENT_OPERATOR.followLeader()
            }
            onPauseVideo = { _ ->
                FreeSpeech.DOCUMENT_OPERATOR.stopFollowingLeader()
            }
        }
    }
}