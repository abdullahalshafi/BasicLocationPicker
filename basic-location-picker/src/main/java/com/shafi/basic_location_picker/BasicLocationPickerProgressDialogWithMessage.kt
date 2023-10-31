package com.shafi.basic_location_picker

import android.app.ActionBar
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView


class BasicLocationPickerProgressDialogWithMessage(private val activity: Activity) {

    private lateinit var dialog: Dialog
    private var messageTv: TextView? = null

    init {
        val display = activity.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        dialog = Dialog(activity)
        val view =
            LayoutInflater.from(activity).inflate(R.layout.basic_location_picker_progress_dialog_with_message, null)
        messageTv = view.findViewById(R
            .id.progressbar_message_tv)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.window!!.setBackgroundDrawableResource(R.drawable.basic_location_picker_dialog_rounded_bg)
        dialog.window!!.setLayout(width - 150, ActionBar.LayoutParams.WRAP_CONTENT)
    }

    fun setMessage(message: String?) {
        messageTv?.text = message
    }

    fun show() {
        if (!activity.isFinishing && this::dialog.isInitialized && !dialog.isShowing) {
            dialog.show()
        }
    }

    fun dismiss() {
        if (!activity.isFinishing && this::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun isShowing(): Boolean {
        if (!activity.isFinishing && this::dialog.isInitialized) {
            return dialog.isShowing
        }
        return false
    }
}