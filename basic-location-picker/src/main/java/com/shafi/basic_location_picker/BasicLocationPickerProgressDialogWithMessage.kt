package com.shafi.basic_location_picker

import android.app.ActionBar
import android.app.Activity
import android.app.Dialog
import android.graphics.Point
import android.view.LayoutInflater
import android.widget.TextView


class BasicLocationPickerProgressDialogWithMessage(activity: Activity) {

    private var dialog: Dialog
    private var messageTv: TextView? = null
    private var activity: Activity? = activity

    init {
        val display = activity.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        dialog = Dialog(activity)
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.basic_location_picker_progress_dialog_with_message, null)
        messageTv = view.findViewById(R.id.progressbar_message_tv)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        //dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setLayout(width - 150, ActionBar.LayoutParams.WRAP_CONTENT)
    }

    fun setMessage(message: String?) {
        messageTv?.text = message
    }

    public fun show() {
        if ((activity != null && !activity!!.isFinishing) && !dialog.isShowing) {
            dialog.show()
        }
    }

    public fun dismiss() {
        if ((activity != null && !activity!!.isFinishing) && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    public fun isShowing(): Boolean {
        return dialog.isShowing
    }
}