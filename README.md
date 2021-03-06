# Velocidapter

[ ![Download](https://img.shields.io/github/v/release/bleacherreport/Velocidapter?label=Github%20Maven%20Version&logo=github) ](https://github.com/orgs/bleacherreport/packages?repo_name=Velocidapter)

A 100% Kotlin functional adapter code generation library

The RecyclerView Adapter should do 2 things: inflate your ViewHolders in the right order and bind them with the right data. Maybe you like writing the same boilerplate functions for different ViewHolders and view types, but for everyone else, there's Velocidapter.

Velocidapter writes your Adapters for you, and all you have to give it is ViewHolders and their data.

## Installation

Velocidapter is available on GitHub Packages. Replace `{version}` below with&nbsp;  [ ![Download](https://img.shields.io/github/v/release/bleacherreport/Velocidapter?label=) ](https://github.com/orgs/bleacherreport/packages?repo_name=Velocidapter)&nbsp; (remove the prefix `v`).
```groovy
repositories {
    mavenCentral()
}

kapt 'com.bleacherreport.velocidapter:velocidapter:{version}'
implementation 'com.bleacherreport.velocidapter:velocidapter-android:{version}'
```
 If you don't have `kapt` set up in your project already, follow [this](https://kotlinlang.org/docs/reference/kapt.html).
 
## Usage

Velocidapter uses kapt annotation processing to generate adapter classes and type safe lists for you to update View Holders.

### Short Version

For all you folks that just want to jump in, below is an example of a simple screen with one adapter that has two different ViewHolders and data types. The next section breaks this example down.

```kotlin
const val MyAdapter = "MyAdapter"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dataTarget = recyclerView.withLinearLayoutManager()
                                     .attachMyAdapter()
        val dataList = MyAdapterDataList()
        dataList.add("hello")
        dataList.add(123)
        dataTarget.updateDataset(dataList)
    }
}
```

```kotlin
@ViewHolder(adapters = [MyAdapter])
fun MyStringViewBinding.bind(data: String) {
    textView.text = "$data world"
}
```

```kotlin
@ViewHolder(adapters = [MyAdapter])
class NumberViewHolder(val binding : MyNumberViewBinding) : 
        RecyclerView.ViewHolder(binding.root) {
    
    @Bind
    fun bindModel(data: Int) {
        binding.textView.text = "$data 456"
    }
}
```

# Long Version

Decide a name for your adapter. This is the name your generated classes will be based on.

```kotlin
const val MyAdapter = "MyAdapter"
```

### ViewHolder Class Example
-----
Next, create your ViewHolder. Your Adapter is bound to one or more Adapters using the `adapters` field in the `@ViewHolder` annotation (you see here we use the string defined above). You also specify the `ViewBinding` inside the constructor

```kotlin
@ViewHolder(adapters = [MyAdapter])
class StringViewHolder(val binding : MyViewBinding, val alsoInclude : CustomClass) : 
        RecyclerView.ViewHolder(binding.root)
```

You'll notice there is also a paramater of type `CustomClass` in the example above.  You can include as many parameters in the constructor as you'd like, and the auto generated adapter will also require those paramters on creation.

Your ViewHolder needs to have a way to be bound with data. You need to annotate any function within the ViewHolder with `@Bind`. The function can have any name and can take one or two parameters.
1. The first argument must the data model you bind with
2. **Optional** the Second argument may be `position: Int`
3. **Optional** the function may extend the `ViewBinding` of the `ViewHolder`

```kotlin
@Bind
fun MyViewBinding.bindModel(data: StringViewHolderModel, position: Int) {
    textView.text = "${data.text} world"
}
```

### ViewBinding Extension Function Example
-----
Next, create your `ViewBinding` Extension Function. Your Adapter is bound to one or more Adapters using the `adapters` field in the `@ViewHolder` annotation (you see here we use the string defined above).

Your `ViewBinding` extension function can have any name and can take one parameter.
1. The method must extend a `ViewBinding`
2. The next argument must be the data model you bind with

```kotlin
@ViewHolder(adapters = [MyAdapter])
fun MyViewBinding.bindModel(data: StringViewBindingModel) {
    textView.text = "${data.text} world"
}

object ViewBindingsUtil {
    @ViewHolder(adapters = [MyAdapter])
    fun MyViewBinding.bindModel(data: StringViewBindingObjectModel) {
        textView.text = "${data.text} world"
    }
}
```

### Build
-----

Now you should be ready to run a quick build of your project, and the Adapter will be generated for you. Now you can bind it to it's RecyclerView, likely somewhere in your activity or fragment. The function `attachMyAdapter()` is generated based on your adapter name and will return a `AdapterDataTarget`

```kotlin
val dataTarget = recyclerView.withLinearLayoutManager()
                             .attachMyAdapter()
```

Now of course you need to give this adapter the data it should show. This data should come in a generated type. For this example it's called `MyAdapterDataList`, a type safe wrapper around a list.

```kotlin
val dataList = MyAdapterDataList()
```

You can only add data to this list that conforms to the data that's able to be bound by the ViewHolders in this Adapter. For this example, since the Adapter only has one ViewHolder, which takes `Strings`, the `add()` functions on this DataList only accepts Strings.

```kotlin
// ViewHolder Class Example
dataList.addListOfStringViewHolderModel(
    listOf(
        StringViewHolderModel("hello"), 
        StringViewHolderModel("world")
    )
)
// ViewBinding Top Level Extension Function Example
dataList.addListOfStringViewBindingModel(
    listOf(
        StringViewBindingModel("hello"), 
        StringViewBindingModel("world")
    )
)
// ViewBinding Object Member Extension Function Example
dataList.add(StringViewBindingObjectModel("hello"))
dataList.add(StringViewBindingObjectModel("world"))
```

This list is passed to the Adapter using the `AdapterDataTarget` update function `updateDataset()`. This function should be the primary way to update data. For this data set, since it's not `DiffComparable`, it will simply reset the data and call `notifyDataSetChanged()` on the Adapter. For more complex usages, see below.

```kotlin
dataTarget.updateDataset(dataList)
```

And that's it! And if you wanted to add another ViewHolder that takes `Ints` for instance, you can just build the ViewHolder, bind it to the same Adapter, and start passing in `Ints` to the `MyAdapterDataList`. Easy as that.

## AdapterDataTarget Functions

To clear all items from an `AdapterDataTarget` just call `setEmpty()`
```kotlin
dataTarget.setEmpty()
```

To reset all items, just call `resetData()`
```kotlin
dataTarget.resetData(datalist)
```

What if I want to update a few dataset items without resetting the whole list? First we will need all dataset types within the list to implement the `DiffComparable` interface. Our class must implement an `equals()` method that checks for exact internal equality and an `isSame()` method that check for equality via unique identifier

```kotlin
data class DiffPoko(val id : Int, val time: Long) : DiffComparable {
    override fun isSame(that: Any): Boolean {
        return if(that is DiffPoko) {
            id == that.id
        } else {
            false
        }
    }
}
```

We then need to enable list diffing on the `AdapterDataTarget`

```kotlin
val target = recyclerView.withLinearLayoutManager()
                         .attachDiffTypeAdapter()
                         .enableDiff()
```

Once that's enabled, all we need to do is update the list using

```kotlin
target.updateDataset(newDataList)
```

Velocidapter will update, delete, and move items as needed based off of the DiffComparable check. Failure to implement `DiffComparable` for all data types or forgetting to call `enableDiff()` will cause `updateDataset()` to function as `resetData()`

## LiveData

`LiveData` is supported out of the box as well.

```kotlin
val liveData = viewModel.getLiveData()
recyclerView.withLinearLayoutManager()
            .attachMyAdapter()
            .observeLiveData(liveData, lifecycleOwner)
```
or
```kotlin
val observer = recyclerView.withLinearLayoutManager()
                           .attachMyAdapter()
                           .observeLiveDataForever(liveData)
```

## Contribution
Issues and PRs are welcome!


## License
```
Copyright 2018 Bleacher Report

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
