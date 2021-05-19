package com.bleacherreport.velocidapterannotations

annotation class VelocidapterOptions(
    val testInflations: Boolean,
    val testInflationSuffix: String = "",
    val createInflationhelpersSuffix: String = "DONT_CREATE",
)