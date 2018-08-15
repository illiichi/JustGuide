package com.illiichi.justdialog

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class DescriptionPageView @JvmOverloads constructor(context: Context,
                                                    attrs: AttributeSet? = null,
                                                    defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    fun show(title: String, pages: Array<String>){
        var currentPage = 0
        LayoutInflater.from(this.context).inflate(R.layout.activity_page, this)

        val textTitle = this.findViewById<TextView>(R.id.textTitle)
        val webView = this.findViewById<ImageView>(R.id.webView)
        val buttonPrev = this.findViewById<Button>(R.id.buttonPrev)
        val buttonNext = this.findViewById<Button>(R.id.buttonNext)
        val buttonDone = this.findViewById<Button>(R.id.buttonBack)

        fun changePage(dx: Int){
            val totalPage = pages.size
            currentPage = minOf(maxOf(currentPage + dx, 0), totalPage)
            textTitle.text = "$title(${currentPage + 1} / $totalPage)"
            webView.setImageURI(Uri.parse(pages[currentPage]))

            buttonPrev.isEnabled = currentPage >= 1
            buttonNext.visibility = if(currentPage >= (totalPage - 1))  View.GONE else View.VISIBLE;
            buttonDone.visibility = if(currentPage >= (totalPage - 1))  View.VISIBLE else View.GONE;
        }

        buttonPrev.setOnClickListener{ _ -> changePage(-1)}
        buttonNext.setOnClickListener{_ -> changePage(+1)}
        buttonDone.setOnClickListener { _ -> this.visibility = View.GONE }
        changePage(currentPage)
        this.visibility = View.VISIBLE
    }
}