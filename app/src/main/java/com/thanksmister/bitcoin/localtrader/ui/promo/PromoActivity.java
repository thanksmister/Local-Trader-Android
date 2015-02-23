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

package com.thanksmister.bitcoin.localtrader.ui.promo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.ui.about.AboutModule;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class PromoActivity extends BaseActivity implements PromoView
{
    @Inject
    PromoPresenter presenter;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    @InjectView(R.id.pager)
    ViewPager viewPager;

    @InjectView(R.id.indicator)
    CirclePageIndicator circlePageIndicator;

    @OnClick(R.id.registerButton)
    public void registerButtonClicked()
    {
        presenter.showRegistration();
    }

    @OnClick(R.id.loginButton)
    public void loginButtonClicked()
    {
        presenter.showLoginView();
    }

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter pagerAdapter;


    public static Intent createStartIntent(Context context)
    {
        Intent intent = new Intent(context, PromoActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_promo);

        ButterKnife.inject(this);

        pagerAdapter = new ScreenPagerAdapter(getContext());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer(false, new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View page, float position) {
                final float normalizedposition = Math.abs(Math.abs(position) - 1);
                page.setAlpha(normalizedposition);
                page.setScaleX(normalizedposition / 2 + 0.5f);
                page.setScaleY(normalizedposition / 2 + 0.5f);
            }
        });

        circlePageIndicator.setViewPager(viewPager);
        circlePageIndicator.setFillColor(getContext().getResources().getColor(R.color.red_light_pressed));
        circlePageIndicator.setStrokeColor(getContext().getResources().getColor(R.color.gray_pressed));
        circlePageIndicator.setRadius(12);
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
        return Arrays.<Object>asList(new PromoModule(this));
    }

    @Override
    public Context getContext()
    {
        return this;
    }
}
