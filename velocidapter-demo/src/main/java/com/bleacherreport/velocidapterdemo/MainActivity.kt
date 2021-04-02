package com.bleacherreport.velocidapterdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bleacherreport.velocidapter.*
import com.bleacherreport.velocidapterandroid.enableDiff
import com.bleacherreport.velocidapterandroid.observeLiveData
import com.bleacherreport.velocidapterandroid.withLinearLayoutManager
import com.bleacherreport.velocidapterdemo.diff.DiffPoko
import com.bleacherreport.velocidapterdemo.parentchild.ChildPoko
import com.bleacherreport.velocidapterdemo.parentchild.ParentPoko
import com.bleacherreport.velocidapterdemo.single.NumberViewItemBindingExtension
import com.bleacherreport.velocidapterdemo.single.NumberViewItemBindingMemberFunction
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val viewModel = MainViewModel()
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener { item ->
            job?.cancel()
            when (item.itemId) {
                R.id.single -> viewSingle()
                R.id.multi -> viewMulti()
                R.id.parent_child -> viewParentChild()
                R.id.diff -> viewDiff()
                else -> false
            }
        }
        navigation.selectedItemId = R.id.single
    }

    private fun viewSingle(): Boolean {
        description.setText(R.string.description_single)
        val target = recyclerView.withLinearLayoutManager().attachSingleAdapter()
        val dataList = SingleAdapterDataList()
        for (i in 0 until 100) dataList.add(NumberViewItemBindingMemberFunction(i.toString()))
        dataList.addListOfNumberViewItemBindingExtension(listOf(101,
            102,
            103).map { NumberViewItemBindingExtension(it.toString()) })
        target.updateDataset(dataList)
        return true
    }

    private fun viewMulti(): Boolean {
        description.setText(R.string.description_multi)
        val liveData = viewModel.multiLiveData
        recyclerView.withLinearLayoutManager()
                .attachMultiAdapter()
                .observeLiveData(liveData, this)
        viewModel.updateData()
        return true
    }

    private fun viewParentChild(): Boolean {
        description.setText(R.string.description_parent_child)
        val target = recyclerView.withLinearLayoutManager().attachParentChildAdapter()
        val dataList = ParentChildAdapterDataList()
        for (i in 0 until 100) {
            dataList.add(ChildPoko(i.toString()))
            dataList.add(ParentPoko(i.toString()))
        }
        target.updateDataset(dataList)
        return true
    }

    private fun viewDiff(): Boolean {
        description.setText(R.string.description_diff)
        val target = recyclerView.withLinearLayoutManager().attachDiffTypeAdapter().enableDiff()
        val map = HashMap<Int, DiffPoko>()
        job = GlobalScope.launch(Dispatchers.Main) {
            var i = 0
            while (true) {
                map[i] = DiffPoko(i, System.currentTimeMillis())

                val dataList = DiffTypeAdapterDataList()
                dataList.addListOfDiffPoko(map.values.toList())
                target.updateDataset(dataList)

                delay(500)
                i++
                if (i == 10) i = 0
            }
        }
        return true
    }

    companion object {
        const val SingleAdapter = "SingleAdapter"
        const val MultiAdapter = "MultiAdapter"
        const val ParentChildAdapter = "ParentChildAdapter"
        const val DiffTypeAdapter = "DiffTypeAdapter"
    }
}
