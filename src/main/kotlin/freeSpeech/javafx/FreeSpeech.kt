package freeSpeech.javafx

import freeSpeech.javafx.view.PrimaryView
import freeSpeech.model.DocumentOperator
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.input.MouseEvent
import tornadofx.App
import java.nio.file.Paths
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class FreeSpeech : App(PrimaryView::class) {

    //METHODS
    override fun stop() {
        DOCUMENT_OPERATOR.stopFollowingLeader()
    }


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
        const val INFO_BAR_HEIGHT: Double = 20.0
        const val TIME_BAR_OFFSET: Double = MIN_WIDTH

        const val FILE_ERROR_OPENING_TITLE = "File ERROR"
        const val FILE_ERROR_OPENING_HEADER = "Opening desired file has failed."
        const val FILE_SUCCESS_SAVING_TITLE: String = "File saved"
        const val FILE_SUCCESS_SAVING_HEADER: String = "The file has been properly saved."
    }
}


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