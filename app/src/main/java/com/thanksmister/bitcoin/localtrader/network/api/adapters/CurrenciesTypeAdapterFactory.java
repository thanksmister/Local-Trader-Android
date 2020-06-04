/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.network.api.adapters;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Author: Michael Ritchie
 * Updated: 12/19/15
 * http://stackoverflow.com/questions/28601418/gson-deserializer-with-retrofit-converter-just-need-inner-json-for-all-response
 */
public class CurrenciesTypeAdapterFactory implements TypeAdapterFactory
{
    @Override
    public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type)
    {
        final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

        return new TypeAdapter<T>()
        {
            @Override
            public void write(JsonWriter out, T value) throws IOException
            {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException
            {
                JsonElement jsonElement = elementAdapter.read(in);
                JsonObject dataObject = new JsonObject();
                JsonObject currencyObject = new JsonObject();
                     
                if (jsonElement.isJsonObject()) {
                    
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    
                    if (jsonObject.has("data") && jsonObject.get("data").isJsonObject()) {

                        dataObject = jsonObject.getAsJsonObject("data");

                        if (dataObject.has("currencies") && dataObject.get("currencies").isJsonObject()) {
                            currencyObject = dataObject.getAsJsonObject("currencies");
                        }

                        jsonElement = currencyObject;
                    }
                }
                
                return delegate.fromJsonTree(jsonElement);
            }
        }.nullSafe();
    }
}
