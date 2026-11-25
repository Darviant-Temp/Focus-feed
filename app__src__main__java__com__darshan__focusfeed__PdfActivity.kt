package com.darshan.focusfeed

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView

class PdfActivity : Activity() {
    private var pfd: ParcelFileDescriptor? = null
    private var rend: PdfRenderer? = null
    private val cache = object : LruCache<Int, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val f = Pdfs.file(this, intent.getStringExtra("id") ?: "")
        if (!f.exists()) { finish(); return }
        pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        val r = PdfRenderer(pfd!!)
        rend = r
        val w = resources.displayMetrics.widthPixels
        val lv = ListView(this)
        lv.divider = null
        lv.setBackgroundColor(Color.parseColor("#F4F4F7"))
        lv.adapter = object : BaseAdapter() {
            override fun getCount(): Int = r.pageCount
            override fun getItem(position: Int): Any = position
            override fun getItemId(position: Int): Long = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val iv = (convertView as? ImageView) ?: ImageView(this@PdfActivity).apply {
                    adjustViewBounds = true
                    setPadding(0, 6, 0, 6)
                }
                iv.setImageBitmap(page(r, position, w))
                return iv
            }
        }
        setContentView(lv)
    }

    private fun page(r: PdfRenderer, i: Int, w: Int): Bitmap {
        val hit = cache.get(i)
        if (hit != null) return hit
        synchronized(r) {
            val pg = r.openPage(i)
            val h = (w.toFloat() / pg.width * pg.height).toInt()
            val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bm.eraseColor(Color.WHITE)
            pg.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pg.close()
            cache.put(i, bm)
            return bm
        }
    }

    override fun onDestroy() {
        try { rend?.close(); pfd?.close() } catch (e: Exception) { }
        super.onDestroy()
    }
}
