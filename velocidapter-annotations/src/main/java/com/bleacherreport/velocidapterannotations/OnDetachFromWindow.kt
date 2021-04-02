package com.bleacherreport.velocidapterannotations

/**
 * Optional annotation that can be applied to annotated ViewHolder function to be called when
 * RecyclerView.onViewDetachedFromWindow would be called.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class OnDetachFromWindow