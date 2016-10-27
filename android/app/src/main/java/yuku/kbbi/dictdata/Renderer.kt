package yuku.kbbi.dictdata

import android.graphics.Typeface
import android.support.v4.content.res.ResourcesCompat
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.*
import android.view.View
import yuku.kbbi.App
import yuku.kbbi.R
import yuku.kbbi.dastruk.Cav
import yuku.kbbi.dastruk.ValueReader
import java.io.BufferedInputStream

class Renderer(val file_no: Int, val offset: Int, val acu_click: (Int) -> Unit) {
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

    fun render(): SpannableStringBuilder {
        val vr = ValueReader(BufferedInputStream(App.context.assets.open("dictdata/acu_desc_$file_no.txt")))
        vr.skip(offset)

        val res = SpannableStringBuilder()
        val cav = Cav()

        while (true) {
            vr.readClv(cav)
            if (cav.code == 0xff) break

            when (cav.code) {
                0 -> res.append(cav.string)
                1, 3 -> run {
                    val len = res.length
                    res.append(cav.string)
                    res.setSpan(StyleSpan(Typeface.BOLD), len, res.length, 0)
                    res.setSpan(RelativeSizeSpan(1.4f), len, res.length, 0)
                }
                2 -> res.append("/${cav.string}/")
                4 -> run {
                    val len = res.length
                    res.append("(${cav.string})")
                    res.setSpan(StyleSpan(Typeface.BOLD), len, res.length, 0)
                }
                10, 11, 12, 13, 14 -> run {
                    val len = res.length

                    when (cav.code) {
                        10 -> res.append("Varian")
                        11 -> res.append("Dasar")
                        12 -> res.append("Gabungan kata")
                        13 -> res.append("Kata berimbuhan")
                        14 -> res.append("Peribahasa")
                        15 -> res.append("Idiom")
                    }

                    res.setSpan(StyleSpan(Typeface.BOLD), len, res.length, 0)
                    res.append(": ")
                }
                20, 21, 22, 23, 24, 25 -> run {
                    val len = res.length
                    res.append(cav.string)
                    res.setSpan(ForegroundColorSpan(colors_20s[cav.code - 20]), len, res.length, 0)
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

        return res
    }
}
