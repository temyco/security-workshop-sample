package co.temy.securitysample.extentions

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun View.showKeyboard(delay: Long, flags: Int = 0) = postDelayed({ showKeyboard(flags) }, delay)

fun View.showKeyboard(flags: Int = 0) {
    val service: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    service?.showSoftInput(this, flags)
}

fun View.hideKeyboard() {
    val service: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    service?.hideSoftInputFromWindow(windowToken, 0)
}