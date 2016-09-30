package se.familjensmas.pellets

import java.math.BigDecimal

interface ShuntLevelStorage {

    fun readShuntLevel(): BigDecimal

    fun save(shuntLevel: BigDecimal)

}