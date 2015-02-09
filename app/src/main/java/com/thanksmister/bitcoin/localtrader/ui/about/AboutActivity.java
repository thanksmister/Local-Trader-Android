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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

public class AboutActivity extends BaseActivity implements AboutView
{
    @Inject
    AboutPresenter presenter;

    @InjectView(R.id.aboutBar)
    Toolbar toolbar;

    @OnClick(R.id.sendFeedbackButton)
    public void sendButtonClicked()
    {
       feedback();
    }

    @OnClick(R.id.donateBitcoinButton)
    public void donateButtonClicked()
    {
        donate();
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

    public static Intent createStartIntent(Context context)
    {
        Intent intent = new Intent(context, AboutActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_about);

        ButterKnife.inject(this);

        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.app_name));
            setToolBarMenu(toolbar);
        }

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            TextView versionName = (TextView) findViewById(R.id.versionName);
            versionName.setText(" v" + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e.getMessage());
        }
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

    /*@Override
    public void startActivity(Intent intent) 
    {
        try {
        *//* First attempt at fixing an HTC broken by evil Apple patents. *//*
            if (intent.getComponent() != null
                    && ".HtcLinkifyDispatcherActivity".equals(intent.getComponent().getShortClassName()))
                intent.setComponent(null);
            super.startActivity(intent);
        } catch (ActivityNotFoundException e) {
        *//*
         * Probably an HTC broken by evil Apple patents. This is not perfect,
         * but better than crashing the whole application.
         *//*
            super.startActivity(Intent.createChooser(intent, null));
        }
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    protected void donate()
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
        showAlertDialog(new AlertDialogEvent("License", getString(R.string.license)));
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

    @Override
    public Context getContext()
    {
        return this;
    }
}
