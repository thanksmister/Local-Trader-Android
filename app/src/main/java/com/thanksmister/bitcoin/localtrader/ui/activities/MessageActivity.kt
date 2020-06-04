/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.activities

import androidx.lifecycle.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.TextUtils
import android.view.View
import android.view.View.GONE
import android.view.inputmethod.InputMethodManager
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.MessageViewModel
import kotlinx.android.synthetic.main.view_message.*
import timber.log.Timber
import javax.inject.Inject

class MessageActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: MessageViewModel

    private var contactId: Int = 0
    private var contactName: String? = null
    private var message: String = ""
    private var mUri: Uri? = null
    private var mFileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_message)

        if (savedInstanceState == null) {
            contactId = intent.getIntExtra(EXTRA_ID, 0)
            contactName = intent.getStringExtra(EXTRA_NAME)
            if(intent.hasExtra(EXTRA_MESSAGE)) {
                message = intent.getStringExtra(EXTRA_MESSAGE)
            }
        } else {
            contactId = savedInstanceState.getInt(EXTRA_ID, 0)
            contactName = savedInstanceState.getString(EXTRA_NAME)
            if(savedInstanceState.containsKey(EXTRA_MESSAGE)) {
                message = savedInstanceState.getString(EXTRA_MESSAGE)
            }
        }

        if (!TextUtils.isEmpty(message)) {
            editMessageText.setText(message)
        }
        messageTitle.text = getString(R.string.title_message_to, contactName)

        attachButton.setOnClickListener {
            attachFile()
        }
        removeAttachmentButton.setOnClickListener {
            removeAttachment()
        }
        sendMessageButton.setOnClickListener {
            validateMessage()
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MessageViewModel::class.java)
        observeViewModel(viewModel)
    }

    private fun observeViewModel(viewModel: MessageViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@MessageActivity, message.message, DialogInterface.OnClickListener { dialog, which ->
                    if(message.code == ExceptionCodes.MESSAGE_ERROR_CODE || message.code == ExceptionCodes.AUTHENTICATION_ERROR_CODE ) {
                        finish()
                    }
                })
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@MessageActivity, it)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.toast(it)
            }
        })
        viewModel.getMessagePostStatus().observe(this, Observer { status ->
            status?.let {
                if (it) {
                    dialogUtils.hideProgressDialog()
                    handleMessageSent()
                } else if (!it) {
                    dialogUtils.hideProgressDialog()
                    dialogUtils.toast(R.string.toast_error_message);
                }
            }
        })
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_ID, contactId)
        outState.putString(EXTRA_NAME, contactName)
        outState.putString(EXTRA_MESSAGE, message)
    }

    override fun onBackPressed() {
        setResult(RESULT_MESSAGE_CANCELED)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(data != null) {
            when (requestCode) {
                GALLERY_INTENT_CALLED -> if (resultCode != RESULT_CANCELED) {
                    mUri = data.data
                    if (mUri != null) {
                        mFileName = getDocumentName(mUri!!)
                        attachButton.visibility = GONE
                        attachmentLayout.visibility = View.VISIBLE
                        attachmentName.text = mFileName
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun validateMessage() {
        message = editMessageText.text.toString()
        if (TextUtils.isEmpty(message) && mUri == null) {
            dialogUtils.toast(getString(R.string.toast_message_blank))
            return
        }
        if (mUri != null && mFileName != null) {
            dialogUtils.showProgressDialog(this@MessageActivity, getString(R.string.dialog_send_message));
            viewModel.generateMessageBitmap(contactId, mFileName!!, message, mUri!!)
        } else {
            dialogUtils.showProgressDialog(this@MessageActivity, getString(R.string.dialog_send_message))
            if(contactId > 0) {
                viewModel.postMessage(contactId, message)
            }
        }
    }

    private fun handleMessageSent() {
        editMessageText.setText("")
        // hide keyboard and notify
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        } catch (e: NullPointerException) {
            Timber.e("Error closing keyboard")
        }
        setResult(RESULT_MESSAGE_SENT)
        finish()
    }

    private fun removeAttachment() {
        attachButton!!.visibility = View.VISIBLE
        attachmentLayout!!.visibility = GONE
        mUri = null
        mFileName = null
    }

    private fun attachFile() {
        val intent: Intent
        if (Build.VERSION.SDK_INT >= 19) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            try {
                startActivityForResult(Intent.createChooser(intent, getString(R.string.chooser_select_image)), GALLERY_INTENT_CALLED)
            } catch (ex: android.content.ActivityNotFoundException) {
                dialogUtils.toast(getString(R.string.toast_no_file_manager))
            }

        } else {
            intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            try {
                startActivityForResult(Intent.createChooser(intent, getString(R.string.chooser_select_image)), GALLERY_INTENT_CALLED)
            } catch (ex: android.content.ActivityNotFoundException) {
                dialogUtils.toast(getString(R.string.toast_no_file_manager))
            }
        }
    }

    private fun getDocumentName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor.use { internalCursor ->
            if (internalCursor != null && internalCursor.moveToFirst()) {
                val displayName = internalCursor.getString(internalCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                internalCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return displayName
            }
        }
        return ""
    }

    companion object {
        const val EXTRA_ID = "com.thanksmister.extras.EXTRA_ID"
        const val EXTRA_NAME = "com.thanksmister.extras.EXTRA_NAME"
        const val EXTRA_MESSAGE = "com.thanksmister.extras.EXTRA_MESSAGE"
        const val REQUEST_MESSAGE_CODE = 760
        const val RESULT_MESSAGE_SENT = 765
        const val RESULT_MESSAGE_CANCELED = 768
        const val GALLERY_INTENT_CALLED = 112

        fun createStartIntent(context: Context, contactId: Int, contactName: String): Intent {
            val intent = Intent(context, MessageActivity::class.java)
            intent.putExtra(EXTRA_ID, contactId)
            intent.putExtra(EXTRA_NAME, contactName)
            return intent
        }
    }
}