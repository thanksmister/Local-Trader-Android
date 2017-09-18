/*
 * Copyright (c) 2016. DusApp
 */

package com.thanksmister.bitcoin.localtrader;

/**
 * Author: Michael Ritchie
 * Updated: 12/20/15
 */

import java.lang.annotation.Retention;

import javax.inject.Qualifier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Retention(RUNTIME)
public @interface ForApplication
{

}
