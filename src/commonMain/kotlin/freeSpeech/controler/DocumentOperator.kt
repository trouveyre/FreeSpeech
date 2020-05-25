package freeSpeech.controler

import freeSpeech.model.Document
import freeSpeech.model.TimedText
import freeSpeech.model.load
import freeSpeech.view.DocumentListener
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds


@OptIn(ExperimentalTime::class)
class DocumentOperator {


    //PROPERTIES
    val listeners = mutableSetOf<DocumentListener>()

    private var _document: Document? = null
    val document: Document?
        get() = _document

    var _currentTime: Duration = Duration.ZERO
    val currentTime: Duration
        get() = _currentTime

    var focusedText: TimedText?
        get() = document?.texts?.find { it.startTime <= currentTime && currentTime <= it.stopTime }
        set(value) {
            if (value != null && document?.texts?.contains(value) == true)
                setCurrentTime(value.startTime, null)
        }

    private var _timeJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }
    var leader: DocumentListener? = null

    var onNewDocument: ((Document) -> Unit)? = null
    var onOpenDocument: ((Document) -> Unit)? = null
    var onCloseDocument: ((Document) -> Unit)? = null

    //METHODS
    fun setCurrentTime(value: Duration, setter: DocumentListener?) {
        if (value != _currentTime) {
            _currentTime = value
            listeners.filterNot { it === setter }.forEach {
                it.currentTime = value
            }
        }
    }

    fun newDocument(name: String = Document.DEFAULT_NAME) {
        closeDocument()
        _document = Document(name).also {
            onNewDocument?.invoke(it)
        }
    }
    fun openDocument(pathname: String) {
        closeDocument()
        _document = Document().also {
            it.pathname = pathname
            it.load()
            onOpenDocument?.invoke(it)
        }
    }
    fun closeDocument() {
        val document = _document
        _timeJob = null
        if (document != null) {
            _document = null
            onCloseDocument?.invoke(document)
        }
    }

    fun followLeader() {
        val leader = leader
        if (leader != null) {
            _timeJob = GlobalScope.launch(start = CoroutineStart.LAZY) {
                val thisJob = _timeJob
                while (_timeJob === thisJob) {
                    setCurrentTime(leader.currentTime, leader)
                }
            }
            _timeJob?.start()
        }
    }
    fun stopFollowingLeader() {
        _timeJob = null
    }


    fun nextText(steps: Int = 1) {  // TODO there is a bug to fix
        val sortedTexts = document?.texts?.sortedBy { it.startTime }
        if (sortedTexts != null)
            repeat(steps) {
                focusedText = sortedTexts.find { it.startTime > currentTime }
            }
    }

    fun previousText(steps: Int = 1) {
        val sortedTexts = document?.texts?.sortedByDescending { it.startTime }
        if (sortedTexts != null)
            repeat(steps) {
                focusedText = sortedTexts.find { it.startTime < currentTime }
            }
    }


    companion object {
        const val DEFAULT_READING_SPEED: Double = 0.3

        private var ratioHasChanged = false
        var pixelsPerMillisecond: Double = DEFAULT_READING_SPEED
            set(value) {
                field = value
                ratioHasChanged = true
            }
        var millisecondsPerPixel: Double = 1.0 / DEFAULT_READING_SPEED
            get() {
                if (ratioHasChanged) {
                    field = 1.0 / pixelsPerMillisecond
                    ratioHasChanged = false
                }
                return field
            }
            set(value) {
                pixelsPerMillisecond = 1.0 / value
            }
    }
}

@OptIn(ExperimentalTime::class)
fun Double.pixelsToMilliseconds() = times(DocumentOperator.millisecondsPerPixel).milliseconds

@OptIn(ExperimentalTime::class)
fun Duration.millisecondsToPixels() = inMilliseconds * DocumentOperator.pixelsPerMillisecond