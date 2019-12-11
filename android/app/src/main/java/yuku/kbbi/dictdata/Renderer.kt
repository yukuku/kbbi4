package yuku.kbbi.dictdata

import android.graphics.Typeface
import android.support.v4.content.res.ResourcesCompat
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.view.View
import yuku.kbbi.App
import yuku.kbbi.BuildConfig
import yuku.kbbi.R
import yuku.kbbi.dastruk.Cav
import yuku.kbbi.dastruk.ValueReader
import yuku.salsa20.cipher.Salsa20InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream

class Renderer(val file_no: Int, val offset: Int, val acu_click: (Int) -> Unit, val kategori_click: (String, String) -> Unit) {
    val linkColor by lazy {
        ResourcesCompat.getColor(App.context.resources, R.color.colorPrimary, null)
    }

    val colors_20s by lazy {
        intArrayOf(
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_KELAS, null),
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_BAHASA, null),
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_BIDANG, null),
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_ILMIAH, null),
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_KIMIA, null),
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_RAGAM, null)
        )
    }

    val colors_30s by lazy {
        intArrayOf(
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_ki, null),
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_kp, null),
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_akr, null),
            ResourcesCompat.getColor(App.context.resources, R.color.renderer_ukp, null)
        )
    }

    object K {
        val d = fun(s: String) = ByteArray(s.length / 2) { i ->
            Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16).toByte()
        }

        val e = fun() = String.format(Locale.US, "%016x", (android.text.format.DateUtils::class.java.name.hashCode().toLong() shl 31) or 987654321)

        val f = fun() = run {
            var k = 1L
            for (c in SpannableStringBuilder::class.java.canonicalName.orEmpty()) {
                k *= 47
                k += c.toInt() xor 777
            }
            String.format(Locale.US, "%016x", k)
        }
    }

    val bin by lazy { K.d(BuildConfig.ENC_KEY_1 + K.e() + K.f() + "d4036e") }
    val src by lazy { K.d(BuildConfig.ENC_KEY_IV) }

    private fun getDesc(fn: String): ValueReader {
        return ValueReader(GZIPInputStream(Salsa20InputStream(
            App.context.assets.open(fn),
            bin,
            src
        )))
    }

    fun render(): SpannableStringBuilder {
        val res = SpannableStringBuilder()

        getDesc("dictdata/acu_desc_$file_no.s").use { vr ->
            vr.skip(offset)

            val cav = Cav()

            while (true) {
                vr.readClv(cav)
                if (cav.code == 0xff) break

                when (cav.code) {
                    0 -> res.append(cav.string)
                    1, 3, 5 -> run {
                        val len = res.length
                        res.append(cav.string)
                        res.setSpan(RelativeSizeSpan(1.4f), len, res.length, 0)

                        if (cav.code == 5) {
                            res.setSpan(StyleSpan(Typeface.BOLD_ITALIC), len, res.length, 0)
                        } else {
                            res.setSpan(StyleSpan(Typeface.BOLD), len, res.length, 0)
                        }
                    }
                    2 -> res.append("/${cav.string}/")
                    4 -> run {
                        val len = res.length
                        res.append("(${cav.string})")
                        res.setSpan(StyleSpan(Typeface.BOLD), len, res.length, 0)
                    }
                    10, 11, 12, 13, 14, 15 -> run {
                        val len = res.length

                        when (cav.code) {
                            10 -> res.append("Varian")
                            11 -> res.append("Dasar")
                            12 -> res.append("Gabungan kata")
                            13 -> res.append("Kata turunan")
                            14 -> res.append("Peribahasa")
                            15 -> res.append("Kiasan")
                        }

                        res.setSpan(StyleSpan(Typeface.BOLD), len, res.length, 0)
                        res.append(": ")
                    }
                    20, 21, 22, 23, 24, 25 -> run {
                        val len = res.length
                        val nilai = cav.string
                        res.append(nilai)
                        res.setSpan(ForegroundColorSpan(colors_20s[cav.code - 20]), len, res.length, 0)

                        val link_facet = when (cav.code) {
                            20 -> "kelas"
                            21 -> "bahasa"
                            22 -> "bidang"
                            25 -> "ragam"
                            else -> null
                        }

                        if (link_facet != null) {
                            res.setSpan(object : ClickableSpan() {
                                override fun onClick(widget: View) {
                                    kategori_click(link_facet, nilai)
                                }

                                override fun updateDrawState(ds: TextPaint) {
                                    // nop
                                }
                            }, len, res.length, 0)
                        }
                    }
                    74 -> run { // KIMIA + SUB
                        val len = res.length
                        res.append(cav.string)
                        res.setSpan(ForegroundColorSpan(colors_20s[cav.code - 70]), len, res.length, 0)
                        res.setSpan(SubscriptSpan(), len, res.length, 0)
                    }
                    30, 31, 32, 33 -> run {
                        val len = res.length

                        when (cav.code) {
                            30 -> res.append("ki")
                            31 -> res.append("kp")
                            32 -> res.append("akr")
                            33 -> res.append("ukp")
                        }

                        res.setSpan(ForegroundColorSpan(colors_30s[cav.code - 30]), len, res.length, 0)
                    }
                    40, 41 -> run {
                        val len = res.length
                        val acu_id = cav.number
                        res.append(Acu.getAcu(acu_id))
                        res.setSpan(ForegroundColorSpan(linkColor), len, res.length, 0)
                        res.setSpan(object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                acu_click(acu_id)
                            }

                            override fun updateDrawState(ds: TextPaint) {
                                // nop
                            }
                        }, len, res.length, 0)

                        if (cav.code == 41) {
                            res.append(" » ")
                        }
                    }
                    42 -> run {
                        val len = res.length
                        res.append(cav.string)
                        res.setSpan(StyleSpan(Typeface.BOLD), len, res.length, 0)
                    }
                    50 -> run {
                        val len = res.length
                        res.append(cav.string)
                        res.setSpan(ForegroundColorSpan(0xff666666.toInt()), len, res.length, 0)
                    }
                    60, 61, 62, 63 -> run {
                        val len = res.length
                        res.append(cav.string)
                        when (cav.code) {
                            60 -> res.setSpan(StyleSpan(Typeface.BOLD), len, res.length, 0)
                            61 -> res.setSpan(StyleSpan(Typeface.ITALIC), len, res.length, 0)
                            62 -> res.setSpan(SubscriptSpan(), len, res.length, 0)
                            63 -> res.setSpan(SuperscriptSpan(), len, res.length, 0)
                        }
                    }
                }
            }
        }

        return res
    }
}
