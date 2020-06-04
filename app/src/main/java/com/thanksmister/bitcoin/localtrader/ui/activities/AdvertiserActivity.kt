/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
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
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.thanksmister.bitcoin.localtrader.BuildConfig
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.managers.ConnectionLiveData
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType.*
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.AdvertisementsViewModel
import com.thanksmister.bitcoin.localtrader.utils.Dates
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_advertiser.*
import timber.log.Timber
import javax.inject.Inject

class AdvertiserActivity : BaseActivity() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewModel: AdvertisementsViewModel

    private var connectionLiveData: ConnectionLiveData? = null
    private var adId: Int = 0
    private var advertisement: Advertisement? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_advertiser)

        adId = if (savedInstanceState == null) {
            intent.getIntExtra(EXTRA_AD_ID, 0)
        } else {
            savedInstanceState.getInt(EXTRA_AD_ID, 0)
        }

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = ""
        }
        requestButton.setOnClickListener {
            showTradeRequest(advertisement)
        }
        requestButton.isEnabled = false

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AdvertisementsViewModel::class.java)
        observeViewModel(viewModel)
    }

    override fun onStart() {
        super.onStart()
        connectionLiveData = ConnectionLiveData(this@AdvertiserActivity)
        connectionLiveData?.observe(this, Observer { connected ->
            if(!connected!!) {
                dialogUtils.toast(getString(R.string.error_network_disconnected))
            }
        })
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_AD_ID, adId)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.advertiser, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_profile -> {
                showProfile(advertisement)
                return true
            }
            R.id.action_location -> {
                showAdvertisementOnMap(advertisement)
                return true
            }
            R.id.action_website -> {
                showPublicAdvertisement(advertisement)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observeViewModel(viewModel: AdvertisementsViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if (message?.message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@AdvertiserActivity, message.message!!)
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@AdvertiserActivity, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if(message != null) {
                dialogUtils.toast(message)
            }
        })

        dialogUtils.showProgressDialog(this@AdvertiserActivity, getString(R.string.dialog_loading))
        disposable.add(viewModel.fetchAdvertiser(adId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    if(it != null) {
                        dialogUtils.hideProgressDialog()
                        advertisement = it.advertisement
                        setTradeRequirements(advertisement)
                        setHeader(advertisement!!.tradeType)
                        setTradeRequirements(advertisement)
                        if (TradeUtils.isOnlineTrade(advertisement!!)) {
                            setAdvertisement(advertisement, it.method)
                        } else {
                            setAdvertisement(advertisement, null)
                        }
                        advertiserContentLayout.visibility = View.VISIBLE
                    }
                }, {
                    error -> Timber.e("Error" + error.message)
                    dialogUtils.hideProgressDialog()
                    dialogUtils.showAlertDialog(this@AdvertiserActivity, getString(R.string.error_title),
                            getString(R.string.toast_error_opening_advertisement), DialogInterface.OnClickListener { _, _ ->
                        finish()
                    })
                }))
    }

    @Suppress("DEPRECATION")
    private fun setAdvertisement(advertisement: Advertisement?, method: Method?) {

        if(advertisement != null) {
            val location = advertisement.location
            val price = advertisement.currency
            val tradeType = TradeType.valueOf(advertisement.tradeType)
            var title = ""
            when (tradeType) {
                ONLINE_BUY -> title = getString(R.string.text_advertisement_online_buy)
                ONLINE_SELL -> title = getString(R.string.text_advertisement_online_sale)
                else -> { }
            }
            val paymentMethod = TradeUtils.getPaymentMethod(this@AdvertiserActivity, advertisement, method)
            if (TextUtils.isEmpty(paymentMethod)) {
                noteTextAdvertiser.text = Html.fromHtml(getString(R.string.advertisement_notes_text_online_location, title, price, location))
            } else {
                noteTextAdvertiser.text = Html.fromHtml(getString(R.string.advertisement_notes_text_online, title, price, paymentMethod, location))
            }

            if (advertisement.isATM) {
                priceLayout.visibility = View.GONE
                priceLayoutDivider.visibility = View.GONE
                tradePrice.text = getString(R.string.text_atm)
                noteTextAdvertiser.text = Html.fromHtml(getString(R.string.advertiser_notes_text_atm, advertisement.currency, location))
            } else {
                tradePrice.text = getString(R.string.trade_price, advertisement.tempPrice, advertisement.currency)
            }

            traderName.text = advertisement.profile.username

            if (advertisement.isATM) {
                adTradeLimit.text = ""
            } else {
                if (advertisement.maxAmount != null && advertisement.minAmount != null) {
                    adTradeLimit.text = getString(R.string.trade_limit, advertisement.minAmount, advertisement.maxAmount, advertisement.currency)
                }
                if (advertisement.maxAmount == null && advertisement.minAmount != null) {
                    adTradeLimit.text = getString(R.string.trade_limit_min, advertisement.minAmount, advertisement.currency)
                }
                if (advertisement.maxAmountAvailable != null && advertisement.minAmount != null) { // no maximum set
                    adTradeLimit.text = getString(R.string.trade_limit, advertisement.minAmount, advertisement.maxAmountAvailable, advertisement.currency)
                } else if (advertisement.maxAmountAvailable != null) {
                    adTradeLimit.text = getString(R.string.trade_limit_max, advertisement.maxAmountAvailable, advertisement.currency)
                }
            }
            if (advertisement.message != null) {
                tradeTerms.text = advertisement.message!!.trim { it <= ' ' }
            }
            tradeFeedback.text = advertisement.profile.feedbackScore.toString()
            tradeCount.text = advertisement.profile.tradeCount
            if (advertisement.profile.lastOnline != null) {
                lastSeenIcon.setBackgroundResource(TradeUtils.determineLastSeenIcon(advertisement.profile.lastOnline!!))
            }
            val date = Dates.parseLocalDateStringAbbreviatedTime(advertisement.profile.lastOnline)
            dateText.text = getString(R.string.text_last_seen, date)
            requestButton.isEnabled = true
        }
    }

    private fun setTradeRequirements(advertisement: Advertisement?) {
        if(advertisement != null) {
            var showLayout = false
            if (advertisement.trustedRequired
                    || advertisement.smsVerificationRequired
                    || advertisement.requireIdentification) {
                showLayout = true
            }

            trustedTextView.visibility = if (advertisement.trustedRequired) View.VISIBLE else View.GONE
            identifiedTextView.visibility = if (advertisement.requireIdentification) View.VISIBLE else View.GONE
            smsTextView.visibility = if (advertisement.smsVerificationRequired) View.VISIBLE else View.GONE

            if (advertisement.requireFeedbackScore != null && TradeUtils.isOnlineTrade(advertisement)) {
                feedbackText.visibility = View.VISIBLE
                feedbackText.text = Html.fromHtml(getString(R.string.trade_request_minimum_feedback_score, advertisement.requireFeedbackScore))
                showLayout = true
            } else {
                feedbackText.visibility = View.GONE
            }

            if (advertisement.requireTradeVolume != null && TradeUtils.isOnlineTrade(advertisement)) {
                volumeText.visibility = View.VISIBLE
                volumeText.text = Html.fromHtml(getString(R.string.trade_request_minimum_volume, advertisement.requireTradeVolume))
                showLayout = true
            } else {
                volumeText.visibility = View.GONE
            }

            if (advertisement.firstTimeLimitBtc != null && TradeUtils.isOnlineTrade(advertisement)) {
                limitText.visibility = View.VISIBLE
                limitText.text = Html.fromHtml(getString(R.string.trade_request_new_buyer_limit, advertisement.firstTimeLimitBtc))
                showLayout = true
            } else {
                limitText.visibility = View.GONE
            }

            requirementsLayout.visibility = if (showLayout) View.VISIBLE else View.GONE
        }
    }

    private fun setHeader(tradeTypeString: String) {
        Timber.d("setHeader trade type $tradeTypeString")
        var header = ""
        val tradeType = TradeType.valueOf(tradeTypeString)
        header = when (tradeType) {
            ONLINE_SELL, ONLINE_BUY -> if (tradeType == ONLINE_SELL) getString(R.string.text_online_seller) else getString(R.string.text_online_buyer)
            NONE -> ""
        }
        if (supportActionBar != null) {
            supportActionBar!!.title = header
        }
    }

    private fun showTradeRequest(advertisement: Advertisement?) {
        if(advertisement != null) {
            val tradeType = TradeType.valueOf(advertisement.tradeType)
            if (TradeType.NONE.name == tradeType.name) {
                dialogUtils.showAlertDialog(this@AdvertiserActivity, getString(R.string.error_title), getString(R.string.error_invalid_trade_type),
                        DialogInterface.OnClickListener { dialog, which ->
                            if (!BuildConfig.DEBUG) {
                                Crashlytics.log("advertisement_data: " + advertisement.toString());
                                Crashlytics.logException(Throwable("Bad trade type for requested trade: $tradeType  $advertisement Id: $adId"))
                            }
                            finish()
                        })
                return
            }
            val intent = TradeRequestActivity.createStartIntent(this, advertisement.adId,
                    advertisement.tradeType, advertisement.countryCode, advertisement.onlineProvider,
                    advertisement.tempPrice, advertisement.minAmount,
                    advertisement.maxAmountAvailable, advertisement.currency,
                    advertisement.profile.username)
            startActivity(intent)
        }
    }

    private fun showPublicAdvertisement(advertisement: Advertisement?) {
        if(advertisement != null) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(advertisement.actions.publicView))
                startActivity(browserIntent)
            } catch (exception: ActivityNotFoundException) {
                toast(getString(R.string.toast_error_no_installed_ativity))
            }
        }
    }

    private fun showProfile(advertisement: Advertisement?) {
        if(advertisement != null) {
            val url = "https://localbitcoins.com/accounts/profile/" + advertisement.profile.username + "/"
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } catch (exception: ActivityNotFoundException) {
                toast(getString(R.string.toast_error_no_installed_ativity))
            }
        }
    }

    private fun showAdvertisementOnMap(advertisement: Advertisement?) {
        if(advertisement != null) {
            val geoUri = "geo:0,0?q=" + advertisement.location!!
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                startActivity(intent)
            } catch (exception: ActivityNotFoundException) {
                toast(getString(R.string.toast_error_no_installed_ativity))
            }
        }
    }

    companion object {
        const val EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID"
        fun createStartIntent(context: Context, adId: Int): Intent {
            val intent = Intent(context, AdvertiserActivity::class.java)
            intent.putExtra(EXTRA_AD_ID, adId)
            return intent
        }
    }
}