package se.familjensmas.pellets

interface Listener<T> {
	fun event(event : T)
}