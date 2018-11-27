package com.bleacherreport.adaptergenandroid


/**
 * Extended by generated DataList classes to type check all classes passed to Adapter.
 * Note: while this class has "List" in the name it does not implement the List interface, generated
 * child classes will only have adder methods. This is done for the sake of immutability.
 */
abstract class ScopedDataList {

    abstract val isDiffComparable : Boolean
    protected val listInternal = mutableListOf<Any>()

    val list: List<Any>
        get() = listInternal.toList()

    fun isNullOrEmpty(): Boolean = listInternal.isNullOrEmpty()
}