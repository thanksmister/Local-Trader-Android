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

package com.thanksmister.bitcoin.localtrader.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import javax.inject.Inject;

import timber.log.Timber;

public class EditAdvertisementActivity extends BaseActivity {

    private static final int ADVERTISEMENT_LOADER_ID = 1;
    public static final String EXTRA_ADVERTISEMENT_ID = "com.thanksmister.extras.EXTRA_ADVERTISEMENT_ID";
    public static final String EXTRA_EDITED_ADVERTISEMENT = "com.thanksmister.extras.EXTRA_EDITED_ADVERTISEMENT";

    public static final int REQUEST_CODE = 10937;
    public static final int RESULT_UPDATED = 72322;


    Toolbar toolbar;


    Button nextButton;

    TextView editMaximumAmountCurrency;


    TextView editMinimumAmountCurrency;


    EditText editMaximumAmount;


    EditText editMinimumAmount;


    EditText editPriceEquation;


    EditText editMessageText;


    CheckBox activeCheckBox;


    public void nextButtonClicked() {
        validateChangesAndCommit(editAdvertisement);
    }

    private AdvertisementItem advertisement;
    private Advertisement editAdvertisement;
    private String adId;

    public static Intent createStartIntent(Context context, String adId) {
        Intent intent = new Intent(context, EditAdvertisementActivity.class);
        intent.putExtra(EXTRA_ADVERTISEMENT_ID, adId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_advertisement);

        if (savedInstanceState == null) {
            adId = getIntent().getStringExtra(EXTRA_ADVERTISEMENT_ID);
        } else {
            adId = savedInstanceState.getString(EXTRA_ADVERTISEMENT_ID);
        }

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
            }
        }

        //getSupportLoaderManager().restartLoader(ADVERTISEMENT_LOADER_ID, null, this);
        toggleNavButtons(getString(R.string.button_save_changes));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //getSupportLoaderManager().destroyLoader(ADVERTISEMENT_LOADER_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ADVERTISEMENT_ID, adId);
    }

    @Override
    public void onBackPressed() {
        toast(getString(R.string.text_post_update_canceled));
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            advertisementCanceled();
            return true;
        } else if (item.getItemId() == R.id.action_advertisement) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_EDIT_URL + adId)));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(EditAdvertisementActivity.this, getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.edit);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void handleNetworkDisconnect() {
        if (NetworkUtils.isNetworkConnected(EditAdvertisementActivity.this)) {
            snack(getString(R.string.error_no_internet), false);
        }
    }

    private void setInitialAdvertisementViews(Advertisement editAdvertisement, AdvertisementItem advertisement) {
        this.advertisement = advertisement;
        this.editAdvertisement = editAdvertisement;
        setAdvertisementOnView(editAdvertisement);
    }

    public void setAdvertisementOnView(@NonNull Advertisement editAdvertisement) {

        activeCheckBox.setChecked(editAdvertisement.visible);

        if (TradeUtils.ALTCOIN_ETH.equals(editAdvertisement.online_provider)) {
            editMinimumAmountCurrency.setText(getString(R.string.eth));
            editMaximumAmountCurrency.setText(getString(R.string.eth));
        } else {
            editMinimumAmountCurrency.setText(editAdvertisement.currency);
            editMaximumAmountCurrency.setText(editAdvertisement.currency);
        }

        if (!TextUtils.isEmpty(editAdvertisement.min_amount)) {
            editMinimumAmount.setText(editAdvertisement.min_amount);
        }

        if (!TextUtils.isEmpty(editAdvertisement.max_amount)) {
            editMaximumAmount.setText(editAdvertisement.max_amount);
        }

        if (!TextUtils.isEmpty(editAdvertisement.price_equation)) {
            editPriceEquation.setText(editAdvertisement.price_equation);
        }

        if (!TextUtils.isEmpty(editAdvertisement.message)) {
            editMessageText.setText(editAdvertisement.message);
        }
    }

    public boolean validateChanges() {

        editAdvertisement.visible = activeCheckBox.isChecked();

        String min = editMinimumAmount.getText().toString();
        String max = editMaximumAmount.getText().toString();
        String equation = editPriceEquation.getText().toString();

        if (TextUtils.isEmpty(equation)) {
            toast(getString(R.string.toast_price_equation_blank));
            return false;
        } else if (TextUtils.isEmpty(min)) {
            toast(getString(R.string.toast_minimum_amount));
            return false;
        } else if (TextUtils.isEmpty(max)) {
            toast(getString(R.string.toast_maximum_amount));
            return false;
        }

        editAdvertisement.message = editMessageText.getText().toString();
        editAdvertisement.price_equation = equation;
        editAdvertisement.min_amount = String.valueOf(TradeUtils.convertCurrencyAmount(min));
        editAdvertisement.max_amount = String.valueOf(TradeUtils.convertCurrencyAmount(max));

        return true;
    }

    /*public Advertisement getEditAdvertisement() {
        Advertisement advertisement = new Advertisement();
        String advertisementJson = preference.getString("editAdvertisement", null);
        Timber.d("getEditAdvertisement: " + advertisementJson);
        if (!TextUtils.isEmpty(advertisementJson)) {
            advertisement = new Gson().fromJson(advertisementJson, Advertisement.class);
        }
        return advertisement;
    }

    public void setEditAdvertisement(Advertisement advertisement) {
        String editString = new Gson().toJson(advertisement);
        Timber.d("setEditAdvertisement: " + editString);
        preference.putString("editAdvertisement", editString);
    }*/

    private void toggleNavButtons(String nextButtonTitle) {
        nextButton.setText(nextButtonTitle);
    }

    /**
     * Check the edit editAdvertisement to see if there are any changes to commit.
     *
     * @param editAdvertisement Advertisement
     */
    private void validateChangesAndCommit(Advertisement editAdvertisement) {
        if (!validateChanges()) {
            return;
        }
        boolean commitChanges = false;
        if (advertisement != null) {
            try {
                if ((editAdvertisement.account_info != null && !editAdvertisement.account_info.equals(advertisement.account_info()))
                        || (editAdvertisement.price_equation != null && !editAdvertisement.price_equation.equals(advertisement.price_equation()))
                        || (editAdvertisement.bank_name != null && !editAdvertisement.bank_name.equals(advertisement.bank_name()))
                        || (editAdvertisement.message != null && !editAdvertisement.message.equals(advertisement.message()))
                        || (editAdvertisement.min_amount != null && !editAdvertisement.min_amount.equals(advertisement.min_amount()))
                        || (editAdvertisement.max_amount != null && !editAdvertisement.max_amount.equals(advertisement.max_amount()))
                        || editAdvertisement.visible != advertisement.visible()) {
                    commitChanges = true;
                }
            } catch (NullPointerException e) {
                toast(getString(R.string.toast_error_updat_advertisement));
                Intent returnIntent = getIntent();
                setResult(RESULT_CANCELED, returnIntent);
                finish();
                return;
            }
        } else {
            advertisementCanceled();
            return;
        }

        Timber.d("commitChanges: " + commitChanges);
        Timber.d("\n\n\neditAdvertisement: " + new Gson().toJson(advertisement));
        Timber.d("\n\n\neditAdvertisement: " + new Gson().toJson(editAdvertisement));

        if (commitChanges) {
            showProgressDialog(new ProgressDialogEvent(getString(R.string.dialog_saving_changes)), true);
           /* dataService.updateAdvertisement(editAdvertisement)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(this.<JSONObject>bindUntilEvent(ActivityEvent.PAUSE))
                    .subscribe(new Action1<JSONObject>() {
                        @Override
                        public void call(final JSONObject jsonObject) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgressDialog();
                                    advertisementSaved();
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
                                    showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), throwable.getMessage()), new Action0() {
                                        @Override
                                        public void call() {
                                            advertisementCanceled();
                                        }
                                    });
                                }
                            });

                        }
                    });*/
        } else {
            advertisementCanceled();
        }
    }

    private void advertisementCanceled() {
        hideProgressDialog();
        toast(getString(R.string.text_post_update_canceled));
        Intent returnIntent = getIntent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }

    /**
     * Notify the calling activity that the editAdvertisement has been updated
     */
    private void advertisementSaved() {
        hideProgressDialog();
        toast(getString(R.string.message_advertisement_changed));
        Intent returnIntent = getIntent();
        setResult(RESULT_UPDATED, returnIntent);
        finish();
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
                    AdvertisementItem advertisement = AdvertisementItem.getModel(cursor);
                    if (advertisement != null) {
                        Advertisement editAdvertisement = new Advertisement().convertAdvertisementItemToAdvertisement(advertisement);
                        setInitialAdvertisementViews(editAdvertisement, advertisement);
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