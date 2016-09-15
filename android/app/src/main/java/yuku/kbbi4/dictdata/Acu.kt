package yuku.kbbi4.dictdata

import android.util.Log
import android.util.TimingLogger
import yuku.kbbi4.App
import yuku.kbbi4.dastruk.ValueReader
import java.io.BufferedInputStream
import java.util.*

object Acu {
    const val TAG = "Acu"

    private val acus by lazy {
        val tl = TimingLogger(TAG, "load acus")

        val vr = ValueReader(BufferedInputStream(App.context.assets.open("dictdata/acu_nilai.txt"), 200000))

        val size = vr.readVarint()
        val res = Array(size) {
            val length = vr.readUint8()
            vr.readRawString(length)
        }

        tl.addSplit("$size acus loaded")
        tl.dumpToLog()

        res
    }

    /**
     * Each element is:
     * 8 bit file_no
     * 24 bit offset
     */
    private val offlens by lazy {
        val tl = TimingLogger(TAG, "load offlens")

        val vr = ValueReader(BufferedInputStream(App.context.assets.open("dictdata/acu_offlens.txt"), 200000))
        val size = vr.readVarint()
        var file_no = -1
        var offset = 0
        val res = IntArray(size) {
            var length = vr.readVarint()
            if (length == 0xffff) {
                file_no++
                offset = 0

                length = vr.readVarint()
            }

            val res = file_no shl 24 or offset
            offset += length
            res
        }

        tl.addSplit("$size offlens loaded")
        tl.dumpToLog()

        res
    }

    /**
     * Call this from background thread to initialize data safely
     */
    fun warmup() {
        Log.d(TAG, "${acus.size} acus and ${offlens.size} offlens")
    }

    fun getId(acu: String): Int {
        val pos = Arrays.binarySearch(acus, acu)
        return if (pos < 0) 0 else pos + 1
    }

    fun getAcu(id: Int): String {
        return acus[id - 1]
    }

    fun listAcus(prefix: String): List<String> {
        var from = Arrays.binarySearch(acus, "$prefix\u0000").inv() - 1
        var to = Arrays.binarySearch(acus, "$prefix\uffff").inv()

        if (from < 0) from = 0
        if (to > acus.size) to = acus.size

        return acus.slice(from..to - 1)
    }

    fun getRenderer(acu: String): Renderer {
        val offlen = offlens[getId(acu) - 1]
        return Renderer(offlen shr 24, offlen and 0xffffff)
    }
}
