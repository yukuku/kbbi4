package yuku.kbbi4.dictdata

import android.util.TimingLogger
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

    /**
     * Each element is:
     * 8 bit file_no
     * 24 bit offset
     */
    val offlens = IntArrayList(100000)

    init {
        val tl = TimingLogger(TAG, "mulai")

        run {
            val vr = ValueReader(BufferedInputStream(App.context.assets.open("dictdata/acu_nilai.txt")))

            while (true) {
                val length = vr.readUint8()
                if (length == 0) break
                val nilai = vr.readRawString(length)
                acus.add(nilai)
                index_nilai.put(nilai, acus.size - 1)
            }

            tl.addSplit("${acus.size} acus loaded")
        }

        run {
            val vr = ValueReader(BufferedInputStream(App.context.assets.open("dictdata/acu_offlens.txt")))
            var file_no = -1
            var offset = 0

            while (true) {
                val length = vr.readVarint()
                if (length == 0xffff) {
                    file_no++
                    offset = 0
                    continue
                }

                if (length == 0x0) {
                    break
                }

                offlens.add(file_no shl 24 or offset)
                offset += length
            }

            tl.addSplit("${acus.size} offlens loaded")
        }

        tl.dumpToLog()
    }

    fun noop() {}

    fun getId(acu: String): Int {
        return index_nilai[acu] + 1
    }

    fun getAcu(id: Int): String {
        return acus[id - 1]
    }

    fun listAcus(prefix: String): List<String> {
        var from = Collections.binarySearch(acus, "$prefix\u0000").inv() - 1
        var to = Collections.binarySearch(acus, "$prefix\uffff").inv()

        if (from < 0) from = 0
        if (to > acus.size) to = acus.size

        return acus.subList(from, to)
    }

    fun getRenderer(acu: String): Renderer {
        val offlen = offlens[index_nilai[acu]]
        return Renderer(offlen shr 24, offlen and 0xffffff)
    }
}
