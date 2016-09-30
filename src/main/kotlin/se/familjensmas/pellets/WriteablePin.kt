package se.familjensmas.pellets

/**
 * Created by jorgen on 2016-09-18.
 */
interface WriteablePin {

    fun set(state: OutputState)

    fun low() = set(OutputState.DOWN)

    fun high() = set(OutputState.UP)

}