/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.Parser
import com.thanksmister.bitcoin.localtrader.utils.StringUtils
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import timber.log.Timber
import java.io.*
import java.net.SocketTimeoutException
import javax.inject.Inject

class MessageViewModel @Inject
constructor(application: Application, private val preferences: Preferences) : BaseViewModel(application) {

    private val messagePostStatus = MutableLiveData<Boolean>()
    private val fetcher: LocalBitcoinsFetcher by lazy {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun getMessagePostStatus(): LiveData<Boolean> {
        return messagePostStatus
    }

    private fun setMessagePostStatus(value: Boolean) {
        this.messagePostStatus.value = value
    }

    init {

    }

    fun postMessage(contactId: Int, message: String) {
        disposable += fetcher.postMessage(contactId, message)
                .applySchedulers()
                .subscribe ({
                    setMessagePostStatus(true)
                }, { error ->
                    Timber.e("Message Post Error " + error.message)
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> handleSocketTimeoutException()
                        else -> showAlertMessage(getApplication<BaseApplication>().getString(R.string.toast_error_message))
                    }
                })
    }

    private fun postMessageWithAttachment(contactId: Int, message: String, file: File) {
        Timber.d("postMessageWithAttachment: file ${file.path}")
        disposable += fetcher.postMessageWithAttachment(contactId, message, file)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    setMessagePostStatus(true)
                }, {
                    error -> Timber.e("Message Post Attachment Error" + error.message)
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.toast_error_message))
                    }
                })
    }


    fun generateMessageBitmap(contactId: Int, fileName: String, message: String, uri: Uri) {
        disposable.add(
                generateBitmapObservable(fileName, uri)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ file ->
                            if(file != null) {
                                postMessageWithAttachment(contactId, message, file)
                            }
                        }, { error ->
                            Timber.e(error.message)
                            showAlertMessage(getApplication<BaseApplication>().getString(R.string.toast_file_no_upload))
                        }))
    }

    private fun generateBitmapObservable(fileName: String, uri: Uri): Observable<File> {
        return Observable.create { subscriber ->
            try {
                val file = getBitmapFromStream(fileName, uri)
                subscriber.onNext(file)
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }

    private fun getBitmapFromStream(fileName: String, uri: Uri): File {
        var bitmap: Bitmap? = null
        try {
            val outDimens = getBitmapDimensions(getApplication<BaseApplication>(), uri)
            val sampleSize = calculateSampleSize(outDimens.outWidth, outDimens.outHeight, 1200, 1200)
            bitmap = downSampleBitmap(getApplication<BaseApplication>(), uri, sampleSize)
            val file = File(getApplication<BaseApplication>().cacheDir, StringUtils.removeExtension(fileName))
            file.createNewFile()
            val bos = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 0, bos)
            val bitmapData = bos.toByteArray()
            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapData)
            fos.flush()
            fos.close()
            return file
        } catch (e: Exception) {
            Timber.e("File Exception: " + e.message)
            throw Exception(e.message)
        }
    }

    @Throws(FileNotFoundException::class, IOException::class)
    private fun getBitmapDimensions(context: Context, uri: Uri): BitmapFactory.Options {
        val outDimens = BitmapFactory.Options()
        outDimens.inJustDecodeBounds = true // the decoder will return null (no bitmap)
        val inputStream = context.contentResolver.openInputStream(uri)
        // if Options requested only the size will be returned
        BitmapFactory.decodeStream(inputStream, null, outDimens)
        inputStream.close()

        return outDimens
    }

    private fun calculateSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        var inSampleSize = 1
        if (height > targetHeight || width > targetWidth) {
            // Calculate ratios of height and width to requested height and
            // width
            val heightRatio = Math.round(height.toFloat() / targetHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / targetWidth.toFloat())
            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        return inSampleSize
    }

    @Throws(FileNotFoundException::class, IOException::class)
    private fun downSampleBitmap(context: Context, uri: Uri, sampleSize: Int): Bitmap? {
        val resizedBitmap: Bitmap?
        val outBitmap = BitmapFactory.Options()
        outBitmap.inJustDecodeBounds = false // the decoder will return a bitmap
        outBitmap.inSampleSize = sampleSize
        val inputStream = context.contentResolver.openInputStream(uri)
        resizedBitmap = BitmapFactory.decodeStream(inputStream, null, outBitmap)
        inputStream.close()
        return resizedBitmap
    }
}