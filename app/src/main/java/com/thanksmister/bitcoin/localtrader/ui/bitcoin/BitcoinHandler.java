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

package com.thanksmister.bitcoin.localtrader.ui.bitcoin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;

import javax.inject.Inject;

import timber.log.Timber;

public class BitcoinHandler extends BaseActivity
{
    @Inject
    DbManager dbManager;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
    {
		super.onCreate(savedInstanceState);
       
        Uri data = getIntent().getData();
 
        if (data != null  ) {
            
            Timber.d("BitcoinUriHandler: URL: " + data.toString());
            
            String url = data.toString();
            String scheme = data.getScheme(); // "http"

            Timber.d("BitcoinUriHandler: scheme: " + scheme);
            
            if(!dbManager.isLoggedIn()) {
                showToast("You need to be logged in to perform that action.");
                launchMainApplication();
            } else {
                Intent intent = MainActivity.createStartIntent(getApplicationContext(), url);
                startActivity(intent);
                finish(); 
            }
            
        } else {
            //showToast(getString(R.string.toast_invalid_address));
            launchMainApplication();
        }
	}

    private void launchMainApplication() 
    {
        startActivity(new Intent(BitcoinHandler.this, MainActivity.class));
        finish();
	}
    
    public void showToast(String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}