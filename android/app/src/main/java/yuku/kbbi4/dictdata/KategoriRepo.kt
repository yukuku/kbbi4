package yuku.kbbi4.dictdata

import android.util.TimingLogger
import yuku.kbbi4.App
import yuku.kbbi4.dastruk.ValueReader
import java.io.BufferedInputStream

data class Kategori(@JvmField val nilai: String, @JvmField val desc: String)

object KategoriRepo {
    const val TAG = "Kategori"

    private val repo by lazy {
        val tl = TimingLogger(TAG, "load kategori repo")

        val res = mapOf(*listOf("bahasa", "bidang").map { jenis ->
            ValueReader(BufferedInputStream(App.context.assets.open("dictdata/kat_index_$jenis.txt"), 40000)).use { vr ->
                val size = vr.readVarint()
                val list = Array(size) {
                    Kategori(vr.readString(), vr.readString())
                }
                Pair(jenis, list.toList())
            }
        }.toTypedArray())

        tl.addSplit("kategori repo loaded")
        tl.dumpToLog()

        res
    }

    fun listKategoris(jenis: String): List<Kategori> {
        return repo[jenis].orEmpty()
    }

    fun getKategori(jenis: String, nilai: String): Kategori {
        return listKategoris(jenis).find { kategori -> kategori.nilai == nilai }!!
    }

    fun listAcuIds(jenis: String, nilai: String): IntArray {
        return ValueReader(BufferedInputStream(App.context.assets.open("dictdata/kat_${jenis}_$nilai.txt"), 40000)).use { vr ->
            val size = vr.readVarint()
            IntArray(size) { vr.readVarint() }
        }
    }
}
