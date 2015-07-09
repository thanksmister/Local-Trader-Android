/*
 * Copyright (c) 2015 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

public class AboutFragment extends BaseFragment 
{
    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    
    @OnClick(R.id.sendFeedbackButton)
    public void sendButtonClicked()
    {
       feedback();
    }

    @OnClick(R.id.donateBitcoinButton)
    public void donateButtonClicked()
    {
        changeTipMe();
    }

    @OnClick(R.id.rateApplicationButton)
    public void rateButtonClicked()
    {
        rate();
    }

    @OnClick(R.id.joinCommunityButton)
    public void communityButtonClicked()
    {
        join();
    }

    @OnClick(R.id.licenseButton)
    public void licenseButtonClicked()
    {
        showLicense();
    }

    public static AboutFragment newInstance()
    {
        return new AboutFragment();
    }

    public AboutFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View fragmentView = inflater.inflate(R.layout.view_about, container, false);

        ButterKnife.inject(this, fragmentView);

        return fragmentView;
    }

    @Override
    public void onViewCreated(View fragmentView, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(fragmentView, savedInstanceState);

        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            TextView versionName = (TextView) getActivity().findViewById(R.id.versionName);
            versionName.setText(" v" + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e.getMessage());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setupToolbar();
    }

    private void setupToolbar()
    {
        ((MainActivity) getActivity()).setSupportActionBar(toolbar);

        // Show menu icon
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setTitle(getString(R.string.view_title_about));
        ab.setDisplayHomeAsUpEnabled(true);
    }
    
    protected void rate()
    {
        final String appName = Constants.GOOGLE_PLAY_RATING;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
        } catch (android.content.ActivityNotFoundException ex) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
        }
    }

    protected void donate()
    {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("bitcoin:" + Constants.BITCOIN_ADDRESS + "?amount=" + ".01")));
        } catch (android.content.ActivityNotFoundException ex) {
            changeTipMe();
        } catch (Exception e) {
            Timber.e(e.getLocalizedMessage());
        }
    }

    protected void changeTipMe()
    {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.CHANGE_TIP_ADDRESS)));
        } catch (android.content.ActivityNotFoundException ex) {
            Intent sendEmail = new Intent(Intent.ACTION_SEND);
            sendEmail.setType("text/plain");
            sendEmail.putExtra(Intent.EXTRA_SUBJECT, "Change Tip Address");
            sendEmail.putExtra(Intent.EXTRA_TEXT, Constants.CHANGE_TIP_ADDRESS);
            startActivity(Intent.createChooser(sendEmail, "Share using:"));
        }
    }

    protected void feedback()
    {
        Intent Email = new Intent(Intent.ACTION_SENDTO);
        Email.setType("text/email");
        Email.setData(Uri.parse("mailto:" + Constants.EMAIL_ADDRESS));
        Email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_to_subject_text));
        startActivity(Intent.createChooser(Email, getString(R.string.mail_subject_text)));
    }

    protected void join()
    {
        try {
            Intent googleIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GOOGLE_PLUS_COMMUNITY));
            googleIntent.setPackage("com.google.android.apps.plus");
            startActivity(googleIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GOOGLE_PLUS_COMMUNITY)));
        }
    }

    protected void showLicense()
    {
        ((BaseActivity) getActivity()).showAlertDialog(new AlertDialogEvent("License", getString(R.string.license)));
    }
}
