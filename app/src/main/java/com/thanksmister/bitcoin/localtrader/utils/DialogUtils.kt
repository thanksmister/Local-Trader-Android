/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.utils

import android.app.Dialog
import androidx.lifecycle.*
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.thanksmister.bitcoin.localtrader.R
import timber.log.Timber

/**
 * Dialog utils
 */
class DialogUtils(base: Context?) : ContextWrapper(base), LifecycleObserver {

    private var alertDialog: AlertDialog? = null
    private var dialog: Dialog? = null
    private var progressDialog: Dialog? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun clearDialogs() {
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
            dialog = null
        }
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
        hideProgressDialog()
    }

    fun toast(messageId: Int) {
        toast(getString(messageId))
    }

    fun toast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM, 0, 180)
        toast.show()
    }

    fun showProgressDialog(context: Context, message: String, cancelable: Boolean = true) {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
            return
        }
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var dialogView: View? = null
        dialogView = inflater.inflate(R.layout.dialog_progress, null, false)
        val progressDialogMessage: TextView
        if (dialogView != null) {
            progressDialogMessage = dialogView.findViewById(R.id.progressDialogMessage)
            progressDialogMessage.text = message
        }
        progressDialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setCancelable(cancelable)
                .setView(dialogView)
                .show()
    }

    fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    fun hideAlertDialog() {
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
    }

    fun showAlertDialog(context: Context, message: String) {
        hideAlertDialog()
        alertDialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    fun showAlertHtmlDialog(context: Context, message: String) {
        hideAlertDialog()
        dialog = Dialog(context, R.style.CustomAlertDialog)
        dialog?.setContentView(R.layout.dialog_markets)
        dialog?.setCancelable(true)
        dialog?.show()
        val textView = dialog?.findViewById<TextView>(R.id.dialogMessage)
        textView?.text = Html.fromHtml(message)
        textView?.movementMethod = LinkMovementMethod.getInstance()
        dialog?.findViewById<Button>(R.id.closeButton)?.setOnClickListener {
            dialog?.hide()
        }
    }

    fun showAlertHtmlDialog(context: Context, message: String, onPositiveButton: View.OnClickListener) {
        hideAlertDialog()
        dialog = Dialog(context, R.style.CustomAlertDialog)
        dialog?.setContentView(R.layout.dialog_markets)
        dialog?.setCancelable(true)
        dialog?.show()
        val textView = dialog?.findViewById<TextView>(R.id.dialogMessage)
        textView?.text = Html.fromHtml(message)
        textView?.movementMethod = LinkMovementMethod.getInstance()
        dialog?.findViewById<Button>(R.id.closeButton)?.setOnClickListener(onPositiveButton)
    }

    fun showAlertDialogToDismiss(activity: Context, title: String, message: String) {
        hideAlertDialog()
        alertDialog = AlertDialog.Builder(activity, R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    fun showAlertDialog(activity: Context, title: String, message: String) {
        hideAlertDialog()
        alertDialog = AlertDialog.Builder(activity, R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    fun showAlertDialog(context: Context, message: String, onClickListener: DialogInterface.OnClickListener) {
        hideAlertDialog()
        Timber.d("showAlertDialog")
        alertDialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .show()
    }

    fun showAlertDialog(context: Context, title: String, message: String, onPositiveButton: DialogInterface.OnClickListener) {
        hideAlertDialog()
        Timber.d("showAlertDialog")
        alertDialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onPositiveButton)
                .show()
    }

    fun showAlertDialog(context: Context, title: String, message: String, onPositiveButton: DialogInterface.OnClickListener,
                        onNegativeButton: DialogInterface.OnClickListener) {
        hideAlertDialog()
        Timber.d("showAlertDialog")
        alertDialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onPositiveButton)
                .setNegativeButton(android.R.string.cancel, onNegativeButton)
                .show()
    }

    fun showAlertDialog(context: Context, message: String, onPositiveButton: DialogInterface.OnClickListener,
                        onNegativeButton: DialogInterface.OnClickListener) {
        hideAlertDialog()
        Timber.d("showAlertDialog")
        alertDialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onPositiveButton)
                .setNegativeButton(android.R.string.cancel, onNegativeButton)
                .show()
    }

    fun showAlertDialogCancel(context: Context, message: String, onClickListener: DialogInterface.OnClickListener) {
        hideAlertDialog()
        Timber.d("showAlertDialog")
        alertDialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }
}