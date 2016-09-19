package yuku.kbbi4.dictdata

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import yuku.kbbi4.App
import yuku.kbbi4.dastruk.Cav
import yuku.kbbi4.dastruk.ValueReader
import java.io.BufferedInputStream

class Renderer(val file_no: Int, val offset: Int) {
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
                }
                2 -> res.append("/${cav.string}/")
                4 -> run {
                    val len = res.length
                    res.append("(${cav.string})")
                    res.setSpan(StyleSpan(Typeface.BOLD), len, res.length, 0)
                }
                10, 11, 12, 13, 14 -> run {
                    res.append("\n\n")

                    val len = res.length

                    when (cav.code) {
                        10 -> res.append("Varian")
                        11 -> res.append("Dasar")
                        12 -> res.append("Kata gabungan")
                        13 -> res.append("Kata berimbuhan")
                        14 -> res.append("Peribahasa")
                        15 -> res.append("Idiom")
                    }

                    res.setSpan(UnderlineSpan(), len, res.length, 0)
                    res.append(": ")
                }
                20, 21, 22, 23, 24 -> run {
                    val len = res.length
                    res.append(cav.string)
                    res.setSpan(ForegroundColorSpan(Color.BLUE), len, res.length, 0)
                }
                30, 31, 32 -> run {
                    val len = res.length

                    when (cav.code) {
                        30 -> res.append("ki")
                        31 -> res.append("kp")
                        32 -> res.append("akr")
                    }

                    res.setSpan(ForegroundColorSpan(Color.GREEN), len, res.length, 0)
                }
                40, 41 -> run {
                    val len = res.length
                    res.append(Acu.getAcu(cav.number))
                    res.setSpan(ForegroundColorSpan(Color.RED), len, res.length, 0)

                    if (cav.code == 41) {
                        res.append(" >> ")
                    }
                }
                50 -> run {
                    val len = res.length
                    res.append(cav.string)
                    res.setSpan(ForegroundColorSpan(Color.GRAY), len, res.length, 0)
                }
            }
        }

        return res
    }
}
