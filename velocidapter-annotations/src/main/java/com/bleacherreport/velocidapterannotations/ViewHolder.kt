package com.bleacherreport.velocidapterannotations

/**
 * Used to flag a ViewBinding for Adapter code generation.
 *
 *      //** Top Level Extension Function **//
 *      @ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
 *      fun ItemNumberBinding.bindTopLevel(item: NumberViewItemBindingExtension) {
 *         textView.text = item.text
 *      }
 *
 *      //** Object Member Extension Function **//
 *      object Test {
 *        @ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
 *        fun ItemNumberBinding.bindTest(item: NumberViewItemBindingMemberFunction) {
 *          textView.text = item.text
 *        }
 *      }
 *
 *      //** View Holder Class **//
 *      @ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
 *      class NumberViewHolder(val binding: ItemNumberBinding) : RecyclerView.ViewHolder(binding.root) {
 *        @Bind
 *        fun bindViewHolder(item: NumberViewItemViewHolder, position: Int) {
 *          binding.bindTopLevel(item.number)
 *        }
 *      }
 *
 * @param adapters array of adapter names to which this ViewHolder belongs. Note: it is reccomended that
 * these names be made constant as a misspelling between two different ViewHolder annotations will generate
 * two different Adapter extension functions
 *
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ViewHolder(vararg val adapters: String, val newBindingSuffix: String = VelociSuffix.VELOCI_NONE)

object VelociSuffix {
    const val VELOCI_NEW = "New"
    const val VELOCI_NONE = "NONE"
}
