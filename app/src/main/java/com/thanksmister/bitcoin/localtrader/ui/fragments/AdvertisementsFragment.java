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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.persistence.Method;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.adapters.AdvertisementsAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class AdvertisementsFragment extends BaseFragment  {

    private static final int ADVERTISEMENT_LOADER_ID = 1;
    private static final int METHOD_LOADER_ID = 2;

    RecyclerView recycleView;

    @Inject
    protected SharedPreferences sharedPreferences;

    private AdvertisementsAdapter itemAdapter;
    private List<AdvertisementItem> advertisements = Collections.emptyList();
    private List<Method> methods = Collections.emptyList();
    //private AdvertisementObserver advertisementObserver;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AdvertisementsFragment newInstance() {
        return new AdvertisementsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // can't retain nested fragments
        setRetainInstance(false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        itemAdapter = getAdapter();
        recycleView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleView.setLayoutManager(linearLayoutManager);

        ItemClickSupport.addTo(recycleView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                AdvertisementItem advertisement = getAdapter().getItemAt(position);
                if (advertisement != null && !TextUtils.isEmpty(advertisement.ad_id())) {
                    showAdvertisement(getAdapter().getItemAt(position));
                }
            }
        });

        itemAdapter = new AdvertisementsAdapter(getActivity(), new AdvertisementsAdapter.OnItemClickListener() {
            @Override
            public void onSearchButtonClicked() {
                showSearchScreen();
            }

            @Override
            public void onAdvertiseButtonClicked() {
                createAdvertisementScreen();
            }
        });

        recycleView.setAdapter(itemAdapter);
    }

    private void setupList(List<AdvertisementItem> advertisementItems, List<Method> methodItems) {
        if (isAdded()) {
            itemAdapter.replaceWith(advertisementItems, methodItems);
            recycleView.setAdapter(itemAdapter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_dashboard_items, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //advertisementObserver = new AdvertisementObserver(new Handler());
        if (getActivity() != null) {
            //getActivity().getContentResolver().registerContentObserver(SyncProvider.ADVERTISEMENT_TABLE_URI, true, advertisementObserver);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            //getActivity().getContentResolver().unregisterContentObserver(advertisementObserver);
        }
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

    private AdvertisementsAdapter getAdapter() {
        return itemAdapter;
    }

    private void showAdvertisement(@NonNull AdvertisementItem advertisement) {
        Intent intent = AdvertisementActivity.createStartIntent(getActivity(), advertisement.ad_id());
        startActivityForResult(intent, AdvertisementActivity.REQUEST_CODE);
    }

    private void createAdvertisementScreen() {
       /* showAlertDialog(new AlertDialogEvent(getString(R.string.view_title_advertisements), getString(R.string.dialog_edit_advertisements)), new Action0() {
            @Override
            public void call() {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Action0() {
            @Override
            public void call() {
                // na-da
            }
        });*/
    }

    protected void showSearchScreen() {
        if (isAdded() && getActivity() != null) {
            ((MainActivity) getActivity()).navigateSearchView();
        }
    }

   /* @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == ADVERTISEMENT_LOADER_ID && getActivity() != null) {
            return new CursorLoader(getActivity(), SyncProvider.ADVERTISEMENT_TABLE_URI, null, null, null, null);
        } else if (id == METHOD_LOADER_ID && getActivity() != null) {
            return new CursorLoader(getActivity(), SyncProvider.METHOD_TABLE_URI, null, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ADVERTISEMENT_LOADER_ID:
                advertisements = AdvertisementItem.getModelList(cursor);
                if (methods != null && advertisements != null) {
                    setupList(advertisements, methods);
                }
                break;
            case METHOD_LOADER_ID:
                methods = MethodItem.getModelList(cursor);
                if (methods != null && advertisements != null) {
                    setupList(advertisements, methods);
                }
                break;
            default:
                throw new Error("Incorrect loader Id");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }*/

   /* private class AdvertisementObserver extends ContentObserver {
        AdvertisementObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (selfChange && getActivity() != null) {
                getActivity().getSupportLoaderManager().restartLoader(ADVERTISEMENT_LOADER_ID, null, AdvertisementsFragment.this);
            }
        }
    }*/
}