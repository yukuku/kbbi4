package yuku.kbbi4.dictdata

import android.util.Log
import com.carrotsearch.hppc.IntArrayList
import com.carrotsearch.hppc.ObjectIntHashMap
import yuku.kbbi4.App
import yuku.kbbi4.dastruk.ValueReader
import java.io.BufferedInputStream
import java.util.*

object Acu {
    const val TAG = "Acu"

    val acus = ArrayList<String>(100000)
    val index_nilai = ObjectIntHashMap<String>()
    val offlens = IntArrayList(100000)

    init {
        val wmulai = System.currentTimeMillis()

        val vr = ValueReader(BufferedInputStream(App.context.assets.open("dictdata/acu_nilai.txt")))

        while (true) {
            val length = vr.readUint8()
            if (length == 0) break
            val nilai = vr.readRawString(length)
            acus.add(nilai)
            index_nilai.put(nilai, acus.size - 1)
        }

        Log.d(TAG, "${acus.size} acus loaded in ${System.currentTimeMillis() - wmulai} ms")
    }

    fun noop() {}

    fun getId(acu: String): Int {
        return index_nilai[acu] + 1
    }

    fun listAcus(prefix: String): List<String> {
        var from = Collections.binarySearch(acus, "$prefix\u0000").inv() - 1
        var to = Collections.binarySearch(acus, "$prefix\uffff").inv()

        if (from < 0) from = 0
        if (to > acus.size) to = acus.size

        return acus.subList(from, to)
    }
}
