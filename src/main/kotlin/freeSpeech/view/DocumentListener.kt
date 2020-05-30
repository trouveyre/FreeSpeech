package freeSpeech.view

import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
interface DocumentListener {


    //PROPERTIES
    var currentTime: Duration
}