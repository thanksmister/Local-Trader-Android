package com.thanksmister.bitcoin.localtrader.ui.search;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.ui.MethodAdapter;
import com.thanksmister.bitcoin.localtrader.ui.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.misc.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class SearchFragment extends BaseFragment implements SearchView
{
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";
    
    @Inject
    SearchPresenter presenter;
    
    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(android.R.id.content)
    View content;

    @InjectView(android.R.id.empty)
    View empty;

    @InjectView(R.id.emptyTextView)
    TextView emptyTextView;

    @InjectView(R.id.currentLocation)
    TextView currentLocation;

    @InjectView(R.id.mapLayout)
    View mapLayout;

    @InjectView(R.id.searchLayout)
    View searchLayout;

    @InjectView(R.id.editLocation)
    AutoCompleteTextView editLocation;

    @InjectView(R.id.locationSpinner)
    Spinner locationSpinner;

    @InjectView(R.id.typeSpinner)
    Spinner typeSpinner;

    @InjectView(R.id.paymentMethodSpinner)
    Spinner paymentMethodSpinner;

    @InjectView(R.id.paymentMethodLayout)
    View paymentMethodLayout;

    @OnClick(R.id.clearButton)
    public void clearButtonClicked()
    {
        showSearchLayout();
        presenter.stopLocationCheck();
    }

    @OnClick(R.id.mapButton)
    public void mapButtonClicked()
    {
        showMapLayout();
        presenter.stopLocationCheck();
        currentLocation.setText("- - - -");
        presenter.startLocationCheck(); // get location
    }

    @OnClick(R.id.searchButton)
    public void searchButtonClicked()
    {
        presenter.showSearchResultsScreen();
    }

    @OnClick(R.id.emptyRetryButton)
    public void emptyButtonClicked()
    {
        presenter.resume();
    }

    private Address address;
    private PredictAdapter predictAdapter;

    
    public static SearchFragment newInstance(int sectionNumber)
    {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public SearchFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey(EXTRA_ADDRESS))
                address = savedInstanceState.getParcelable(EXTRA_ADDRESS);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if(address != null)
            outState.putParcelable(EXTRA_ADDRESS, address);
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
        View fragmentView = inflater.inflate(R.layout.view_search, container, false);

        ButterKnife.inject(this, fragmentView);
        
        return fragmentView;
    }

    @Override
    public void onViewCreated(View fragmentView, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(fragmentView, savedInstanceState);

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                switch (position) {
                    case 0:
                        presenter.setTradeType(locationSpinner.getSelectedItemPosition() == 0? TradeType.LOCAL_BUY:TradeType.ONLINE_BUY);
                        break;
                    case 1:
                        presenter.setTradeType(locationSpinner.getSelectedItemPosition() == 0? TradeType.LOCAL_SELL:TradeType.ONLINE_SELL);
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0){
            }
        });

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                switch (position) {
                    case 0:
                        presenter.setTradeType(typeSpinner.getSelectedItemPosition() == 0? TradeType.LOCAL_BUY:TradeType.LOCAL_SELL);
                        break;
                    case 1:
                        presenter.setTradeType(typeSpinner.getSelectedItemPosition() == 0? TradeType.ONLINE_BUY:TradeType.ONLINE_SELL);
                        break;
                }

                paymentMethodLayout.setVisibility(position == 0?View.GONE:View.VISIBLE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0){
            }
        });

        paymentMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Method method =  (Method) paymentMethodSpinner.getAdapter().getItem(position);
                presenter.setPaymentMethod(method);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0){
            }
        });

        String[] locationTitles = getResources().getStringArray(R.array.list_location_spinner);
        List<String> locationList = new ArrayList<String>(Arrays.asList(locationTitles));

        SpinnerAdapter locationAdapter = new SpinnerAdapter(getContext(), R.layout.spinner_layout, locationList);
        locationSpinner.setAdapter(locationAdapter);

        String[] typeTitles = getResources().getStringArray(R.array.list_types_spinner);
        List<String> typeList = new ArrayList<String>(Arrays.asList(typeTitles));

        SpinnerAdapter typeAdapter = new SpinnerAdapter(getContext(), R.layout.spinner_layout, typeList);
        typeSpinner.setAdapter(typeAdapter);

        editLocation.setOnItemClickListener((parent, view, position, id) -> {
            Address address = predictAdapter.getItem(position);
            showMapLayout();
            editLocation.setText("");

            presenter.stopLocationCheck();
            presenter.setAddress(address);
            setAddress(address);
        });

        editLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3){}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (!Strings.isBlank(charSequence)) {
                    presenter.doAddressLookup(charSequence.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable){}
        });

        predictAdapter = new PredictAdapter(getContext(), Collections.emptyList());
        setEditLocationAdapter(predictAdapter);
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
        
        presenter.resume();
    }

    @Override 
    public void onPause() 
    {
        super.onPause();
        
        presenter.pause();
    }
    
    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        
        ButterKnife.reset(this);
    }
    
    @Override
    public void showError(String message)
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        emptyTextView.setText(message);
    }

    @Override
    public void showProgress()
    {
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress()
    {
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
    }

    @Override
    public PredictAdapter getEditLocationAdapter()
    {
        return predictAdapter;
    }

    @Override
    public void setMethods(List<Method> methods)
    {
        MethodAdapter typeAdapter = new MethodAdapter(getContext(), R.layout.spinner_layout, methods);
        paymentMethodSpinner.setAdapter(typeAdapter);
    }

    @Override
    public void setAddress(Address address)
    {
        this.address = address;
        
        if (address != null)
            currentLocation.setText(TradeUtils.getAddressShort(address));
    }

    protected void showSearchLayout()
    {
        mapLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));
        searchLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        mapLayout.setVisibility(View.GONE);
        searchLayout.setVisibility(View.VISIBLE);
    }

    protected void showMapLayout()
    {
        mapLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        searchLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));
        mapLayout.setVisibility(View.VISIBLE);
        searchLayout.setVisibility(View.GONE);
    }

    protected void setEditLocationAdapter(PredictAdapter adapter)
    {
        if (editLocation != null)
            editLocation.setAdapter(adapter);
    }
}
