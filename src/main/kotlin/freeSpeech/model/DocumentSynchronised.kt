package freeSpeech.model

import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
interface DocumentSynchronised {


    //PROPERTIES
    var currentTime: Duration
}