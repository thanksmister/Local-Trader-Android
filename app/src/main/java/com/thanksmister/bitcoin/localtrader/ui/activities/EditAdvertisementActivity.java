/*
 * Copyright (c) 2017 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Button;

import com.google.gson.Gson;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.ui.fragments.BaseEditFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditInfoFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditMoreInfoFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditOnlineFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditSecurityFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditTypeFragment;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;

import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class EditAdvertisementActivity extends BaseActivity implements BaseEditFragment.OnFragmentInteractionListener {

    public static final String EXTRA_ADVERTISEMENT = "com.thanksmister.extras.EXTRA_ADVERTISEMENT";
    public static final String EXTRA_EDITED_ADVERTISEMENT = "com.thanksmister.extras.EXTRA_EDITED_ADVERTISEMENT";
    public static final String EXTRA_CREATE = "com.thanksmister.extras.EXTRA_CREATE";

    private static final String INFORMATION_FRAGMENT = "com.thanksmister.fragment.INFORMATION_FRAGMENT";
    private static final String SECURITY_FRAGMENT = "com.thanksmister.fragment.SECURITY_FRAGMENT";
    private static final String LIQUIDITY_FRAGMENT = "com.thanksmister.fragment.LIQUIDITY_FRAGMENT";
    private static final String TRADE_TYPE_FRAGMENT = "com.thanksmister.fragment.TRADE_TYPE_FRAGMENT";
    private static final String MORE_INFO_FRAGMENT = "com.thanksmister.fragment.MORE_INFO_FRAGMENT";
    private static final String ONLINE_OPTIONS_FRAGMENT = "com.thanksmister.fragment.ONLINE_OPTIONS_FRAGMENT";

    public static final int REQUEST_CODE = 10937;
    public static final int RESULT_UPDATED = 72322;
    public static final int RESULT_CREATED = 72323;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.nextButton)
    Button nextButton;

    @Inject
    public DataService dataService;

    @OnClick(R.id.nextButton)
    public void nextButtonClicked() {
        validateChangesAndNavigateNext();
    }

    @InjectView(R.id.previousButton)
    Button previousButton;

    @OnClick(R.id.previousButton)
    public void previousButtonButtonClicked() {
        validateChangesAndNavigatePrevious();
    }

    private AdvertisementItem advertisement;
    // this is sort of our model used for updates
    private Advertisement editAdvertisement;
    private Fragment fragment;
    private boolean create;
    private String[] tagPaths;

    public static Intent createStartIntent(Context context, AdvertisementItem advertisement, boolean create) {
        Intent intent = new Intent(context, EditAdvertisementActivity.class);
        intent.putExtra(EXTRA_ADVERTISEMENT, advertisement);
        intent.putExtra(EXTRA_CREATE, create);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_advertisement);

        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            create = getIntent().getBooleanExtra(EXTRA_CREATE, false);
            advertisement = getIntent().getParcelableExtra(EXTRA_ADVERTISEMENT);
        } else {
            create = savedInstanceState.getBoolean(EXTRA_CREATE);
            advertisement = savedInstanceState.getParcelable(EXTRA_ADVERTISEMENT);
            editAdvertisement = savedInstanceState.getParcelable(EXTRA_EDITED_ADVERTISEMENT);
        }

        if (advertisement != null && editAdvertisement == null) {
            editAdvertisement = new Advertisement();
            editAdvertisement = editAdvertisement.convertAdvertisementItemToAdvertisement(advertisement);
        } else if (editAdvertisement == null) {
            editAdvertisement = new Advertisement();
        }

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                //getSupportActionBar().setTitle("Edit Advertisement");
            }
        }

        if (create) {
            if (TradeType.ONLINE_SELL.equals(editAdvertisement.trade_type)) {
                tagPaths = new String[]{TRADE_TYPE_FRAGMENT, MORE_INFO_FRAGMENT, ONLINE_OPTIONS_FRAGMENT, SECURITY_FRAGMENT};
            } else {
                tagPaths = new String[]{TRADE_TYPE_FRAGMENT, MORE_INFO_FRAGMENT, SECURITY_FRAGMENT};
            }
        } else {
            if (TradeType.ONLINE_SELL.name().equals(advertisement.trade_type())) {
                tagPaths = new String[]{INFORMATION_FRAGMENT, ONLINE_OPTIONS_FRAGMENT, SECURITY_FRAGMENT};
            } else {
                tagPaths = new String[]{INFORMATION_FRAGMENT, SECURITY_FRAGMENT};
            }
        }

        navigateNextFragment(editAdvertisement, create);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ADVERTISEMENT, advertisement);
        outState.putParcelable(EXTRA_EDITED_ADVERTISEMENT, editAdvertisement);
        outState.putBoolean(EXTRA_CREATE, create);
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
            advertisementCanceled(create);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void handleNetworkDisconnect() {
        if (!NetworkUtils.isNetworkConnected(EditAdvertisementActivity.this)) {
            snack(getString(R.string.error_no_internet), false);
        }
    }

    /**
     * Check that the form values are complete and if so navigates
     */
    private void validateChangesAndNavigateNext() {
        if (fragment != null) {
            boolean valid = ((BaseEditFragment) fragment).validateChangesAndSave();
            if(valid) {
                editAdvertisement = ((BaseEditFragment) fragment).getAdvertisement();
                Timber.d("editAdvertisement: location: " + editAdvertisement.location);
                // update the paths if user has selected a new online trade
                if(create) {
                    if (TradeType.ONLINE_SELL.equals(editAdvertisement.trade_type)) {
                        tagPaths = new String[]{TRADE_TYPE_FRAGMENT, MORE_INFO_FRAGMENT, ONLINE_OPTIONS_FRAGMENT, SECURITY_FRAGMENT};
                    } else {
                        tagPaths = new String[]{TRADE_TYPE_FRAGMENT, MORE_INFO_FRAGMENT, SECURITY_FRAGMENT};
                    }
                }
                String path = fragment.getTag();
                if (path.equals(tagPaths[tagPaths.length - 1])) {
                    checkCommitChanges(editAdvertisement);
                } else {
                    navigateNextFragment(editAdvertisement, create);
                }
            }
        }
    }

    private void validateChangesAndNavigatePrevious() {
        if (fragment != null) {
            boolean valid = ((BaseEditFragment) fragment).validateChangesAndSave();
            if(valid) {
                editAdvertisement = ((BaseEditFragment) fragment).getAdvertisement();
                navigatePreviousFragment(editAdvertisement, create);
            }
        }
    }

    private void navigateNextFragment(Advertisement advertisement, final boolean create) {
        if (create) {
            if (fragment == null) {
                toggleNavButtons(false, true, getString(R.string.button_next));
                fragment = EditTypeFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, TRADE_TYPE_FRAGMENT).commit();
            } else if (TRADE_TYPE_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(true, true, getString(R.string.button_next));
                fragment = EditMoreInfoFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, MORE_INFO_FRAGMENT).commit();
            } else if (MORE_INFO_FRAGMENT.equals(fragment.getTag()) && TradeType.ONLINE_SELL.equals(advertisement.trade_type)) {
                toggleNavButtons(true, true, getString(R.string.button_next));
                fragment = EditOnlineFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, ONLINE_OPTIONS_FRAGMENT).commit();
            } else if (MORE_INFO_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(true, true, getString(R.string.button_save_changes));
                fragment = EditSecurityFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, SECURITY_FRAGMENT).commit();
            } else if (ONLINE_OPTIONS_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(true, true, getString(R.string.button_save_changes));
                fragment = EditSecurityFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, SECURITY_FRAGMENT).commit();
            } else {
                toggleNavButtons(false, false, getString(R.string.button_save_changes));
            }
        } else {
            if (fragment == null) {
                toggleNavButtons(false, true, getString(R.string.button_next));
                fragment = EditInfoFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, INFORMATION_FRAGMENT).commit();
            } else if (INFORMATION_FRAGMENT.equals(fragment.getTag()) && TradeType.ONLINE_SELL.equals(advertisement.trade_type)) {
                toggleNavButtons(true, true, getString(R.string.button_next));
                fragment = EditOnlineFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, ONLINE_OPTIONS_FRAGMENT).commit();
            } else if (INFORMATION_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(true, true, getString(R.string.button_save_changes));
                fragment = EditSecurityFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, SECURITY_FRAGMENT).commit();
            } else if (ONLINE_OPTIONS_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(true, true, getString(R.string.button_save_changes));
                fragment = EditSecurityFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, SECURITY_FRAGMENT).commit();
            } else {
                toggleNavButtons(false, false, getString(R.string.button_save_changes));
            }
        }
    }

    private void navigatePreviousFragment(final Advertisement advertisement, boolean create) {

        Timber.d("navigatePreviousFragment fragment: " + fragment);
        Timber.d("navigatePreviousFragment create: " + create);
        Timber.d("navigatePreviousFragment trade type: " + advertisement.trade_type);
        Timber.d("navigatePreviousFragment fragment tag: " + fragment.getTag());

        if (create) {
            if (MORE_INFO_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(false, true, getString(R.string.button_next));
                fragment = EditTypeFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, TRADE_TYPE_FRAGMENT).commit();
            } else if (ONLINE_OPTIONS_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(true, true, getString(R.string.button_next));
                fragment = EditMoreInfoFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, MORE_INFO_FRAGMENT).commit();
            } else if (SECURITY_FRAGMENT.equals(fragment.getTag()) && TradeType.ONLINE_SELL.equals(advertisement.trade_type)) {
                toggleNavButtons(true, true, getString(R.string.button_next));
                fragment = EditOnlineFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, ONLINE_OPTIONS_FRAGMENT).commit();
            } else if (SECURITY_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(true, true, getString(R.string.button_next));
                fragment = EditMoreInfoFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, MORE_INFO_FRAGMENT).commit();
            } else {
                toggleNavButtons(false, true, getString(R.string.button_next));
            }
        } else {
            if (ONLINE_OPTIONS_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(false, true, getString(R.string.button_next));
                fragment = EditInfoFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, INFORMATION_FRAGMENT).commit();
            } else if (SECURITY_FRAGMENT.equals(fragment.getTag()) && TradeType.ONLINE_SELL.equals(advertisement.trade_type)) {
                toggleNavButtons(true, true, getString(R.string.button_next));
                fragment = EditOnlineFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, ONLINE_OPTIONS_FRAGMENT).commit();
            } else if (SECURITY_FRAGMENT.equals(fragment.getTag())) {
                toggleNavButtons(false, true, getString(R.string.button_next));
                fragment = EditInfoFragment.newInstance(advertisement);
                getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, fragment, INFORMATION_FRAGMENT).commit();
            } else {
                toggleNavButtons(false, true, getString(R.string.button_next));
            }
        }
    }

    private void toggleNavButtons(boolean enablePrev, boolean enableNext, String nextButtonTitle) {
        previousButton.setEnabled(enablePrev);
        nextButton.setEnabled(enableNext);
        nextButton.setText(nextButtonTitle);
    }

    /**
     * Check the edit advertisement to see if there are any changes to commit.
     * @param editAdvertisement Advertisement
     */
    private void checkCommitChanges(Advertisement editAdvertisement) {
        boolean commitChanges = false;
        if(advertisement == null && create) {
            commitChanges = true;
        } else if(advertisement != null && !create){
            try {
                if (!editAdvertisement.online_provider.equals(advertisement.online_provider())
                        || (editAdvertisement.currency != null && !editAdvertisement.currency.equals(advertisement.currency()))
                        || (editAdvertisement.account_info != null && !editAdvertisement.account_info.equals(advertisement.account_info()))
                        || (editAdvertisement.phone_number != null && !editAdvertisement.phone_number.equals(advertisement.phone_number()))
                        || (editAdvertisement.price_equation != null && !editAdvertisement.price_equation.equals(advertisement.price_equation()))
                        || (editAdvertisement.bank_name != null && !editAdvertisement.bank_name.equals(advertisement.bank_name()))
                        || (editAdvertisement.email != null && !editAdvertisement.email.equals(advertisement.email()))
                        || (editAdvertisement.reference_type != null && !editAdvertisement.reference_type.equals(advertisement.reference_type()))
                        || (editAdvertisement.atm_model != null && !editAdvertisement.atm_model.equals(advertisement.atm_model()))
                        || !editAdvertisement.trade_type.name().equals(advertisement.trade_type())
                        || (editAdvertisement.message != null && !editAdvertisement.message.equals(advertisement.message()))
                        || (editAdvertisement.city != null && !editAdvertisement.city.equals(advertisement.city()))
                        || (editAdvertisement.reference_type != null && !editAdvertisement.reference_type.equals(advertisement.reference_type()))
                        || (editAdvertisement.country_code != null && !editAdvertisement.country_code.equals(advertisement.country_code()))
                        || (editAdvertisement.location != null && !editAdvertisement.location.equals(advertisement.location_string()))
                        || (editAdvertisement.first_time_limit_btc != null && !editAdvertisement.first_time_limit_btc.equals(advertisement.first_time_limit_btc()))
                        || (editAdvertisement.require_feedback_score != null && !editAdvertisement.require_feedback_score.equals(advertisement.require_feedback_score()))
                        || (editAdvertisement.require_trade_volume != null && !editAdvertisement.require_trade_volume.equals(advertisement.require_trade_volume()))
                        || (editAdvertisement.min_amount != null && !editAdvertisement.min_amount.equals(advertisement.min_amount()))
                        || (editAdvertisement.max_amount != null && !editAdvertisement.max_amount.equals(advertisement.max_amount()))
                        || editAdvertisement.lat != advertisement.lat()
                        || editAdvertisement.lon != advertisement.lon()
                        || editAdvertisement.require_identification != advertisement.require_identification()
                        || editAdvertisement.trusted_required != advertisement.trusted_required()
                        || editAdvertisement.track_max_amount != advertisement.track_max_amount()
                        || editAdvertisement.sms_verification_required != advertisement.sms_verification_required()
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
            advertisementCanceled(create);
            return;
        }

        Timber.d("commitChanges: " + commitChanges);
        Timber.d("\n\n\nadvertisement: " + new Gson().toJson(advertisement));
        Timber.d("\n\n\neditAdvertisement: " + new Gson().toJson(editAdvertisement));
        
        if (commitChanges && !create) {
            showProgressDialog(new ProgressDialogEvent(getString(R.string.dialog_saving_changes)), true);
            dataService.updateAdvertisement(editAdvertisement)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<JSONObject>() {
                        @Override
                        public void call(final JSONObject jsonObject) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgressDialog();
                                    if (Parser.containsError(jsonObject)) {
                                        RetroError error = Parser.parseError(jsonObject);
                                        showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), error.getMessage()), new Action0() {
                                            @Override
                                            public void call() {
                                                advertisementCanceled(create);
                                            }
                                        });
                                    } else {
                                        advertisementSaved(create);
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
                                    showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), throwable.getMessage()), new Action0() {
                                        @Override
                                        public void call() {
                                            advertisementCanceled(create);
                                        }
                                    });
                                }
                            });

                        }
                    });
        } else if (commitChanges) {
            showProgressDialog(new ProgressDialogEvent(getString(R.string.dialog_saving_changes)), true);
            dataService.createAdvertisement(editAdvertisement)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<JSONObject>() {
                        @Override
                        public void call(final JSONObject jsonObject) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgressDialog();
                                    if (Parser.containsError(jsonObject)) {
                                        RetroError error = Parser.parseError(jsonObject);
                                        showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), error.getMessage()), new Action0() {
                                            @Override
                                            public void call() {
                                                advertisementCanceled(create);
                                            }
                                        });
                                    } else {
                                        advertisementSaved(create);
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
                                    RetroError error = DataServiceUtils.createRetroError(throwable);
                                    showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), error.getMessage()), new Action0() {
                                        @Override
                                        public void call() {
                                            advertisementCanceled(create);
                                        }
                                    });
                                }
                            });

                        }
                    });
        } else {
            advertisementCanceled(create);
        }
    }
    
    private void advertisementCanceled(boolean create){
        hideProgressDialog();
        if(create) {
            toast(getString(R.string.text_new_post_canceled));
        } else {
            toast(getString(R.string.text_post_update_canceled));
        }
        Intent returnIntent = getIntent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }

    /**
     * Notify the calling activity that the advertisement has been updated
     */
    private void advertisementSaved(boolean create) {
        hideProgressDialog();
        if(create) {
            toast(getString(R.string.message_advertisement_created)); 
        } else {
            toast(getString(R.string.message_advertisement_changed));
        }
        Intent returnIntent = getIntent();
        setResult(RESULT_UPDATED, returnIntent);
        finish();
    }
}