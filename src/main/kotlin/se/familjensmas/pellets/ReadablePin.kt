package se.familjensmas.pellets

/**
 * Created by jorgen on 2016-09-26.
 */
interface ReadablePin {

    enum class PinState {
        HIGH,
        LOW
    }

    fun read() : PinState

    fun addListener(listener: Listener<PinState>)

}