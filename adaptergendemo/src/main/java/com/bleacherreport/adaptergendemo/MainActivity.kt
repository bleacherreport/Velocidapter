package com.bleacherreport.adaptergendemo

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import com.bleacherreport.adaptergen.NumberAdapterDataList
import com.bleacherreport.adaptergen.attachNumberAdapter
import com.bleacherreport.adaptergenandroid.withLinearLayoutManager
import com.bleacherreport.adaptergendemo.singleviewholdertype.NumberBox
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.single -> {
                val target = recyclerView.withLinearLayoutManager().attachNumberAdapter()
                val dataList = NumberAdapterDataList()
                for(i in 0 until 100) dataList.add(NumberBox(i))
                target.resetData(dataList)
                return@OnNavigationItemSelectedListener true
            }
            R.id.multi -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.parent_child -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.diff -> {
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

    companion object {
        const val numberAdapter = "NumberAdapter"
    }
}
