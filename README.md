# Velocidapter

A 100% Kotlin functional adapter code generation library

The RecyclerView Adapter should do 2 things: inflate your ViewHolders in the right order and bind them with the right data. Maybe you like writing the same boilerplate functions for different ViewHolders and view types, but for everyone else, there's Velocidapter.

Velocidapter writes your Adapters for you, and all you have to give it is ViewHolders and their data.


## Usage

Velocidapter uses kapt annotation processing to generate adapter classes and typesafe lists for you to update View Holders.

### Short Version

For all you folks that just want to jump in, below is an example of a simple screen with one adapter that has two different ViewHolders and data types. The next section breaks this example down.

```
const val MyAdapter = "MyAdapter"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val adapter = recyclerView.withLinearLayoutManager().attachMyAdapter()
        val dataList = MyAdapterDataList()
        dataList.add("hello")
        dataList.add(123)
        adapter.resetData(dataList)
    }
}
```

```
@ViewHolder(adapters = [MyAdapter], layoutResId = R.layout.item_my_string)
class MyStringViewHolder(override val containerView: View) : 
        RecyclerView.ViewHolder(containerView), LayoutContainer {
    
    @Bind
    fun bindModel(data: String, position: Int) {
        textView.text = "$data world"
    }
}
```

```
@ViewHolder(adapters = [MyAdapter], layoutResId = R.layout.item_my_number)
class NumberViewHolder(override val containerView: View) : 
        RecyclerView.ViewHolder(containerView), LayoutContainer {
    
    @Bind
    fun bindModel(data: Int, position: Int) {
        textView.text = "$data 456"
    }
}
```

### Long Version

Decide a name for your adapter. This is the name your generated classes will be based on.

```
const val MyAdapter = "MyAdapter"
```

Next, create your ViewHolder. Your Adapter is bound to one or more Adapters using the `adapters` field in the `@ViewHolder` annotation (you see here we use the string defined above). You also specify the layout this ViewHolder infaltes in the `layoutResId` field.

```
@ViewHolder(adapters = [MyAdapter], layoutResId = R.layout.item_my_view)
class StringViewHolder(override val containerView: View) : 
        RecyclerView.ViewHolder(containerView), LayoutContainer
```

Your ViewHolder needs to have a way to be bound with data. You need to annotate any function within the ViewHolder with `@Bind`. The function can have any name, but must have 2 parameters, the first can be any type and is your only method of binding data to this ViewHolder. The second is an `Int` and indicates this item's position in the Adapter.

```
@Bind
fun bindModel(data: String, position: Int)
```

Now you should be ready to run a quick build of your project, and the Adapter will be generated for you. Now you can bind it to it's RecyclerView, likely somewhere in your activity or fragment. The function `attachMyAdapter()` is generated based on your adapter name

```
val adapter = recyclerView.withLinearLayoutManager().attachMyAdapter()
```

Now of course you need to give this adapter the data it should show. This data should come in a generated type. For this example it's called `MyAdapterDataList` and it's essentially a wrapper around a list.

```
val dataList = MyAdapterDataList()
```

You can only add data to this list that conforms to the data that's able to be bound by the ViewHolders in this Adapter. For this example, since the Adapter only has one ViewHolder, which takes `Strings`, the `add()` functions on this DataList only accepts Strings.

```
dataList.add("hello")
dataList.addListOfString(listOf("hello", "world"))
```

This list is passed to the Adapter using the most basic update function `resetData()`

```
adapter.resetData(dataList)
```

And that's it! And if you wanted to add another ViewHolder that takes `Ints` for instance, you can just build the ViewHolder, bind it to the same Adapter, and start passing in `Ints` to the `MyAdapterDataList`. Easy as that.

## Contribution
Issues are welcome, but we are not currently accepting Pull Requests


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