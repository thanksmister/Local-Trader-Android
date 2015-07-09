/*
 * Copyright 2007 ZXing authors
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

package com.thanksmister.bitcoin.localtrader.data.database;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.lang.ref.WeakReference;

/*http://stackoverflow.com/questions/11961857/implementing-asyncqueryhandler*/
public class ContentResolverAsyncHandler extends AsyncQueryHandler
{
    private WeakReference<AsyncQueryListener> mListener;

    public interface AsyncQueryListener
    {
        void onQueryComplete();
    }

    public ContentResolverAsyncHandler(ContentResolver cr, AsyncQueryListener listener)
    {
        super(cr);
        mListener = new WeakReference<AsyncQueryListener>(listener);
    }

    public ContentResolverAsyncHandler(ContentResolver cr)
    {
        super(cr);
    }

    /**
     * Assign the given {@link AsyncQueryListener} to receive query events from
     * asynchronous calls. Will replace any existing listener.
     */
    public void setQueryListener(AsyncQueryListener listener)
    {
        mListener = new WeakReference<AsyncQueryListener>(listener);
    }

    /**
     * {@inheritDoc}
     */
    protected void onQueryComplete()
    {
        final AsyncQueryListener listener = mListener.get();
        if (listener != null) {
            listener.onQueryComplete();
        } 
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri)
    {
        super.onInsertComplete(token, cookie, uri);
        onQueryComplete();
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result)
    {
        super.onUpdateComplete(token, cookie, result);
        onQueryComplete();
    }

    @Override
    protected void onDeleteComplete(int token, Object cookie, int result)
    {
        super.onDeleteComplete(token, cookie, result);
        onQueryComplete();
    }
}