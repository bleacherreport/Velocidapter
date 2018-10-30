package com.bleacherreport.adaptergenanotations

/**
 * Used to specify the method in an annotated ViewHolder which will be call to bind the specified View
 * to the annotated ViewHolder.
 *
 * Note: method must be public and take an Any and an Int in that specific order. The Any's specific class
 * type will be used to generate add functions for a ScopedDataList class.
 */
annotation class Bind