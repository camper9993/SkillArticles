package ru.skillbranch.skillarticles.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import kotlinx.android.synthetic.main.activity_root.view.*
import kotlinx.android.synthetic.main.layout_bottombar.view.*

fun View.setMarginOptionally(
    left : Int = marginLeft,
    right : Int = marginRight,
    top : Int = marginTop,
    bottom : Int = marginBottom
) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(left, top, right, bottom)
    layoutParams = params
}
