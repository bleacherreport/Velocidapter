package com.bleacherreport.velocidapterannotations

/**
 * Used to flag a ViewBinding for Adapter code generation.
 *
 * @param adapters array of adapter names to which this ViewHolder belongs. Note: it is reccomended that
 * these names be made constant as a misspelling between two different ViewHolder annotations will generate
 * two different Adapter extension functions
 */
@Target(AnnotationTarget.CLASS)
annotation class ViewHolderBind(vararg val adapters: String)
