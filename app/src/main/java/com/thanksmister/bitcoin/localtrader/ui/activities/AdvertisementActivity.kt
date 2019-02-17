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
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.Toolbar
import android.text.Html
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem
import com.thanksmister.bitcoin.localtrader.databinding.ActivityAdvertisementBinding
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.persistence.Method
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.utils.Strings
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils

import timber.log.Timber


class AdvertisementActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val binding: ActivityAdvertisementBinding by lazy {
        DataBindingUtil.setContentView<ActivityAdvertisementBinding>(this,  R.layout.activity_advertisement)
    }

    internal var tradePrice: TextView? = null

    internal var noteTextAdvertisement: TextView? = null

    internal var tradeLimit: TextView? = null

    internal var tradeTerms: TextView? = null

    internal var priceEquation: TextView? = null

    internal var onlineProvider: TextView? = null

    internal var bankName: TextView? = null

    internal var paymentDetails: TextView? = null

    internal var noteText: TextView? = null

    internal var advertisementId: TextView? = null

    internal var noteLayout: View? = null

    internal var bankNameLayout: View? = null

    internal var termsLayout: View? = null

    internal var paymentDetailsLayout: View? = null

    internal var onlinePaymentLayout: View? = null

    internal var content: View? = null

    internal var toolbar: Toolbar? = null

    internal var requirementsLayout: View? = null

    internal var trustedTextView: TextView? = null

    internal var identifiedTextView: TextView? = null

    internal var smsTextView: TextView? = null

    internal var feedbackText: TextView? = null

    internal var limitText: TextView? = null

    internal var volumeText: TextView? = null

    internal var swipeLayout: SwipeRefreshLayout? = null

    private var adId: String? = null
    private var menu: Menu? = null
    private val methodItems: List<Method>? = null
    private val advertisement: AdvertisementItem? = null
    private var handler: Handler? = null

    private val refreshRunnable = Runnable {
        if (swipeLayout != null)
            swipeLayout!!.isRefreshing = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = Handler()

        if (savedInstanceState == null && intent.hasExtra(EXTRA_AD_ID)) {
            adId = intent.getStringExtra(EXTRA_AD_ID)
        } else if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_AD_ID)) {
            adId = savedInstanceState.getString(EXTRA_AD_ID)
        }

        if (toolbar != null) {
            setSupportActionBar(toolbar)
            if (supportActionBar != null) {
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                supportActionBar!!.setTitle("")
                setToolBarMenu(toolbar!!)
            }
        }

        swipeLayout!!.setOnRefreshListener(this)
        swipeLayout!!.setColorSchemeColors(resources.getColor(R.color.red))

        if (TextUtils.isEmpty(adId)) {
            /*showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), getString(R.string.error_no_advertisement)), new Action0() {
                @Override
                public void call() {
                    finish();
                }
            });*/
        }
    }

    // Bug: http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
    public override fun onSaveInstanceState(outState: Bundle) {
        //No call for super(). Bug on API Level > 11.
        outState.putString(EXTRA_AD_ID, adId)
        super.onSaveInstanceState(outState)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        if (requestCode == EditAdvertisementActivity.REQUEST_CODE) {
            if (resultCode == EditAdvertisementActivity.RESULT_UPDATED) {
                updateAdvertisement() // update the new editAdvertisement
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (toolbar != null) {
            toolbar!!.inflateMenu(R.menu.advertisement)
        }
        this.menu = menu
        if (advertisement != null) {
            setMenuVisibilityIcon(advertisement.visible())
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRefresh() {
        Timber.d("onRefresh")
        updateAdvertisement()
    }

    fun onRefreshStop() {
        handler!!.removeCallbacks(refreshRunnable)

        if (swipeLayout != null)
            swipeLayout!!.isRefreshing = false
    }

    fun onRefreshStart() {
        handler = Handler()
        handler!!.postDelayed(refreshRunnable, 50)
    }

    public override fun onResume() {
        super.onResume()
    }

    override fun handleNetworkDisconnect() {
        onRefreshStop()
        snack(getString(R.string.error_no_internet), true)
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    override fun handleRefresh() {
        onRefreshStart()
        updateAdvertisement()
    }

    fun setToolBarMenu(toolbar: Toolbar) {
        toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    editAdvertisement(advertisement!!)
                    return@OnMenuItemClickListener true
                }
                R.id.action_share -> {
                    shareAdvertisement()
                    return@OnMenuItemClickListener true
                }
                R.id.action_delete -> {
                    deleteAdvertisement()
                    return@OnMenuItemClickListener true
                }
                R.id.action_visible -> {
                    updateAdvertisementVisibility()
                    return@OnMenuItemClickListener true
                }
                R.id.action_website -> {
                    viewOnlineAdvertisement()
                    return@OnMenuItemClickListener true
                }
                R.id.action_location -> {
                    showAdvertisementOnMap()
                    return@OnMenuItemClickListener true
                }
            }
            false
        })
    }

    private fun updateAdvertisement() {
        toast(getString(R.string.toast_refreshing_data))
        /*dataService.getAdvertisement(adId)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Update editAdvertisement subscription safely unsubscribed");
                    }
                })
                .compose(this.<Advertisement>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Advertisement>() {
                    @Override
                    public void call(Advertisement advertisement) {
                        if (advertisement != null) {
                            dbManager.updateAdvertisement(advertisement);
                        }
                        onRefreshStop();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(throwable, true);
                        onRefreshStop();
                    }
                });*/
    }

    fun setAdvertisement(advertisement: AdvertisementItem, methods: List<Method>) {

        val method = TradeUtils.getMethodForAdvertisement(advertisement, methods)
        tradePrice!!.text = getString(R.string.trade_price, advertisement.temp_price(), advertisement.currency())

        val price = advertisement.currency()
        val tradeType = TradeType.valueOf(advertisement.trade_type())
        var title = ""
        when (tradeType) {
            TradeType.LOCAL_BUY -> title = getString(R.string.text_advertisement_local_buy)
            TradeType.LOCAL_SELL -> title = getString(R.string.text_advertisement_local_sale)
            TradeType.ONLINE_BUY -> title = getString(R.string.text_advertisement_online_buy)
            TradeType.ONLINE_SELL -> title = getString(R.string.text_advertisement_online_sale)
        }

        val location = advertisement.location_string()
        if (TradeUtils.isLocalTrade(advertisement)) {
            noteTextAdvertisement!!.text = Html.fromHtml(getString(R.string.advertisement_notes_text_locally, title, price, location))
        } else {
            val paymentMethod = TradeUtils.getPaymentMethod(advertisement, method)
            if (TextUtils.isEmpty(paymentMethod)) {
                noteTextAdvertisement!!.text = Html.fromHtml(getString(R.string.advertisement_notes_text_online_location, title, price, location))
            } else {
                noteTextAdvertisement!!.text = Html.fromHtml(getString(R.string.advertisement_notes_text_online, title, price, paymentMethod, location))
            }
        }


        if (advertisement.atm_model() != null) {
            tradeLimit!!.text = ""
        } else if (advertisement.min_amount() == null) {
            tradeLimit!!.text = ""
        } else if (advertisement.max_amount() == null) {
            tradeLimit!!.text = getString(R.string.trade_limit_min, advertisement.min_amount(), advertisement.currency())
        } else { // no maximum set
            tradeLimit!!.text = getString(R.string.trade_limit, advertisement.min_amount(), advertisement.max_amount(), advertisement.currency())
        }

        priceEquation!!.text = advertisement.price_equation()

        if (!TextUtils.isEmpty(advertisement.message())) {
            tradeTerms!!.text = advertisement.message()!!.trim { it <= ' ' }
        } else {
            termsLayout!!.visibility = View.GONE
        }

        if (TradeUtils.isOnlineTrade(advertisement)) {

            val paymentMethod = TradeUtils.getPaymentMethodName(advertisement, method)
            onlineProvider!!.text = paymentMethod

            if (!TextUtils.isEmpty(advertisement.bank_name())) {
                bankName!!.text = advertisement.bank_name()
            } else {
                bankNameLayout!!.visibility = View.GONE
            }

            if (!TextUtils.isEmpty(advertisement.account_info())) {
                paymentDetails!!.text = advertisement.account_info()!!.trim { it <= ' ' }
            } else {
                paymentDetailsLayout!!.visibility = View.GONE
            }
        } else {
            onlinePaymentLayout!!.visibility = View.GONE
            paymentDetailsLayout!!.visibility = View.GONE
            bankNameLayout!!.visibility = View.GONE
        }

        advertisementId!!.text = advertisement.ad_id()
        setTradeRequirements(advertisement)
        updateAdvertisementNote(advertisement)
    }

    /**
     * Toggles the trader requirements and options visibility
     *
     * @param advertisement `AdvertisementItem`
     */
    fun setTradeRequirements(advertisement: AdvertisementItem) {
        var showLayout = false
        if (advertisement.trusted_required()
                || advertisement.sms_verification_required()
                || advertisement.require_identification()) {

            showLayout = true
        }

        trustedTextView!!.visibility = if (advertisement.trusted_required()) View.VISIBLE else View.GONE
        identifiedTextView!!.visibility = if (advertisement.require_identification()) View.VISIBLE else View.GONE
        smsTextView!!.visibility = if (advertisement.sms_verification_required()) View.VISIBLE else View.GONE

        if (!Strings.isBlank(advertisement.require_feedback_score()) && TradeUtils.isOnlineTrade(advertisement)) {
            feedbackText!!.visibility = View.VISIBLE
            feedbackText!!.text = Html.fromHtml(getString(R.string.trade_request_minimum_feedback_score, advertisement.require_feedback_score()))
            showLayout = true
        } else {
            feedbackText!!.visibility = View.GONE
        }

        if (!Strings.isBlank(advertisement.require_trade_volume()) && TradeUtils.isOnlineTrade(advertisement)) {
            volumeText!!.visibility = View.VISIBLE
            volumeText!!.text = Html.fromHtml(getString(R.string.trade_request_minimum_volume, advertisement.require_trade_volume()))
            showLayout = true
        } else {
            volumeText!!.visibility = View.GONE
        }

        if (!Strings.isBlank(advertisement.first_time_limit_btc()) && TradeUtils.isOnlineTrade(advertisement)) {
            limitText!!.visibility = View.VISIBLE
            limitText!!.text = Html.fromHtml(getString(R.string.trade_request_new_buyer_limit, advertisement.first_time_limit_btc()))
            showLayout = true
        } else {
            limitText!!.visibility = View.GONE
        }

        requirementsLayout!!.visibility = if (showLayout) View.VISIBLE else View.GONE
    }

    fun updateAdvertisementNote(advertisement: AdvertisementItem) {
        noteLayout!!.visibility = if (advertisement.visible()) View.GONE else View.VISIBLE
        noteText!!.text = getString(R.string.advertisement_invisible_warning)
        setMenuVisibilityIcon(advertisement.visible())
    }

    private fun setMenuVisibilityIcon(show: Boolean) {
        val icon: Int
        if (show) {
            icon = R.drawable.ic_action_visibility
        } else {
            icon = R.drawable.ic_action_visibility_off
        }

        if (menu != null && menu!!.hasVisibleItems()) {
            val menuItem = menu!!.getItem(0)
            menuItem?.setIcon(icon)
        }

        onRefreshStop()
    }

    private fun viewOnlineAdvertisement() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(advertisement!!.action_public_view()))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showAlertDialog(getString(R.string.toast_error_no_installed_ativity))
        }

    }

    private fun deleteAdvertisement() {
        /*ConfirmationDialogEvent event = new ConfirmationDialogEvent("Delete Advertisement",
                getString(R.string.advertisement_delete_confirm),
                getString(R.string.button_delete),
                getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                deleteAdvertisementConfirmed(adId);
            }
        });

        showConfirmationDialog(event);*/
    }

    private fun deleteAdvertisementConfirmed(adId: String) {
        showProgressDialog(ProgressDialogEvent(getString(R.string.progress_deleting)))
        /*dataService.deleteAdvertisement(adId)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Delete editAdvertisement safely unsubscribed");
                    }
                })
                .compose(this.<Boolean>bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(final Boolean deleted) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                if (deleted) {
                                    dbManager.deleteAdvertisement(adId);
                                    toast(getString(R.string.toast_advertisement_deleted));
                                    setResult(RESULT_DELETED);
                                    finish();
                                } else {
                                    showAlertDialog(getString(R.string.alert_error_deleting_advertisement));
                                }
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                //showAlertDialog(getString(R.string.alert_error_deleting_advertisement));
                                showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), throwable.getMessage()));
                            }
                        });
                    }
                });*/
    }

    private fun updateAdvertisementVisibility() {

        showProgressDialog(ProgressDialogEvent(getString(R.string.dialog_updating_visibility)))

        var editAdvertisement = Advertisement()
        editAdvertisement = editAdvertisement.convertAdvertisementItemToAdvertisement(advertisement)

        val adId = editAdvertisement.ad_id
        val visible = !editAdvertisement.visible

        /*dataService.updateAdvertisementVisibility(editAdvertisement, visible)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                    @Override
                    public void call(JSONObject jsonObject) {
                        hideProgressDialog();
                        dbManager.updateAdvertisementVisibility(adId, visible);
                        toast(getString(R.string.toast_update_visibility));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        hideProgressDialog();
                        showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), throwable.getMessage()));
                    }
                });*/
    }

    private fun showAdvertisementOnMap() {
        var geoUri = ""
        if (TradeUtils.isLocalTrade(advertisement!!)) {
            geoUri = "http://maps.google.com/maps?q=loc:" + advertisement.lat() + "," + advertisement.lon() + " (" + advertisement.location_string() + ")"
        } else {
            geoUri = "geo:0,0?q=" + advertisement.location_string()
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            toast(getString(R.string.toast_no_activity_for_maps))
        }

    }

    private fun shareAdvertisement() {

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"

        var message = ""
        val buyOrSell = if (TradeUtils.isBuyTrade(advertisement!!)) getString(R.string.text_buy) else getString(R.string.text_sell)
        val prep = if (TradeUtils.isSellTrade(advertisement)) getString(R.string.text_from) else getString(R.string.text_to)
        val onlineOrLocal = if (TradeUtils.isLocalTrade(advertisement)) getString(R.string.text_locally) else getString(R.string.text_online)

        if (TradeUtils.isLocalTrade(advertisement)) {
            message = getString(R.string.text_advertisement_message_short, buyOrSell, onlineOrLocal, advertisement.location_string() + prep + advertisement.profile_username(), advertisement.action_public_view())
        } else {
            val provider = TradeUtils.parsePaymentServiceTitle(advertisement.online_provider())
            message = getString(R.string.text_advertisement_message, buyOrSell, onlineOrLocal, advertisement.location_string() + prep + advertisement.profile_username(), provider, advertisement.action_public_view())
        }

        shareIntent.putExtra(Intent.EXTRA_TEXT, message)
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.text_share_advertisement, buyOrSell, advertisement.location_string()))
        startActivity(Intent.createChooser(shareIntent, getString(R.string.text_chooser_share_to)))
    }

    private fun editAdvertisement(advertisement: AdvertisementItem) {
        val intent = EditAdvertisementActivity.createStartIntent(this@AdvertisementActivity, advertisement.ad_id())
        startActivityForResult(intent, EditAdvertisementActivity.REQUEST_CODE)
    }

    companion object {

        val EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID"
        val REQUEST_CODE = 10939
        val RESULT_DELETED = 837373

        fun createStartIntent(context: Context, adId: String): Intent {
            val intent = Intent(context, AdvertisementActivity::class.java)
            intent.putExtra(EXTRA_AD_ID, adId)
            return intent
        }
    }

    /*@Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == ADVERTISEMENT_LOADER_ID && !TextUtils.isEmpty(adId)) {
            return new CursorLoader(AdvertisementActivity.this, SyncProvider.ADVERTISEMENT_TABLE_URI, null, AdvertisementItem.AD_ID + " = ?", new String[]{adId}, null);
        } else if (id == METHOD_LOADER_ID) {
            return new CursorLoader(AdvertisementActivity.this, SyncProvider.METHOD_TABLE_URI, null, null, null, MethodItem.KEY);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ADVERTISEMENT_LOADER_ID:
                advertisement = AdvertisementItem.getModel(cursor);
                if (advertisement != null && methodItems != null) {
                    setAdvertisement(advertisement, methodItems);
                } else if (advertisement == null) {
                    updateAdvertisement(); // TODO make sure this doesn't just randomly run
                }
                break;
            case METHOD_LOADER_ID:
                methodItems = MethodItem.getModelList(cursor);
                if (advertisement != null && !methodItems.isEmpty()) {
                    setAdvertisement(advertisement, methodItems);
                }
                break;
            default:
                throw new Error("Incorrect loader Id");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }*/
}