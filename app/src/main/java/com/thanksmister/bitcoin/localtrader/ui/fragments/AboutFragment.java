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

package com.thanksmister.bitcoin.localtrader.ui.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.AndroidRuntimeException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.lang.reflect.Field;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.constants.Constants.BITCOIN_ADDRESS;

public class AboutFragment extends BaseFragment {
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    SharedPreferences sharedPreferences;

    @OnClick(R.id.guidesButton)
    public void guidesButtonClicked() {
        guides();
    }

    @OnClick(R.id.sendFeedbackButton)
    public void sendButtonClicked() {
        gitHub();
    }

    @OnClick(R.id.sendAccountButton)
    public void sendAccountButton() {
        support();
    }

    @OnClick(R.id.rateApplicationButton)
    public void rateButtonClicked() {
        rate();
    }

    @OnClick(R.id.joinCommunityButton)
    public void communityButtonClicked() {
        join();
    }

    @OnClick(R.id.licenseButton)
    public void licenseButtonClicked() {
        showLicense();
    }

    @OnClick(R.id.donateButton)
    public void donateButtonClicked() {
        donateBitcoin();
    }

    private String versionText;

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    public AboutFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.view_about, container, false);
        ButterKnife.bind(this, fragmentView);
        return fragmentView;
    }

    @Override
    public void onViewCreated(View fragmentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragmentView, savedInstanceState);
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            versionText = " v" + packageInfo.versionName;
            TextView versionName = (TextView) getActivity().findViewById(R.id.versionName);
            versionName.setText(versionText);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e.getMessage());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupToolbar();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupToolbar() {
        if(getActivity() != null) {
            ((MainActivity) getActivity()).setSupportActionBar(toolbar);
            final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
            ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
            ab.setTitle(getString(R.string.view_title_about));
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    protected void rate() {
        final String appName = Constants.GOOGLE_PLAY_RATING;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
        } catch (android.content.ActivityNotFoundException ex) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
        }
    }

    protected void gitHub() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GITHUB)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
        }
        /*Intent Email = new Intent(Intent.ACTION_SENDTO);
        Email.setType("text/email");
        Email.setData(Uri.parse("mailto:" + Constants.EMAIL_ADDRESS));
        Email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_to_subject_text) + " " + versionText);
        startActivity(Intent.createChooser(Email, getString(R.string.mail_subject_text)));*/
    }

    protected void guides() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GUIDES_URL)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
        }
    }

    protected void support() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.SUPPORT_URL)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
        }
    }

    protected void join() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/LocalTrader/")));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Timber.e(e.getLocalizedMessage());
        }
    }

    protected void showLicense() {
        if(getActivity() != null) {
            ((BaseActivity) getActivity()).showAlertDialog(new AlertDialogEvent("License", getString(R.string.license)));
        }
    }

    protected void donateBitcoin() {
        Intent sendIntent;
        try {
            sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(WalletUtils.generateBitCoinURI(BITCOIN_ADDRESS)));
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(sendIntent);
        } catch (ActivityNotFoundException ex) {
            try {
                sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.wallet_my_address_share));
                sendIntent.putExtra(Intent.EXTRA_TEXT, BITCOIN_ADDRESS);
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_using)));
            } catch (AndroidRuntimeException e) {
                Timber.e(e.getMessage());
            }
        }
    }
}