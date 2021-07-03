package kg.nurik.poligonapp.view

import android.util.LongSparseArray
import androidx.core.util.forEach

fun <T> LongSparseArray<T>.getList(): List<T> {
    val list = ArrayList<T>()
    forEach { _, value ->
        list.add(value)
    }
    return list.toList()
}