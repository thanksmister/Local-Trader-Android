/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.ui.about;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.main.MainActivity;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class AboutFragment extends BaseFragment implements AboutView
{
    private static final String ARG_SECTION_NUMBER = "section_number";
    
    @Inject
    AboutPresenter presenter;

    @OnClick(R.id.sendFeedbackButton)
    public void sendButtonClicked()
    {
       feedback();
    }

    @OnClick(R.id.donateBitcoinButton)
    public void donateButtonClicked()
    {
        //donate();
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

    public static AboutFragment newInstance(int sectionNumber)
    {
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public AboutFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
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
    }

    @Override
    public Context getContext()
    {
        return getActivity();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        presenter.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        ButterKnife.reset(this);

        presenter.onDestroy();
    }

    @Override
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new AboutModule(this));
    }

    protected void rate()
    {
        final String appName = Constants.GOOGLE_PLAY_RATING;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
        }
    }

    /*protected void donate()
    {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("bitcoin:" + Constants.BITCOIN_ADDRESS + "?amount=" + ".01")));
        } catch (android.content.ActivityNotFoundException ex) {
            Intent sendEmail = new Intent(Intent.ACTION_SEND);
            sendEmail.setType("text/plain");
            sendEmail.putExtra(Intent.EXTRA_SUBJECT, "Donation Bitcoin Address");
            sendEmail.putExtra(Intent.EXTRA_TEXT, Constants.BITCOIN_ADDRESS);
            startActivity(Intent.createChooser(sendEmail, "Share using:"));
        }
    }*/

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

    @Override
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return false;
            }
        });
    }
}
