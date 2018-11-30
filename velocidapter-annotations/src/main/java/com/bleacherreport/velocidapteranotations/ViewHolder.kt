package com.bleacherreport.velocidapteranotations

/**
 * Used to flag a ViewHolder class for Adapter code generation.
 *
 * Note: this must extend RecyclerView.ViewHolder or a child class for generated code to compile.
 *
 * @param adapters array of adapter names to which this ViewHolder belongs. Note: it is reccomended that
 * these names be made constant as a misspelling between two different ViewHolder annotations will generate
 * two different Adapter extension functions
 * @param layoutResId layout resource id which will be inflated and bound to the ViewHolder class
 */
annotation class ViewHolder(vararg val adapters: String, val layoutResId: Int)