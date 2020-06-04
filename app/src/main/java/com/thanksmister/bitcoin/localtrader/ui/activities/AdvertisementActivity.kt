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

package com.thanksmister.bitcoin.localtrader.ui.activities

import androidx.lifecycle.*
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.AdvertisementsViewModel
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_advertisement.*
import timber.log.Timber
import javax.inject.Inject

class AdvertisementActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewModel: AdvertisementsViewModel

    private var adId: Int = 0
    private var menu: Menu? = null
    private var advertisement: Advertisement? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_advertisement)

        if (savedInstanceState == null && intent.hasExtra(EXTRA_AD_ID)) {
            adId = intent.getIntExtra(EXTRA_AD_ID, 0)
        } else if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_AD_ID)) {
            adId = savedInstanceState.getInt(EXTRA_AD_ID, 0)
        }

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = ""
        }

        advertisementSwipeLayout.setOnRefreshListener(this)
        advertisementSwipeLayout.setColorSchemeColors(resources.getColor(R.color.red))

        /*if (adId == 0) {
            dialogUtils.showAlertDialog(this@AdvertisementActivity, getString(R.string.error_no_advertisement),
                    DialogInterface.OnClickListener { _, _ ->
                        finish();
                    })
        }*/

        if(adId > 0) {
            viewModel = ViewModelProviders.of(this, viewModelFactory).get(AdvertisementsViewModel::class.java)
            observeViewModel(viewModel)
        }
    }

    // Bug: http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
    public override fun onSaveInstanceState(outState: Bundle) {
        //No call for super(). Bug on API Level > 11.
        outState.putInt(EXTRA_AD_ID, adId)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EditAdvertisementActivity.REQUEST_CODE) {
            if (resultCode == EditAdvertisementActivity.RESULT_UPDATED) {
                updateAdvertisement() // update the new editAdvertisement
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.advertisement, menu)
        this.menu = menu
        if (advertisement != null) {
            setMenuVisibilityIcon(advertisement!!.visible)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_edit -> {
                editAdvertisement(advertisement)
                return true
            }
            R.id.action_share -> {
                shareAdvertisement(advertisement)
                return true
            }
            R.id.action_delete -> {
                deleteAdvertisement(advertisement)
                return true
            }
            R.id.action_visible -> {
                updateAdvertisementVisibility(advertisement)
                return true
            }
            R.id.action_website -> {
                viewOnlineAdvertisement(advertisement)
                return true
            }
            R.id.action_location -> {
                showAdvertisementOnMap(advertisement)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observeViewModel(viewModel: AdvertisementsViewModel) {
        // TODO we need to logout if needed
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if (message?.message != null) {
                onRefreshStop()
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@AdvertisementActivity, message.message)
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null) {
                onRefreshStop()
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@AdvertisementActivity, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            dialogUtils.hideProgressDialog()
            if(message != null) {
                dialogUtils.toast(message)
            }
        })
        viewModel.getAdvertisementUpdated().observe(this, Observer { updated ->
            if(updated != null && updated) {
                onRefreshStop()
                dialogUtils.hideProgressDialog()
                dialogUtils.toast(getString(R.string.toast_update_visibility))
            }
        })
        viewModel.getAdvertisementDeleted().observe(this, Observer { updated ->
            if(updated != null && updated) {
                onRefreshStop()
                dialogUtils.hideProgressDialog()
                dialogUtils.toast(getString(R.string.toast_advertisement_deleted))
                finish()
            }
        })
        disposable.add(viewModel.getAdvertisementData(adId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data->
                    if(data != null) {
                        advertisement = data.advertisement
                        setTradeRequirements(advertisement)
                        if (TradeUtils.isOnlineTrade(advertisement!!)) {
                            setAdvertisement(advertisement, data.method)
                        } else {
                            setAdvertisement(advertisement, null)
                        }
                        advertisementContentLayout.visibility = View.VISIBLE
                        onRefreshStop()
                    }
                }, { error ->
                    Timber.e(error.message)
                    dialogUtils.showAlertDialog(this@AdvertisementActivity, getString(R.string.error_title),
                            getString(R.string.toast_error_opening_advertisement), DialogInterface.OnClickListener { _, _ ->
                        finish()
                    })
                    onRefreshStop()
                }))

        dialogUtils.toast(getString(R.string.toast_refreshing_data))
        updateAdvertisement()
    }

    override fun onResume() {
        super.onResume()
        intent.extras?.let {
            val type = it.getInt(MainActivity.EXTRA_NOTIFICATION_TYPE, 0)
            val id = it.getInt(MainActivity.EXTRA_NOTIFICATION_ID, 0)
            if (type == NotificationUtils.NOTIFICATION_TYPE_ADVERTISEMENT) {
                this.adId = id
                viewModel = ViewModelProviders.of(this, viewModelFactory).get(AdvertisementsViewModel::class.java)
                observeViewModel(viewModel)
            }
        }
    }

    override fun onRefresh() {
        updateAdvertisement()
    }

    private fun onRefreshStop() {
        advertisementSwipeLayout.isRefreshing = false
    }

    private fun updateAdvertisement() {
        viewModel.fetchAdvertisement(adId)
    }

    private fun setAdvertisement(advertisement: Advertisement?, method: Method?) {

        advertisement?.let {
            advertisementTradePrice.text = getString(R.string.trade_price, advertisement.tempPrice, advertisement.currency)
            val price = advertisement.currency
            val tradeType = TradeType.valueOf(advertisement.tradeType)
            var title = ""
            when (tradeType) {
                TradeType.ONLINE_BUY -> title = getString(R.string.text_advertisement_online_buy)
                TradeType.ONLINE_SELL -> title = getString(R.string.text_advertisement_online_sale)
                else -> { }
            }
            val location = advertisement.location
            var paymentMethod = TradeUtils.getPaymentMethod(this@AdvertisementActivity, advertisement, method)
            if (TextUtils.isEmpty(paymentMethod)) {
                noteTextAdvertisement.text = Html.fromHtml(getString(R.string.advertisement_notes_text_online_location, title, price, location))
            } else {
                noteTextAdvertisement.text = Html.fromHtml(getString(R.string.advertisement_notes_text_online, title, price, paymentMethod, location))
            }
            when {
                advertisement.atmModel != null -> advertisementTradeLimit.text = ""
                advertisement.minAmount == null -> advertisementTradeLimit.text = ""
                advertisement.maxAmount == null -> advertisementTradeLimit.text = getString(R.string.trade_limit_min, advertisement.minAmount, advertisement.currency)
                else -> // no maximum set
                    advertisementTradeLimit.text = getString(R.string.trade_limit, advertisement.minAmount, advertisement.maxAmount, advertisement.currency)
            }
            priceEquation.text = advertisement.priceEquation
            if (!TextUtils.isEmpty(advertisement.message)) {
                advertisementTradeTerms.text = advertisement.message!!.trim { it <= ' ' }
            } else {
                advertisementTradeTerms.visibility = View.GONE
            }
            if (TradeUtils.isOnlineTrade(advertisement)) {
                paymentMethod = TradeUtils.getPaymentMethodName(advertisement, method)
                onlineProvider.text = paymentMethod
                if (!TextUtils.isEmpty(advertisement.bankName)) {
                    bankName.text = advertisement.bankName
                } else {
                    bankNameLayout.visibility = View.GONE
                }
                if (!TextUtils.isEmpty(advertisement.accountInfo)) {
                    paymentDetails.text = advertisement.accountInfo!!.trim { it <= ' ' }
                } else {
                    paymentDetailsLayout.visibility = View.GONE
                }
            } else {
                onlinePaymentLayout.visibility = View.GONE
                paymentDetailsLayout.visibility = View.GONE
                bankNameLayout.visibility = View.GONE
            }
            advertisementId.text = advertisement.adId.toString()
            setTradeRequirements(advertisement)
            updateAdvertisementNote(advertisement)
        }
    }

    private fun setTradeRequirements(advertisement: Advertisement?) {
        advertisement?.let {
            var showLayout = false
            if (advertisement.trustedRequired
                    || advertisement.smsVerificationRequired
                    || advertisement.requireIdentification) {
                showLayout = true
            }

            advertisementTrustedTextView.visibility = if (advertisement.trustedRequired) View.VISIBLE else View.GONE
            advertisementIdentifiedTextView.visibility = if (advertisement.requireIdentification) View.VISIBLE else View.GONE
            advertisementSmsTextView.visibility = if (advertisement.smsVerificationRequired) View.VISIBLE else View.GONE

            if (!TextUtils.isEmpty(advertisement.requireFeedbackScore) && TradeUtils.isOnlineTrade(advertisement)) {
                advertisementFeedbackText.visibility = View.VISIBLE
                advertisementFeedbackText.text = Html.fromHtml(getString(R.string.trade_request_minimum_feedback_score, advertisement.requireFeedbackScore))
                showLayout = true
            } else {
                advertisementFeedbackText.visibility = View.GONE
            }

            if (!TextUtils.isEmpty(advertisement.requireTradeVolume) && TradeUtils.isOnlineTrade(advertisement)) {
                advertisementVolumeText.visibility = View.VISIBLE
                advertisementVolumeText.text = Html.fromHtml(getString(R.string.trade_request_minimum_volume, advertisement.requireTradeVolume))
                showLayout = true
            } else {
                advertisementVolumeText.visibility = View.GONE
            }

            if (!TextUtils.isEmpty(advertisement.firstTimeLimitBtc) && TradeUtils.isOnlineTrade(advertisement)) {
                advertisementLimitText.visibility = View.VISIBLE
                advertisementLimitText.text = Html.fromHtml(getString(R.string.trade_request_new_buyer_limit, advertisement.firstTimeLimitBtc))
                showLayout = true
            } else {
                advertisementLimitText.visibility = View.GONE
            }

            advertisementRequirementsLayout.visibility = if (showLayout) View.VISIBLE else View.GONE
        }
    }

    private fun updateAdvertisementNote(advertisement: Advertisement?) {
        advertisement?.let {
            noteLayout.visibility = if (advertisement.visible) View.GONE else View.VISIBLE
            noteText.text = getString(R.string.advertisement_invisible_warning)
            setMenuVisibilityIcon(advertisement.visible)
        }
    }

    private fun setMenuVisibilityIcon(show: Boolean) {
        val icon = if (show) {
            R.drawable.ic_action_visibility
        } else {
            R.drawable.ic_action_visibility_off
        }
        if (menu != null && menu!!.hasVisibleItems()) {
            val menuItem = menu!!.getItem(0)
            menuItem?.setIcon(icon)
        }
    }

    private fun viewOnlineAdvertisement(advertisement: Advertisement?) {
        if(advertisement != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(advertisement.actions.publicView))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                dialogUtils.showAlertDialog(this@AdvertisementActivity, getString(R.string.toast_error_no_installed_ativity))
            }
        }
    }

    private fun deleteAdvertisement(advertisement: Advertisement?) {
        advertisement?.let {
            dialogUtils.showAlertDialog(this@AdvertisementActivity, getString(R.string.advertisement_delete_confirm),
                    DialogInterface.OnClickListener {
                        dialog, which ->
                        deleteAdvertisementConfirmed(advertisement);
                    })
        }
    }

    private fun deleteAdvertisementConfirmed(advertisement: Advertisement?) {
        advertisement?.let {
            dialogUtils.showProgressDialog(this@AdvertisementActivity, getString(R.string.progress_deleting))
            viewModel.deleteAdvertisement(advertisement)
        }
    }

    private fun updateAdvertisementVisibility(advertisement: Advertisement?) {
        advertisement?.let {
            dialogUtils.showProgressDialog(this@AdvertisementActivity, getString(R.string.dialog_updating_visibility))
            advertisement.visible = !advertisement.visible
            viewModel.updateAdvertisement(advertisement)
        }
    }

    private fun showAdvertisementOnMap(advertisement: Advertisement?) {
        advertisement?.let {
            val geoUri = "geo:0,0?q=" + advertisement.location.orEmpty()
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                startActivity(intent)
            } catch (exception: ActivityNotFoundException) {
                toast(getString(R.string.toast_no_activity_for_maps))
            }
        }
    }

    private fun shareAdvertisement(advertisement: Advertisement?) {
        advertisement?.let {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            var message = ""
            val buyOrSell = if (TradeUtils.isBuyTrade(advertisement)) getString(R.string.text_buy) else getString(R.string.text_sell)
            val prep = if (TradeUtils.isSellTrade(advertisement)) getString(R.string.text_from) else getString(R.string.text_to)
            val onlineOrLocal = getString(R.string.text_online)
            val provider = TradeUtils.parsePaymentServiceTitle(advertisement.onlineProvider)
            message = getString(R.string.text_advertisement_message, buyOrSell, onlineOrLocal, advertisement.location + prep + advertisement.profile.username, provider, advertisement.actions.advertisementPublicView)
            shareIntent.putExtra(Intent.EXTRA_TEXT, message)
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.text_share_advertisement, buyOrSell, advertisement.location))
            startActivity(Intent.createChooser(shareIntent, getString(R.string.text_chooser_share_to)))
        }
    }

    private fun editAdvertisement(advertisement: Advertisement?) {
        advertisement?.let {
            val intent = EditAdvertisementActivity.createStartIntent(this@AdvertisementActivity, advertisement.adId)
            startActivityForResult(intent, EditAdvertisementActivity.REQUEST_CODE)
        }
    }

    companion object {
        const val EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID"
        const val REQUEST_CODE = 10939
        const val RESULT_DELETED = 837373
        fun createStartIntent(context: Context, adId: Int): Intent {
            val intent = Intent(context, AdvertisementActivity::class.java)
            intent.putExtra(EXTRA_AD_ID, adId)
            return intent
        }
    }
}