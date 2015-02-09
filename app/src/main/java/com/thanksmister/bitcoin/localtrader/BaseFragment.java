package com.thanksmister.bitcoin.localtrader;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.BaseActivity;

import java.util.List;

import dagger.ObjectGraph;

/** Base fragment which performs injection using the activity object graph of its parent. */
public abstract class BaseFragment extends Fragment 
{
    private ObjectGraph activityGraph;
    
    @Override 
    public void onActivityCreated(Bundle savedInstanceState) 
    {
        super.onActivityCreated(savedInstanceState);

        BaseApplication application = (BaseApplication) (getActivity()).getApplication();
        
        activityGraph = application.createScopedGraph(getModules().toArray());
        activityGraph.inject(this);
    }

    public void inject(Object object)
    {
        activityGraph.inject(object);
    }

    protected abstract List<Object> getModules();

    /**
     * Shows a {@link android.widget.Toast} message.
     *
     * @param message An string representing a message to be shown.
     */
    protected void showToastMessage(String message) 
    {
        if(getActivity() != null)
            ((BaseActivity)getActivity()).showToastMessage(message);
    }
}
