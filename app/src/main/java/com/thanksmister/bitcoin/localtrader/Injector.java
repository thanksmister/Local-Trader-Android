package com.thanksmister.bitcoin.localtrader;

/**
 * Author: Michael Ritchie
 * Date: 3/26/15
 * Copyright 2013, ThanksMister LLC
 * 
 * https://github.com/rosshambrick/rain-or-shine/blob/master/app/src/main/java/com/rosshambrick/rainorshine/Injector.java
 */
import dagger.ObjectGraph;

public class Injector {
    
    private static ObjectGraph objectGraph;

    public static void init(BaseApplication application) {
        
        ApplicationModule module = new ApplicationModule(application);
        objectGraph = ObjectGraph.create(module);
    }

    public static <T> T inject(T object) {
        return objectGraph.inject(object);
    }

    public static void reset(BaseApplication application) 
    {
        init(application);
    }
}
