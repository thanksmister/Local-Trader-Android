/*
 * Copyright (c) 2019 ThanksMister LLC
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
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.EditAdvertisement
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.AdvertisementsViewModel
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_edit_advertisement.*
import kotlinx.android.synthetic.main.view_min_max.*
import kotlinx.android.synthetic.main.view_price_equation.*
import kotlinx.android.synthetic.main.view_terms.*

import timber.log.Timber
import javax.inject.Inject

class EditAdvertisementActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: AdvertisementsViewModel

    private var advertisement: Advertisement? = null
    private var adId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_advertisement)

        adId = if (savedInstanceState == null) {
            intent.getIntExtra(EXTRA_ADVERTISEMENT_ID, 0)
        } else {
            savedInstanceState.getInt(EXTRA_ADVERTISEMENT_ID, 0)
        }

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }

        editSveButton.text = getString(R.string.button_save_changes)
        editSveButton.setOnClickListener {
            validateChangesAndCommit()
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AdvertisementsViewModel::class.java)
        observeViewModel(viewModel)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_ADVERTISEMENT_ID, adId)
    }

    override fun onBackPressed() {
        toast(getString(R.string.text_post_update_canceled))
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun observeViewModel(viewModel: AdvertisementsViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if (message?.message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@EditAdvertisementActivity, message.message!!)
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@EditAdvertisementActivity, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.toast(message)
            }
        })
        viewModel.getAdvertisementUpdated().observe(this, Observer { updated ->
            if(updated != null && updated) {
                dialogUtils.hideProgressDialog()
                advertisementSaved()
            }
        })
        disposable.add(viewModel.getAdvertisementData(adId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ data ->
                    if (data != null) {
                        advertisement = data.advertisement
                        setAdvertisementOnView(advertisement!!)
                    }
                }, { error ->
                    Timber.e(error.message)
                    dialogUtils.showAlertDialog(this@EditAdvertisementActivity, getString(R.string.error_title),
                            getString(R.string.toast_error_opening_advertisement), DialogInterface.OnClickListener { _, _ ->
                        finish()
                    })
                }))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            advertisementCanceled()
            return true
        } else if (item.itemId == R.id.action_advertisement) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_EDIT_URL + adId)))
            } catch (ex: android.content.ActivityNotFoundException) {
                Toast.makeText(this@EditAdvertisementActivity, getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit, menu)
        return true
    }

    private fun advertisementError() {
        dialogUtils.hideProgressDialog()
        toast(getString(R.string.toast_error_update_advertisement))
        val returnIntent = intent
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    private fun setAdvertisementOnView(advertisement: Advertisement) {
        activeCheckBox.isChecked = advertisement.visible
        if (TradeUtils.ALTCOIN_ETH == advertisement.onlineProvider) {
            editMinimumAmountCurrency.text = getString(R.string.eth)
            editMaximumAmountCurrency.text = getString(R.string.eth)
        } else {
            editMinimumAmountCurrency.text = advertisement.currency
            editMaximumAmountCurrency.text = advertisement.currency
        }
        if (!TextUtils.isEmpty(advertisement.minAmount)) {
            editMinimumAmount.setText(advertisement.minAmount)
        }
        if (!TextUtils.isEmpty(advertisement.maxAmount)) {
            editMaximumAmount.setText(advertisement.maxAmount)
        }
        if (!TextUtils.isEmpty(advertisement.priceEquation)) {
            editPriceEquation.setText(advertisement.priceEquation)
        }
        if (!TextUtils.isEmpty(advertisement.message)) {
            editMessageText.setText(advertisement.message)
        }
    }

    /**
     * Check the edit advertisement to see if there are any changes to commit.
     * @param advertisement Advertisement
     */
    private fun validateChangesAndCommit() {
        var commitChanges = false
        if (advertisement != null) {
            val isActive = activeCheckBox.isChecked
            val min = editMinimumAmount.text.toString()
            val max = editMaximumAmount.text.toString()
            val equation = editPriceEquation.text.toString()
            val message = editMessageText!!.text.toString()
            val minAmount = TradeUtils.convertCurrencyAmount(min)
            val maxAmount = TradeUtils.convertCurrencyAmount(max)
            when {
                TextUtils.isEmpty(equation) -> {
                    toast(getString(R.string.toast_price_equation_blank))
                    return
                }
                TextUtils.isEmpty(min) -> {
                    toast(getString(R.string.toast_minimum_amount))
                    return
                }
                TextUtils.isEmpty(max) -> {
                    toast(getString(R.string.toast_maximum_amount))
                    return
                }
                else -> {
                    try {
                        if (advertisement!!.priceEquation != equation
                                || advertisement!!.message != message
                                || advertisement!!.minAmount != minAmount
                                || advertisement!!.maxAmount != maxAmount
                                || advertisement!!.visible != isActive) {
                            commitChanges = true
                        }
                    } catch (e: NullPointerException) {
                        advertisementError()
                        return
                    }
                }
            }
            advertisement?.minAmount = minAmount
            advertisement?.message = message
            advertisement?.maxAmount = maxAmount
            advertisement?.visible = isActive
            advertisement?.priceEquation = equation
        } else {
            advertisementError()
            return
        }
        Timber.d("commitChanges: $commitChanges")
        if (commitChanges) {
            dialogUtils.showProgressDialog(this@EditAdvertisementActivity, getString(R.string.dialog_saving_changes))
            viewModel.updateAdvertisement(advertisement!!)
        } else {
            advertisementCanceled()
        }
    }

    private fun advertisementCanceled() {
        dialogUtils.hideProgressDialog()
        toast(getString(R.string.text_post_update_canceled))
        val returnIntent = intent
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    private fun advertisementSaved() {
        dialogUtils.hideProgressDialog()
        toast(getString(R.string.message_advertisement_changed))
        val returnIntent = intent
        setResult(RESULT_UPDATED, returnIntent)
        finish()
    }

    companion object {
        const val EXTRA_ADVERTISEMENT_ID = "com.thanksmister.extras.EXTRA_ADVERTISEMENT_ID"
        //const val EXTRA_EDITED_ADVERTISEMENT = "com.thanksmister.extras.EXTRA_EDITED_ADVERTISEMENT"
        const val REQUEST_CODE = 10937
        const val RESULT_UPDATED = 72322
        fun createStartIntent(context: Context, adId: Int): Intent {
            val intent = Intent(context, EditAdvertisementActivity::class.java)
            intent.putExtra(EXTRA_ADVERTISEMENT_ID, adId)
            return intent
        }
    }

    /*@NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(EditAdvertisementActivity.this, SyncProvider.ADVERTISEMENT_TABLE_URI, null, AdvertisementItem.AD_ID + " = ?", new String[]{adId}, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ADVERTISEMENT_LOADER_ID:
                if (cursor != null && cursor.getCount() > 0) {
                    Advertisement advertisement = Advertisement.getModel(cursor);
                    if (advertisement != null) {

                        setInitialAdvertisementViews(advertisement, advertisement);
                    }
                }
                break;
            default:
                throw new Error("Incorrect loader Id");
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }*/
}