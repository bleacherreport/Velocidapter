package com.bleacherreport.adaptergendemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.bleacherreport.adaptergen.*
import com.bleacherreport.adaptergenandroid.AdapterDataTarget
import com.bleacherreport.adaptergenandroid.enableDiff
import com.bleacherreport.adaptergenandroid.withLinearLayoutManager
import com.bleacherreport.adaptergendemo.diff.DiffPoko
import com.bleacherreport.adaptergendemo.parentchild.ChildPoko
import com.bleacherreport.adaptergendemo.parentchild.ParentPoko
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var updateThread: MainActivity.UpdateThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener { item ->
            updateThread?.interrupt()
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
        for (i in 0 until 100) dataList.add(i)
        target.resetData(dataList)
        return true
    }

    private fun viewMulti(): Boolean {
        description.setText(R.string.description_multi)
        val target = recyclerView.withLinearLayoutManager().attachMultiAdapter()
        val dataList = MultiAdapterDataList()
        for (i in 0 until 100) {
            dataList.add(i)
            dataList.add("$i string")
        }
        target.resetData(dataList)
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
        target.resetData(dataList)
        return true
    }

    private fun viewDiff(): Boolean {
        description.setText(R.string.description_diff)
        val target = recyclerView.withLinearLayoutManager().attachDiffTypeAdapter().enableDiff()
        updateThread = UpdateThread(target)
        updateThread?.start()
        return true
    }

    // Yeah this is a crude way to do threading, just used for example
    private class UpdateThread(val target: AdapterDataTarget<DiffTypeAdapterDataList>) : Thread() {
        override fun run() {
            super.run()
            val map = HashMap<Int, DiffPoko>()
            var i = 0
            while (true) {
                map[i] = DiffPoko(i, System.currentTimeMillis())

                val dataList = DiffTypeAdapterDataList()
                dataList.addListOfDiffPoko(map.values.toList())

                Handler(Looper.getMainLooper()).post {
                    target.updateDataset(dataList)
                }

                try {
                    Thread.sleep(500)
                } catch (ex: InterruptedException) {
                    return
                }
                i++
                if (i == 10) i = 0
            }
        }
    }

    companion object {
        const val SINGLE_VIEW_HOLDER_TYPE_ADAPTER = "SingleAdapter"
        const val MULTI_VIEW_HOLDER_TYPE_ADAPTER = "MultiAdapter"
        const val PARENT_AND_CHILD_VIEW_HOLDER_TYPE_ADAPTER = "ParentChildAdapter"
        const val DIFF_TYPE_ADAPTER = "DiffTypeAdapter"
    }
}
