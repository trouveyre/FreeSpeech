package freeSpeech

import javafx.beans.property.DoubleProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider
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

class VideoView(
        width: Double,
        height: Double
): StackPane() {

    companion object {
        const val BUTTON_PAUSE_TEXT = "PAUSE"
        const val BUTTON_PLAY_TEXT = "PLAY"
        const val BUTTON_REPLAY_TEXT = "REPLAY"
    }


    //FIELDS
    private val _buttonOpen = Button("OPEN VIDEO").apply {
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
    private val _buttonPlayPause: Button by lazy {
        Button(BUTTON_PLAY_TEXT).apply {
            background = Background(BackgroundFill(
                    Color(0.0, 0.0, 0.0, 0.5),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            ))
            disableProperty().bind(_mediaView.mediaPlayerProperty().isNull)
            font = Font.font("Trebuchet MS", FontWeight.BOLD, 26.0)
            isDefaultButton = true
            setMaxSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
            prefWidthProperty().bind(_mediaView.fitWidthProperty())
            prefHeightProperty().bind(_mediaView.fitHeightProperty())
            setOnAction {
                when (_mediaView.mediaPlayer?.status) {
                    MediaPlayer.Status.PLAYING -> _mediaView.mediaPlayer?.pause()
                    MediaPlayer.Status.STOPPED -> _mediaView.mediaPlayer?.play()
                    else -> _mediaView.mediaPlayer?.play()
                }
            }
            textFill = Color.WHITE
        }
    }
    private val _comboVideoSpeed: ComboBox<Double> by lazy {
        ComboBox<Double>().apply {
            disableProperty().bind(_mediaView.mediaPlayerProperty().isNull)
            isEditable = false
            items.addAll(
                    0.5,
                    1.0,
                    2.0
            )
            value = items[1]
            valueProperty().addListener { _, _, newValue ->
                _mediaView.mediaPlayer?.rate = newValue
            }
        }
    }
    private val _controls: BorderPane by lazy {
        BorderPane().apply {
            bottom = _sliderVideoTime
            center = _buttonPlayPause
            left = VBox().apply {
                padding = Insets(20.0, 0.0, 0.0, 12.0)
                children.addAll(
                        Text("video speed:").apply {
                            fill = Color.LIGHTGRAY
                        },
                        _comboVideoSpeed
                )
            }
            right = _sliderVolume
            top = _buttonOpen
        }
    }
    private val _mediaView: MediaView = MediaView().apply {
        fitWidth = width
        fitHeight = height
        isPreserveRatio = true
        isSmooth = true
        mediaPlayerProperty().addListener { _, _, newValue ->
            if (newValue != null)
                newValue.rate = _comboVideoSpeed.value
        }
    }
    private val _sliderVideoTime: Slider by lazy {
        Slider(0.0, 0.0, 0.0).apply {
            blockIncrement = 0.1
            disableProperty().bind(_mediaView.mediaPlayerProperty().isNull)
            orientation = Orientation.HORIZONTAL
            valueProperty().addListener { _, _, newValue ->
                val time = Duration(newValue.toDouble())
                if (currentTimeProperty.value != time)
                    currentTimeProperty.value = time
            }
        }
    }
    private val _sliderVolume: Slider = Slider(0.0, 1.0, 0.67).apply {
        blockIncrement = 0.01
        orientation = Orientation.VERTICAL
    }


    //PROPERTIES
    val fitWidthProperty: DoubleProperty
        get() = _mediaView.fitWidthProperty()
    val fitHeightProperty: DoubleProperty
        get() = _mediaView.fitHeightProperty()

    var currentTime: Duration
        get() = currentTimeProperty.value
        set(value) {
            currentTimeProperty.value = value
        }
    val currentTimeProperty: Property<Duration> = SimpleObjectProperty(Duration.ZERO).apply {
        addListener { _, _, newValue ->
            val millis = newValue.toMillis()
            if (_sliderVideoTime.value != millis)
                _sliderVideoTime.value = millis
            if (_mediaView.mediaPlayer?.currentTime ?: newValue != newValue)
                _mediaView.mediaPlayer?.seek(newValue)
        }
    }
    val startTime: Duration?
        get() = _mediaView.mediaPlayer?.startTime
    val stopTime: Duration?
        get() = _mediaView.mediaPlayer?.stopTime

    var onCloseVideo: (VideoView.(oldPlayer: MediaPlayer?) -> Unit)? = null
    var onOpenVideo: (VideoView.(oldPlayer: MediaPlayer?, newPlayer: MediaPlayer?) -> Unit)? = null

    val source: String?
        get() = _mediaView.mediaPlayer?.media?.source


    init {
        children.addAll(_mediaView, _controls)

        hoverProperty().addListener { _, _, newValue ->
            _controls.isVisible = _mediaView.mediaPlayer == null || newValue || _comboVideoSpeed.isHover
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

    fun openVideo(uri: URI) {
        val oldMediaPlayer = _mediaView.mediaPlayer
        closeVideo()
        _mediaView.mediaPlayer = MediaPlayer(Media(uri.toString())).apply {
            setOnEndOfMedia {
                stop()
            }
            statusProperty().addListener { _, _, newValue ->
                _buttonPlayPause.text = when (newValue) {
                    MediaPlayer.Status.PLAYING -> BUTTON_PAUSE_TEXT
                    MediaPlayer.Status.STOPPED -> BUTTON_REPLAY_TEXT
                    else -> BUTTON_PLAY_TEXT
                }
            }
            volume = _sliderVolume.value
            volumeProperty().bindBidirectional(_sliderVolume.valueProperty())
            _sliderVideoTime.also {
                it.min = startTime.toMillis()
                GlobalScope.launch {
                    while (stopTime == Duration.UNKNOWN)
                        it.max = it.value + 5000
                    it.max = stopTime.toMillis()
                }
            }
            this@VideoView.currentTime = currentTime
        }
        GlobalScope.launch {
            while (_mediaView.mediaPlayer != null) {
                val mediaPlayer = _mediaView.mediaPlayer
                if (mediaPlayer != null) {
                    currentTime = mediaPlayer.currentTime
                }
            }
        }
        onOpenVideo?.invoke(this, oldMediaPlayer, _mediaView.mediaPlayer)
        _controls.isVisible = _controls.isHover
    }
}