package com.bleacherreport.velocidapterannotations

/**
 * Used to flag a ViewBinding for Adapter code generation.
 *
 * @param adapters array of adapter names to which this ViewHolder belongs. Note: it is reccomended that
 * these names be made constant as a misspelling between two different ViewHolder annotations will generate
 * two different Adapter extension functions
 * @param isClassMethod defaults false.  Mark as true if this annotation is on a method in a class
 * For Example,
 */
//        object Test {
//           @ViewBinding(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter], isClassMethod = true)
//           fun bind(itemNumberBinding: ItemNumberBinding, number: Int) {
//               itemNumberBinding.textView.text = number.toString() + " bind method"
//           }
//           }
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ViewHolder(vararg val adapters: String, val isClassMethod: Boolean = false)
