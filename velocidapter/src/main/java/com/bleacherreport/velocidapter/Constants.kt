package com.bleacherreport.velocidapter

const val VELOCIDAPTER_PATH = "com.bleacherreport.velocidapterandroid"
const val VIEW_HOLDER_VIEW_BINDING = "VelocidapterViewHolder"
const val VIEW_HOLDER_VIEW_BINDING_FULL = "$VELOCIDAPTER_PATH.$VIEW_HOLDER_VIEW_BINDING"

const val VIEW_BIND_INSTRUCTION = """
A Velocidapter ViewBinding method must have one @ViewHolderBind annotated method which must follow the below steps:
  1) The method must extend a ViewBinding or pass a ViewBinding as the first argument
  2) The next argument must be the data model you bind with (Any)"""