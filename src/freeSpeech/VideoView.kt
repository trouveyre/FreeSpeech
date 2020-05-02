package freeSpeech

import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.stage.FileChooser
import javafx.util.Duration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

class VideoView: StackPane() {

    companion object {
        const val MIN_WIDTH: Double = FreeSpeech.MIN_WIDTH
        const val MIN_HEIGHT: Double = 0.0
        val MAX_WIDTH: Double = FreeSpeech.MAX_WIDTH
        val MAX_HEIGHT: Double = FreeSpeech.MAX_HEIGHT

        const val BUTTON_OPEN_TEXT: String = "OPEN VIDEO"

        val BUTTON_PLAY_COLOR_BACKGROUND: Color = Color(0.0, 0.0, 0.0, 0.5)
        val BUTTON_PLAY_FONT: Font = Font.font("Trebuchet MS", FontWeight.BOLD, 26.0)
        val BUTTON_PLAY_FONT_COLOR: Color = Color.WHITE
        const val BUTTON_PLAY_TEXT_PAUSE: String = "PAUSE"
        const val BUTTON_PLAY_TEXT_PLAY: String = "PLAY"
        const val BUTTON_PLAY_TEXT_REPLAY: String = "REPLAY"

        const val COMBOBOX_VIDEO_RATE_DEFAULT_VALUE: Double = 1.0
        const val COMBOBOX_VIDEO_RATE_TEXT: String = "video speed:"
        val COMBOBOX_VIDEO_RATE_VALUES: Array<Double> = arrayOf(0.5, 1.0, 2.0)

        const val SLIDER_VOLUME_DEFAULT_VALUE: Double = 0.67
    }


    //FIELDS
    private val _buttonOpen = Button(BUTTON_OPEN_TEXT).apply {
        setOnAction {
            val fileChooser = FileChooser().apply {
                try {
                    initialDirectory = File(System.getProperty("user.dir"))
                }
                catch (e: Exception) {}
                extensionFilters.add(FileChooser.ExtensionFilter("video file", "*.mp4"))
            }
            try {
                val file = fileChooser.showOpenDialog(scene.window)
                if (file != null) {
                    openVideo(file.toURI())
                }
            }
            catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private val _buttonPlayPause: Text by lazy {
        Text(BUTTON_PLAY_TEXT_PLAY).apply {
            fill = BUTTON_PLAY_FONT_COLOR
            font = BUTTON_PLAY_FONT
        }
    }
    private val _comboBoxVideoRate: ComboBox<Double> by lazy {
        ComboBox<Double>().apply {
            disableProperty().bind(_mediaView.mediaPlayerProperty().isNull)
            isEditable = false
            items.addAll(COMBOBOX_VIDEO_RATE_VALUES)
            value = COMBOBOX_VIDEO_RATE_DEFAULT_VALUE
            valueProperty().addListener { _, _, newValue ->
                _mediaView.mediaPlayer?.rate = newValue
            }
        }
    }
    private val _controls: BorderPane by lazy {
        BorderPane().apply {
            background = Background(BackgroundFill(BUTTON_PLAY_COLOR_BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY))
            bottom = _sliderVideoTime
            center = _buttonPlayPause
            left = VBox().apply {
                padding = Insets(20.0, 0.0, 0.0, 15.0)
                children.addAll(
                        Text(COMBOBOX_VIDEO_RATE_TEXT).apply {
                            fill = Color.LIGHTGRAY
                        },
                        _comboBoxVideoRate
                )
            }
            right = _sliderVolume
            top = _buttonOpen
            setOnMouseClicked {
                when (it.button) {
                    MouseButton.PRIMARY -> {
                        when (_mediaView.mediaPlayer?.status) {
                            MediaPlayer.Status.PLAYING -> _mediaView.mediaPlayer?.pause()
                            MediaPlayer.Status.STOPPED -> _mediaView.mediaPlayer?.play()
                            else -> _mediaView.mediaPlayer?.play()
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    private val _mediaView: MediaView = MediaView().apply {
        setMinSize(MIN_WIDTH, MIN_HEIGHT)
        setMaxSize(MAX_WIDTH, MAX_HEIGHT)
        isPreserveRatio = true
        isSmooth = true
        mediaPlayerProperty().addListener { _, _, newValue ->
            if (newValue != null)
                newValue.rate = _comboBoxVideoRate.value
        }
    }
    private val _sliderVideoTime: Slider by lazy {
        Slider(0.0, 0.0, 0.0).apply {
            minWidth = MIN_WIDTH
            maxWidth = MAX_WIDTH
            blockIncrement = 0.1
            disableProperty().bind(_mediaView.mediaPlayerProperty().isNull)
            orientation = Orientation.HORIZONTAL
            valueProperty().addListener { _, _, newValue ->
                val time = Duration(newValue.toDouble())
                if (currentTime != time)
                    currentTime = time
            }
        }
    }
    private val _sliderVolume: Slider = Slider(0.0, 1.0, SLIDER_VOLUME_DEFAULT_VALUE).apply {
        minHeight = MIN_HEIGHT
        maxHeight = MAX_HEIGHT
        blockIncrement = 0.01
        orientation = Orientation.VERTICAL
    }


    //PROPERTIES
    var currentTime: Duration
        get() = currentTimeProperty?.value ?: Duration.UNKNOWN
        set(value) {
            _mediaView.mediaPlayer?.seek(value)
        }
    val currentTimeProperty: ReadOnlyProperty<Duration>?
        get() = _mediaView.mediaPlayer?.currentTimeProperty()

    var fitWidth: Double
        get() = fitWidthProperty.value
        set(value) {
            fitWidthProperty.value = value
        }
    val fitWidthProperty: DoubleProperty
        get() = _mediaView.fitWidthProperty()

    var fitHeight: Double
        get() = fitHeightProperty.value
        set(value) {
            fitHeightProperty.value = value
        }
    val fitHeightProperty: DoubleProperty
        get() = _mediaView.fitHeightProperty()

    var preserveRatio: Boolean
        get() = preserveRatioProperty.value
        set(value) {
            preserveRatioProperty.value = value
        }
    val preserveRatioProperty: BooleanProperty
        get() = _mediaView.preserveRatioProperty()

    val source: String?
        get() = _mediaView.mediaPlayer?.media?.source
    var onCloseVideo: (VideoView.(oldPlayer: MediaPlayer?) -> Unit)? = null
    var onOpenVideo: (VideoView.(oldPlayer: MediaPlayer?, newPlayer: MediaPlayer?) -> Unit)? = null


    init {
        children.addAll(_mediaView, _controls)

        hoverProperty().addListener { _, _, newValue ->
            _controls.isVisible = _mediaView.mediaPlayer == null || newValue || _comboBoxVideoRate.isHover
        }
    }


    //METHODS
    fun closeVideo() {
        _mediaView.mediaPlayer?.apply {
            volumeProperty().unbindBidirectional(_sliderVolume.valueProperty())
            dispose()
        }
        onCloseVideo?.invoke(this, _mediaView.mediaPlayer)
        _mediaView.mediaPlayer = null
    }

    fun openVideo(uri: URI): Boolean {
        return try {
            val oldMediaPlayer = _mediaView.mediaPlayer
            closeVideo()
            _mediaView.mediaPlayer = MediaPlayer(Media(uri.toString())).apply {
                currentTimeProperty().addListener { _, _, newValue ->
                    val millis = newValue.toMillis()
                    if (_sliderVideoTime.value != millis)
                        _sliderVideoTime.value = millis
                }
                setOnEndOfMedia {
                    seek(startTime)
                    pause()
                }
                statusProperty().addListener { _, _, newValue ->
                    _buttonPlayPause.text = when (newValue) {
                        MediaPlayer.Status.PLAYING -> BUTTON_PLAY_TEXT_PAUSE
                        MediaPlayer.Status.STOPPED -> BUTTON_PLAY_TEXT_REPLAY
                        else -> BUTTON_PLAY_TEXT_PLAY
                    }
                }
                volume = _sliderVolume.value
                volumeProperty().bindBidirectional(_sliderVolume.valueProperty())
                _sliderVideoTime.also {
                    it.min = startTime.toMillis()
                    GlobalScope.launch {
                        while (stopTime == Duration.UNKNOWN) {
                            val value = it.value
                            if (value > it.max)
                                Platform.runLater {
                                    it.max = value
                                }
                        }
                        Platform.runLater {
                            it.max = stopTime.toMillis()
                        }
                    }
                }
            }
            onOpenVideo?.invoke(this@VideoView, oldMediaPlayer, _mediaView.mediaPlayer)
            true
        }
        catch (e: Exception) {
            false
        }
    }
}