package com.bleacherreport.velocidapter

const val VELOCIDAPTER_PATH = "com.bleacherreport.velocidapterandroid"
const val VIEW_HOLDER_VIEW_BINDING = "VelocidapterViewHolder"
const val VIEW_HOLDER_VIEW_BINDING_FULL = "$VELOCIDAPTER_PATH.$VIEW_HOLDER_VIEW_BINDING"

const val BIND_INSTRUCTION = """
A ViewBinding extension function that has the @ViewHolder annotation must follow the below steps:
  1) The method must extend a ViewBinding
  2) The next argument must be the data model you bind with (Any)"""

const val VIEW_HOLDER_BIND_INSTRUCTION = """
A Velocidapter ViewHolder class must have one @Bind annotated method which must follow the below steps:
  1) The first argument must the data model you bind with (Any)
  2) /**Optional**/ the Second argument may be (position: Int)
  3) /** Optional**/ the function may extend the ViewBinding of the view Holder"""