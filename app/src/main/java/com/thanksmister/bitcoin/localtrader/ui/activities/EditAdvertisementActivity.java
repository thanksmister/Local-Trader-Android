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
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import timber.log.Timber;

public class EditAdvertisementActivity extends BaseActivity {

    public static final String EXTRA_ADVERTISEMENT_ID = "com.thanksmister.extras.EXTRA_ADVERTISEMENT_ID";
    public static final String EXTRA_EDITED_ADVERTISEMENT = "com.thanksmister.extras.EXTRA_EDITED_ADVERTISEMENT";

    public static final int REQUEST_CODE = 10937;
    public static final int RESULT_UPDATED = 72322;

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

    private Advertisement advertisement;
    private Advertisement editAdvertisement;
    private String adId;

    public static Intent createStartIntent(Context context, int adId) {
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        toggleNavButtons(getString(R.string.button_save_changes));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
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
       getMenuInflater().inflate(R.menu.edit, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void handleNetworkDisconnect() {
        if (NetworkUtils.isNetworkConnected(EditAdvertisementActivity.this)) {
            toast(getString(R.string.error_no_internet));
        }
    }

    private void setInitialAdvertisementViews(Advertisement editAdvertisement, Advertisement advertisement) {
        this.advertisement = advertisement;
        this.editAdvertisement = editAdvertisement;
        setAdvertisementOnView(editAdvertisement);
    }

    public void setAdvertisementOnView(@NonNull Advertisement editAdvertisement) {

        activeCheckBox.setChecked(editAdvertisement.getVisible());


        if (TradeUtils.ALTCOIN_ETH.equals(editAdvertisement.getOnlineProvider())) {
            editMinimumAmountCurrency.setText(getString(R.string.eth));
            editMaximumAmountCurrency.setText(getString(R.string.eth));
        } else {
            editMinimumAmountCurrency.setText(editAdvertisement.getCurrency());
            editMaximumAmountCurrency.setText(editAdvertisement.getCurrency());
        }

        if (!TextUtils.isEmpty(editAdvertisement.getMinAmount())) {
            editMinimumAmount.setText(editAdvertisement.getMinAmount());
        }

        if (!TextUtils.isEmpty(editAdvertisement.getMaxAmount())) {
            editMaximumAmount.setText(editAdvertisement.getMaxAmount());
        }

        if (!TextUtils.isEmpty(editAdvertisement.getPriceEquation())) {
            editPriceEquation.setText(editAdvertisement.getPriceEquation());
        }

        if (!TextUtils.isEmpty(editAdvertisement.getMessage())) {
            editMessageText.setText(editAdvertisement.getMessage());
        }
    }

    public boolean validateChanges() {

        editAdvertisement.setVisible(activeCheckBox.isChecked());

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

        editAdvertisement.setMessage(editMessageText.getText().toString());
        editAdvertisement.setPriceEquation(equation);
        editAdvertisement.setMinAmount(String.valueOf(TradeUtils.Companion.convertCurrencyAmount(min)));
        editAdvertisement.setMaxAmount(String.valueOf(TradeUtils.Companion.convertCurrencyAmount(max)));

        return true;
    }

    /*@Deprecated
    public Advertisement getEditAdvertisement() {
        Advertisement advertisement = new Advertisement();
        String advertisementJson = preference.getString("editAdvertisement", null);
        Timber.d("getEditAdvertisement: " + advertisementJson);
        if (!TextUtils.isEmpty(advertisementJson)) {
            advertisement = new Gson().fromJson(advertisementJson, Advertisement.class);
        }
        return advertisement;
    }
*/
    // TODO save this in the view model
    /*public void setEditAdvertisement(Advertisement advertisement) {
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
                if ((editAdvertisement.getAccountInfo() != null && !editAdvertisement.getAccountInfo().equals(advertisement.getAccountInfo()))
                        || (editAdvertisement.getPriceEquation() != null && !editAdvertisement.getPriceEquation().equals(advertisement.getPriceEquation()))
                        || (editAdvertisement.getBankName() != null && !editAdvertisement.getBankName().equals(advertisement.getBankName()))
                        || (editAdvertisement.getMessage() != null && !editAdvertisement.getMessage().equals(advertisement.getMessage()))
                        || (editAdvertisement.getMinAmount() != null && !editAdvertisement.getMinAmount().equals(advertisement.getMinAmount()))
                        || (editAdvertisement.getMaxAmount() != null && !editAdvertisement.getMaxAmount().equals(advertisement.getMaxAmount()))
                        || editAdvertisement.getVisible() != advertisement.getVisible()) {
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
            showProgressDialog(getString(R.string.dialog_saving_changes), true);
//            dataService.updateAdvertisement(editAdvertisement)
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new Action1<JSONObject>() {
//                        @Override
//                        public void call(final JSONObject jsonObject) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    hideProgressDialog();
//                                    advertisementSaved();
//                                }
//                            });
//                        }
//                    }, new Action1<Throwable>() {
//                        @Override
//                        public void call(final Throwable throwable) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    hideProgressDialog();
//                                    showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), throwable.getMessage()), new Action0() {
//                                        @Override
//                                        public void call() {
//                                            advertisementCanceled();
//                                        }
//                                    });
//                                }
//                            });
//
//                        }
//                    });
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