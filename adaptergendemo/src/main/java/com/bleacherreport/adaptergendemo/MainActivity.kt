package com.bleacherreport.adaptergendemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import com.bleacherreport.adaptergen.*
import com.bleacherreport.adaptergenandroid.AdapterDataTarget
import com.bleacherreport.adaptergenandroid.enableDiff
import com.bleacherreport.adaptergenandroid.withLinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var updateThread: MainActivity.UpdateThread? = null

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        updateThread?.interrupt()
        when (item.itemId) {
            R.id.single -> {
                val target = recyclerView.withLinearLayoutManager().attachSingleAdapter()
                val dataList = SingleAdapterDataList()
                for (i in 0 until 100) dataList.add(i)
                target.resetData(dataList)
                return@OnNavigationItemSelectedListener true
            }
            R.id.multi -> {
                val target = recyclerView.withLinearLayoutManager().attachMultiAdapter()
                val dataList = MultiAdapterDataList()
                for (i in 0 until 100) {
                    dataList.add(i)
                    dataList.add("$i string")
                }
                target.resetData(dataList)
                return@OnNavigationItemSelectedListener true
            }
            R.id.parent_child -> {
                val target = recyclerView.withLinearLayoutManager().attachParentChildAdapter()
                val dataList = ParentChildAdapterDataList()
                for (i in 0 until 100) {
                    dataList.add(ChildPoko(i.toString()))
                    dataList.add(ParentPoko(i.toString()))
                }
                target.resetData(dataList)
                return@OnNavigationItemSelectedListener true
            }
            R.id.diff -> {
                val target = recyclerView.withLinearLayoutManager().attachDiffTypeAdapter().enableDiff()
                updateThread = UpdateThread(target)
                updateThread?.start()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    // Yeah this is a crude way to do threading, just used for example
    class UpdateThread(val target: AdapterDataTarget<DiffTypeAdapterDataList>) : Thread() {
        override fun run() {
            super.run()
            val map = HashMap<Int, DiffPoko>();
            while (true) {
                for (i in 0 until 10) {
                    map.put(i, DiffPoko(i, System.currentTimeMillis()))

                    val dataList = DiffTypeAdapterDataList()
                    dataList.addListOfDiffPoko(map.values.toList())

                    Handler(Looper.getMainLooper()).post {
                        target.updateDataset(dataList)
                    }

                    try {
                        Thread.sleep(100)
                    } catch (ex: InterruptedException) {
                        return
                    }
                }
            }
        }
    }

    companion object {
        const val SingleViewHolderTypeAdapter = "SingleAdapter"
        const val MultiViewHolderTypeAdapter = "MultiAdapter"
        const val ParentAndChildHolderTypeAdapter = "ParentChildAdapter"
        const val DiffTypeAdapter = "DiffTypeAdapter"
    }
}
