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

package com.thanksmister.bitcoin.localtrader.data.database;

import android.app.Application;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.SessionContract;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

public class DatabaseManager 
{
    private static DatabaseManager INSTANCE = null;
    
    public static String UPDATES = "updates";
    public static String ADDITIONS = "additions";
    public static String DELETIONS = "deletions";


    public static DatabaseManager getInstance()
    {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseManager();
        }

        return INSTANCE;
    }

    public String getAccessToken(Context context)
    {
        String token = "";
        final ContentResolver contentResolver = context.getContentResolver();
        Uri uri = SessionContract.Session.CONTENT_URI;
        Cursor c = contentResolver.query(uri, SessionContract.PROJECTION, null, null, null);
        assert c != null;
        while (c.moveToNext()) {
            token = c.getString(SessionContract.Session.COLUMN_INDEX_ACCESS_TOKEN);
            break;
        }
        c.close();
        return token;
    }

    public String getRefreshToken(Context context)
    {
        String token = "";
        final ContentResolver contentResolver = context.getContentResolver();
        Uri uri = SessionContract.Session.CONTENT_URI;
        Cursor c = contentResolver.query(uri, SessionContract.PROJECTION, null, null, null);
        assert c != null;
        while (c.moveToNext()) {
            token = c.getString(SessionContract.Session.COLUMN_INDEX_REFRESH_TOKEN);
            break;
        }
        
        c.close();
        return token;
    }

    private boolean insertTokens(Context context, String accessToken, String refreshToken)
    {
        final ContentResolver contentResolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        batch.add(ContentProviderOperation.newInsert(SessionContract.Session.CONTENT_URI)
                .withValue(SessionContract.Session.COLUMN_ACCESS_TOKEN, accessToken)
                .withValue(SessionContract.Session.COLUMN_REFRESH_TOKEN, refreshToken)
                .build());

        try {
            contentResolver.applyBatch(SessionContract.CONTENT_AUTHORITY, batch);
            contentResolver.notifyChange(SessionContract.Session.CONTENT_URI, null, false);
            return true;
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateTokens(Context context, String accessToken, String refreshToken)
    {
        boolean dropped = deleteTokens(context);
        return dropped && insertTokens(context, accessToken, refreshToken);
    }

    public boolean deleteTokens(Context context)
    {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase db = databaseHelper.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db.delete(SessionContract.Session.TABLE_NAME, null, null);
        return true;
    }

    public ArrayList<Contact> getContacts(Context context)
    {
        ArrayList<Contact> items = new ArrayList<Contact>();
        
        // Get list of all items
        Cursor c = getContactsCursor(context);

        while (c.moveToNext()) {
            Contact item = cursorToContact(c);
            items.add(item);
        }

        return items;
    }
    
    public Cursor getContactsCursor(Context context)
    {
        final ContentResolver contentResolver = context.getContentResolver();

        // Get list of all items
        return contentResolver.query(
                ContactContract.ContactData.CONTENT_URI, // URI
                ContactContract.ContactData.PROJECTION,                // Projection
                null,
                null,
                ContactContract.ContactData.COLUMN_NAME_CREATED_AT + " desc"); // Sort
    }

    public Contact getContactById(String contactId, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(ContactContract.ContactData.CONTENT_URI,
                ContactContract.ContactData.PROJECTION,
                ContactContract.ContactData.COLUMN_NAME_CONTACT_ID + " = ? ",
                new String[]{contactId},
                null);

        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            Contact item = cursorToContact(cursor);
            cursor.close();
            return  item;
        }

        return null;
    }

    public int deleteContactById(String contactId, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();

        return resolver.delete(ContactContract.ContactData.CONTENT_URI,
                ContactContract.ContactData.COLUMN_NAME_CONTACT_ID + " = ? ",
                new String[]{contactId});
    }

    public int updateContact(Contact contact, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_CREATED_AT, contact.created_at);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_PAYMENT_COMPLETED_AT, contact.payment_completed_at);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_AMOUNT_BTC, contact.amount_btc);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_AMOUNT, contact.amount);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_CURRENCY, contact.currency);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_REFERENCE_CODE, contact.reference_code);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_EXCHANGE_RATE_UPDATED_AT, contact.exchange_rate_updated_at);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_BUYER_USERNAME, contact.buyer.username);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_BUYER_TRADES, contact.buyer.trade_count);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_BUYER_FEEDBACK, contact.buyer.feedback_score);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_BUYER_NAME, contact.buyer.name);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_BUYER_LAST_SEEN, contact.buyer.last_online);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_SELLER_USERNAME, contact.seller.username);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_SELLER_TRADES, contact.seller.trade_count);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_SELLER_FEEDBACK, contact.seller.feedback_score);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_SELLER_NAME, contact.seller.name);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_SELLER_LAST_SEEN, contact.seller.last_online);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_RELEASE_URL, contact.actions.release_url);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_ADVERTISEMENT_URL, contact.actions.advertisement_public_view);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_MESSAGE_URL, contact.actions.message_url);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_MESSAGE_POST_URL, contact.actions.message_post_url);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_PAID_URL, contact.actions.mark_as_paid_url);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_DISPUTE_URL, contact.actions.dispute_url);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_FUND_URL, contact.actions.fund_url);
        
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_CANCEL_URL, contact.actions.cancel_url);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_ADVERTISEMENT_ID, contact.advertisement.id);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_IS_FUNDED, contact.is_funded?1:0);
        
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_TRADE_TYPE, contact.advertisement.trade_type.name());
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_PAYMENT_METHOD, contact.advertisement.payment_method);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_RECEIVER, contact.account_details.receiver_name);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_IBAN, contact.account_details.iban);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_SWIFT_BIC, contact.account_details.swift_bic);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_REFERENCE, contact.account_details.reference);
        
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_USERNAME, contact.advertisement.advertiser.username);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_TRADES, contact.advertisement.advertiser.trade_count);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_FEEDBACK, contact.advertisement.advertiser.feedback_score);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_NAME, contact.advertisement.advertiser.name);
        contentValues.put(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_LAST_SEEN, contact.advertisement.advertiser.last_online);

        return resolver.update(ContactContract.ContactData.CONTENT_URI,
                contentValues,
                ContactContract.ContactData.COLUMN_NAME_CONTACT_ID + " = ? ",
                new String[]{contact.contact_id});
    }

    public Contact cursorToContactShort(Cursor cursor)
    {
        Contact item = new Contact();
        item.advertisement.trade_type = (TradeType.valueOf(cursor.getString(ContactContract.ContactData.COLUMN_INDEX_TRADE_TYPE)));
        item.contact_id = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_CONTACT_ID));
        item.created_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_CREATED_AT));
        item.amount_btc = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_AMOUNT_BTC));
        item.amount = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_AMOUNT));
        item.currency = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_CURRENCY));
        return item;
    }

    public Contact cursorToContact(Cursor cursor)
    {
        Contact item = new Contact();

        //item.set_id(cursor.getString(ContactContract.ContactData.COLUMN_INDEX_ID));
       
        item.contact_id = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_CONTACT_ID));
        item.created_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_CREATED_AT));
        item.amount_btc = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_AMOUNT_BTC));
        item.amount = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_AMOUNT));
        item.currency = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_CURRENCY));
        
        item.disputed_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_DISPUTED_AT));
        item.closed_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_CLOSED_AT));
        item.exchange_rate_updated_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_EXCHANGE_RATE_UPDATED_AT));
        item.canceled_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_CANCELED_AT));
        item.released_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_RELEASED_AT));
        item.escrowed_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_ESCROWED_AT));
        item.funded_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_FUNDED_AT));
        item.payment_completed_at = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_PAYMENT_COMPLETED_AT));
        
        item.reference_code = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_REFERENCE_CODE));
  
        item.buyer.username = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_BUYER_USERNAME));
        item.buyer.trade_count = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_BUYER_TRADES));
        item.buyer.name = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_BUYER_NAME));
        item.buyer.feedback_score = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_BUYER_FEEDBACK));
        item.buyer.last_online = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_BUYER_LAST_SEEN));

        item.seller.username = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_SELLER_USERNAME));
        item.seller.trade_count = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_SELLER_TRADES));
        item.seller.name = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_SELLER_NAME));
        item.seller.feedback_score = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_SELLER_FEEDBACK));
        item.seller.last_online = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_SELLER_LAST_SEEN));

        item.advertisement.advertiser.username = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_ADVERTISER_USERNAME));
        item.advertisement.advertiser.trade_count = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_ADVERTISER_TRADES));
        item.advertisement.advertiser.name = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_ADVERTISER_NAME));
        item.advertisement.advertiser.feedback_score = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_ADVERTISER_FEEDBACK));
        item.advertisement.advertiser.last_online = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_ADVERTISER_LAST_SEEN));
        item.advertisement.trade_type = (TradeType.valueOf(cursor.getString(ContactContract.ContactData.COLUMN_INDEX_TRADE_TYPE)));
        item.advertisement.id = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_ADVERTISEMENT_ID));
        item.advertisement.payment_method = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_PAYMENT_METHOD));
       
        item.account_details.receiver_name = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_RECEIVER));
        item.account_details.iban = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_IBAN));
        item.account_details.swift_bic = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_SWIFT_BIC));
        item.account_details.reference = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_REFERENCE));

        item.actions.message_url = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_MESSAGE_URL));
        item.actions.message_post_url = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_MESSAGE_POST_URL));
        item.actions.dispute_url = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_DISPUTE_URL));
        item.actions.cancel_url = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_CANCEL_URL));
        item.actions.release_url = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_RELEASE_URL));
        item.actions.mark_as_paid_url = (cursor.getString(ContactContract.ContactData.COLUMN_INDEX_PAID_URL));
        item.actions.advertisement_public_view = ( cursor.getString(ContactContract.ContactData.COLUMN_INDEX_ADVERTISEMENT_URL) );
        item.actions.fund_url = ( cursor.getString(ContactContract.ContactData.COLUMN_INDEX_FUND_URL) );
        
        item.is_funded = ( (cursor.getInt(ContactContract.ContactData.COLUMN_INDEX_IS_FUNDED) ==  1) );

        return item;
    }

    public TreeMap<String, ArrayList<Contact>> updateContacts(final List<Contact> items, final Context context)
    {
        Timber.d("update contact list");
        final ContentResolver contentResolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        TreeMap<String, ArrayList<Contact>> updateMap = new TreeMap<String, ArrayList<Contact>>();
        ArrayList<Contact> newContacts = new ArrayList<Contact>();
        ArrayList<Contact> deletedContacts = new ArrayList<Contact>();
        ArrayList<Contact> updatedContacts = new ArrayList<Contact>();
        
        HashMap<String, Contact> entryMap = new HashMap<String, Contact>();
        for (Contact item : items) {
            entryMap.put(item.contact_id, item);
        }

        // Get list of all items
        Uri uri = ContactContract.ContactData.CONTENT_URI; // Get all entries
        Cursor c = contentResolver.query(uri, ContactContract.ContactData.PROJECTION, null, null, null);
        assert c != null;

        // Find stale data
        int id;
        String contactId;
        String payment_complete_at;
        String closed_at;
        String disputed_at;
        String escrowed_at;
        String funded_at;
        String released_at;
        String canceled_at;
        
        String fundUrl;
        String buyerLastSeen;
        String sellerLastSeen;
        boolean isFunded;

        while (c.moveToNext()) {
            id = c.getInt(ContactContract.ContactData.COLUMN_INDEX_ID);
            contactId = c.getString(ContactContract.ContactData.COLUMN_INDEX_CONTACT_ID);
            
            payment_complete_at = c.getString(ContactContract.ContactData.COLUMN_INDEX_PAYMENT_COMPLETED_AT);
            closed_at = c.getString(ContactContract.ContactData.COLUMN_INDEX_CLOSED_AT);
            disputed_at = c.getString(ContactContract.ContactData.COLUMN_INDEX_DISPUTED_AT);
            escrowed_at = c.getString(ContactContract.ContactData.COLUMN_INDEX_ESCROWED_AT);
            funded_at = c.getString(ContactContract.ContactData.COLUMN_INDEX_FUNDED_AT);
            released_at = c.getString(ContactContract.ContactData.COLUMN_INDEX_RELEASED_AT);
            canceled_at = c.getString(ContactContract.ContactData.COLUMN_INDEX_CANCELED_AT);
            
            buyerLastSeen = c.getString(ContactContract.ContactData.COLUMN_INDEX_BUYER_LAST_SEEN);
            fundUrl = c.getString(ContactContract.ContactData.COLUMN_INDEX_FUND_URL);
            isFunded = (c.getInt(ContactContract.ContactData.COLUMN_INDEX_IS_FUNDED) == 1);
            sellerLastSeen = c.getString(ContactContract.ContactData.COLUMN_INDEX_SELLER_LAST_SEEN);

            Contact match = entryMap.get(contactId);

            if (match != null) {

                entryMap.remove(contactId);

                Uri existingUri = ContactContract.ContactData.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();
                if ((match.payment_completed_at != null && !match.payment_completed_at.equals(payment_complete_at))
                        |(match.closed_at != null && !match.closed_at.equals(closed_at))
                        |(match.disputed_at != null && !match.disputed_at.equals(disputed_at))
                        |(match.escrowed_at != null && !match.escrowed_at.equals(escrowed_at))
                        |(match.funded_at != null && !match.funded_at.equals(funded_at))
                        |(match.released_at != null && !match.released_at.equals(released_at))
                        |(match.canceled_at != null && !match.canceled_at.equals(canceled_at))
             
                        ||(match.buyer.last_online != null && !match.buyer.last_online.equals(buyerLastSeen))
                        ||(match.actions.fund_url != null && !match.actions.fund_url.equals(fundUrl))
                        ||(match.is_funded != isFunded)
                        ||(match.seller.last_online != null && !match.seller.last_online .equals(sellerLastSeen))) {

                    updatedContacts.add(match);

                    // Update existing record
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_PAYMENT_COMPLETED_AT, match.payment_completed_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_CLOSED_AT, match.closed_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_DISPUTED_AT, match.disputed_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_FUNDED_AT, match.funded_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_ESCROWED_AT, match.escrowed_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_RELEASED_AT, match.released_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_EXCHANGE_RATE_UPDATED_AT, match.exchange_rate_updated_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_CANCELED_AT, match.canceled_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_CLOSED_AT, match.closed_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_DISPUTED_AT, match.disputed_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_FUNDED_AT, match.funded_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_ESCROWED_AT, match.escrowed_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_RELEASED_AT, match.released_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_CANCELED_AT, match.canceled_at)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_BUYER_LAST_SEEN, match.buyer.last_online)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_SELLER_LAST_SEEN, match.seller.last_online)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_IS_FUNDED, match.is_funded?1:0)
                            .withValue(ContactContract.ContactData.COLUMN_NAME_FUND_URL, match.actions.fund_url)
                            .build());
                    
                    //updateMessages(match.contact_id, match.messages, context);
                } 
            } else {
                // Entry doesn't exist. Remove it from the database.
                deletedContacts.add(match);
                
                Uri deleteUri = ContactContract.ContactData.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                
                //deleteMessages(match.contact_id, context); // delete associated messages
            }
        }
        c.close();

        // Add new items
        for (Contact item : entryMap.values()) {

            newContacts.add(item);

            batch.add(ContentProviderOperation.newInsert(ContactContract.ContactData.CONTENT_URI)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_CONTACT_ID, item.contact_id)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_CREATED_AT, item.created_at)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_AMOUNT_BTC, item.amount_btc)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_AMOUNT, item.amount)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_CURRENCY, item.currency)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_REFERENCE_CODE, item.reference_code)

                    .withValue(ContactContract.ContactData.COLUMN_NAME_PAYMENT_COMPLETED_AT, item.payment_completed_at)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_CLOSED_AT, item.closed_at)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_DISPUTED_AT, item.disputed_at)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_FUNDED_AT, item.funded_at)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_ESCROWED_AT, item.escrowed_at)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_RELEASED_AT, item.released_at)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_EXCHANGE_RATE_UPDATED_AT, item.exchange_rate_updated_at)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_CANCELED_AT, item.canceled_at)

                            // buyer
                    .withValue(ContactContract.ContactData.COLUMN_NAME_BUYER_USERNAME, item.buyer.username)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_BUYER_TRADES, item.buyer.trade_count)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_BUYER_FEEDBACK, item.buyer.feedback_score)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_BUYER_NAME, item.buyer.name)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_BUYER_LAST_SEEN, item.buyer.last_online)

                            // seller
                    .withValue(ContactContract.ContactData.COLUMN_NAME_SELLER_USERNAME, item.seller.username)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_SELLER_TRADES, item.seller.trade_count)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_SELLER_FEEDBACK, item.seller.feedback_score)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_SELLER_NAME, item.seller.name)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_SELLER_LAST_SEEN, item.seller.last_online)

                            // actions
                    .withValue(ContactContract.ContactData.COLUMN_NAME_RELEASE_URL, item.actions.release_url)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_ADVERTISEMENT_URL, item.actions.advertisement_public_view)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_MESSAGE_URL, item.actions.message_url)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_MESSAGE_POST_URL, item.actions.message_post_url)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_PAID_URL, item.actions.mark_as_paid_url)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_FUND_URL, item.actions.fund_url)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_DISPUTE_URL, item.actions.dispute_url)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_CANCEL_URL, item.actions.cancel_url)

                            // account
                    .withValue(ContactContract.ContactData.COLUMN_NAME_RECEIVER, item.account_details.receiver_name)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_IBAN, item.account_details.iban)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_SWIFT_BIC, item.account_details.swift_bic)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_REFERENCE, item.account_details.reference)

                            // advertisement
                    .withValue(ContactContract.ContactData.COLUMN_NAME_ADVERTISEMENT_ID, item.advertisement.id)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_TRADE_TYPE, item.advertisement.trade_type.name())
                    .withValue(ContactContract.ContactData.COLUMN_NAME_PAYMENT_METHOD, item.advertisement.payment_method)

                            // advertiser
                    .withValue(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_USERNAME, item.advertisement.advertiser.username)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_TRADES, item.advertisement.advertiser.trade_count)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_FEEDBACK, item.advertisement.advertiser.feedback_score)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_NAME, item.advertisement.advertiser.name)
                    .withValue(ContactContract.ContactData.COLUMN_NAME_ADVERTISER_LAST_SEEN, item.advertisement.advertiser.last_online)
                    .build());
            
             //updateMessages(item.contact_id, item.messages, context);
        }
        
        // delete   
        try {
            contentResolver.applyBatch(ContactContract.CONTENT_AUTHORITY, batch);
            contentResolver.notifyChange(
                    ContactContract.ContactData.CONTENT_URI, // URI where data was modified
                    null,                           // No local observer
                    false);                         // IMPORTANT: Do not sync to network
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }

        updateMap.put(UPDATES, updatedContacts);
        updateMap.put(ADDITIONS, newContacts);
        updateMap.put(DELETIONS, deletedContacts);

        Timber.d("returning contacts");
        
        return updateMap;
    }

    public List<Message> getMessages(String contactId, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = resolver.query(
                MessagesContract.Message.CONTENT_URI, // URI
                MessagesContract.PROJECTION,                // Projection
                MessagesContract.Message.COLUMN_NAME_CONTACT_ID + " = ? ",
                new String[] {contactId},  // Select new and pending orders
                null); // Sort

        List<Message> messages = new ArrayList<Message>();
        while (c.moveToNext()) {
            Message message = cursorToMessage(c);
            messages.add(message);
        }

        return messages;
    }

    public int getMessageCount(String contactId, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = resolver.query(
                MessagesContract.Message.CONTENT_URI, // URI
                MessagesContract.PROJECTION,                // Projection
                MessagesContract.Message.COLUMN_NAME_CONTACT_ID + " = ? ",
                new String[] {contactId},  // Select new and pending orders
                null); // Sort

        int count = c.getCount();
        c.close();

        return count;
    }

    public int getNewMessageCount(String contactId, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = resolver.query(
                MessagesContract.Message.CONTENT_URI, // URI
                MessagesContract.PROJECTION,                // Projection
                "seen='" + 1 + "' AND contact_id='" + contactId + "'",
                null,  // Select new and pending orders
                null); // Sort

        int count = c.getCount();
        c.close();

        return count;
    }

    public boolean hasNewMessages(String contactId, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = resolver.query(
                MessagesContract.Message.CONTENT_URI, // URI
                MessagesContract.PROJECTION,                // Projection
                "seen='" + "'1" + "' AND contact_id='" + contactId + "'",
                null,
                null); // Sort
        
        int count = c.getCount();
        c.close();

        return count > 0;
    }

    public int deleteMessages(String contact_id, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();

        return resolver.delete(MessagesContract.Message.CONTENT_URI,
                MessagesContract.Message.COLUMN_NAME_CONTACT_ID + " = ? ",
                new String[]{contact_id});
    }

    public int updateMessageSeen(String id, Boolean seen, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessagesContract.Message.COLUMN_NAME_SEEN, seen?1:0);

        return resolver.update(MessagesContract.Message.CONTENT_URI,
                contentValues,
                MessagesContract.Message._ID + " = ? ",
                new String[]{id});
    }

    public ArrayList<Message> updateMessages(final String contactId, final List<Message> messages, Context context)
    {
        final ContentResolver contentResolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        ArrayList<Message> newMessages = new ArrayList<Message>();
        HashMap<String, Message> entryMap = new HashMap<String, Message>();
        for (Message item : messages) {
            entryMap.put(item.created_at, item);
        }

        Uri uri = MessagesContract.Message.CONTENT_URI; // Get all entries
        Cursor c = contentResolver.query(uri,
                MessagesContract.PROJECTION,
                MessagesContract.Message.COLUMN_NAME_CONTACT_ID + " = ?",
                new String[] {contactId},  // Select new orders
                null);

        // Find stale data
        int id;
        String createdAt;
        while (c.moveToNext()) {
            id = c.getInt(MessagesContract.Message.COLUMN_INDEX_ID);
            createdAt = c.getString(MessagesContract.Message.COLUMN_INDEX_CREATED_AT);
            Message match = entryMap.get(createdAt);
            if (match != null) {
                // Entry exists. Remove from entry map to prevent insert later. Do not update message
                entryMap.remove(createdAt);
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = MessagesContract.Message.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
            }
        }
        c.close();

        // Add new items
        for (Message item : entryMap.values()) {

            batch.add(ContentProviderOperation.newInsert(MessagesContract.Message.CONTENT_URI)
                    .withValue(MessagesContract.Message.COLUMN_NAME_CONTACT_ID, item.contact_id)
                    .withValue(MessagesContract.Message.COLUMN_NAME_MESSAGE, item.msg)
                    .withValue(MessagesContract.Message.COLUMN_NAME_SEEN, item.seen ? 1 : 0)
                    .withValue(MessagesContract.Message.COLUMN_NAME_CREATED_AT, item.created_at)
                    .withValue(MessagesContract.Message.COLUMN_NAME_SENDER_ID, item.sender.id)
                    .withValue(MessagesContract.Message.COLUMN_NAME_SENDER_NAME, item.sender.name)
                    .withValue(MessagesContract.Message.COLUMN_NAME_SENDER_USERNAME, item.sender.username)
                    .withValue(MessagesContract.Message.COLUMN_NAME_SENDER_TRADE_COUNT, item.sender.trade_count)
                    .withValue(MessagesContract.Message.COLUMN_NAME_SENDER_LAST_ONLINE, item.sender.last_seen_on)
                    .withValue(MessagesContract.Message.COLUMN_NAME_SEEN, item.seen ? 1 : 0)
                    .withValue(MessagesContract.Message.COLUMN_NAME_IS_ADMIN, item.is_admin ? 1 : 0)
                    .build());

      
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.thanksmister.localtrader", MODE_PRIVATE);
            StringPreference stringPreference = new StringPreference(sharedPreferences, DataService.PREFS_USER);
            
            // ignore messages by logged in user as being "new"
            if(!item.sender.username.toLowerCase().equals(stringPreference.get())) {
                newMessages.add(item);
            }
        }

        try {
            contentResolver.applyBatch(BaseContract.CONTENT_AUTHORITY, batch);
            contentResolver.notifyChange(MessagesContract.Message.CONTENT_URI, null, false);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }

        return newMessages;
    }

    public Message cursorToMessage(Cursor cursor)
    {
        Message item = new Message();

        //item.id(cursor.getString(MessagesContract.Message.COLUMN_INDEX_ID));
        item.contact_id = (cursor.getString(MessagesContract.Message.COLUMN_INDEX_CONTACT_ID));
        item.msg = (cursor.getString(MessagesContract.Message.COLUMN_INDEX_MESSAGE));
        item.created_at = (cursor.getString(MessagesContract.Message.COLUMN_INDEX_CREATED_AT));
        item.seen = (cursor.getInt(MessagesContract.Message.COLUMN_INDEX_SEEN) == 1);
        item.sender.id = (cursor.getString(MessagesContract.Message.COLUMN_INDEX_SENDER_ID));
        item.sender.name = (cursor.getString(MessagesContract.Message.COLUMN_INDEX_SENDER_NAME));
        item.sender.username = (cursor.getString(MessagesContract.Message.COLUMN_INDEX_SENDER_USERNAME));
        item.sender.trade_count = (cursor.getString(MessagesContract.Message.COLUMN_INDEX_SENDER_TRADE_COUNT));
        item.sender.last_seen_on = (cursor.getString(MessagesContract.Message.COLUMN_INDEX_SENDER_LAST_ONLINE));
        item.is_admin = ( (cursor.getInt(MessagesContract.Message.COLUMN_INDEX_IS_ADMIN) == 1));

        return item;
    }

    public boolean updateAdvertisement(Context context, Advertisement advertisement)
    {
        final ContentResolver contentResolver = context.getContentResolver();

        Cursor c = contentResolver.query(
                AdvertisementContract.Advertisement.CONTENT_URI, // URI
                AdvertisementContract.Advertisement.PROJECTION,                // Projection
                AdvertisementContract.Advertisement.COLUMN_NAME_AD_ID + "=?",                           // Selection
                new String[] { advertisement.ad_id },                           // Selection args
                null); // Sort


        while (c.moveToNext()) {
            // Check to see if the entry needs to be updated
            int id = c.getInt(AdvertisementContract.Advertisement.COLUMN_ID);
            Uri existingUri = AdvertisementContract.Advertisement.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();
            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
            batch.add(ContentProviderOperation.newUpdate(existingUri)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_MAX_AMOUNT, advertisement.max_amount)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_MIN_AMOUNT, advertisement.min_amount)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PRICE_EQUATION, advertisement.price_equation)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_VISIBLE, advertisement.visible)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_LOCATION, advertisement.location)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_CITY, advertisement.city)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_LAT, advertisement.lat)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_LON, advertisement.lon)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_COUNTRY_CODE, advertisement.country_code)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_ACCOUNT_INFO, advertisement.account_info)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_SMS_VERIFICATION_REQUIRED, advertisement.sms_verification_required)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_TRUSTED_REQUIRED, advertisement.trusted_required)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_TRACK_MAX_AMOUNT, advertisement.track_max_amount)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_BANK_NAME, advertisement.bank_name)
                    .build());
    
            try {
                contentResolver.applyBatch(AdvertisementContract.CONTENT_AUTHORITY, batch);
                contentResolver.notifyChange(
                        AdvertisementContract.Advertisement.CONTENT_URI, // URI where data was modified
                        null,                           // No local observer
                        false);
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();
            }
        }
        
        return true;
    }

    public Advertisement getAdvertisementById(Context context, String adId)
    {
        final ContentResolver contentResolver = context.getContentResolver();

        Cursor c = contentResolver.query(
                AdvertisementContract.Advertisement.CONTENT_URI, // URI
                AdvertisementContract.Advertisement.PROJECTION,                // Projection
                AdvertisementContract.Advertisement.COLUMN_NAME_AD_ID + "=?",                           // Selection
                new String[] { String.valueOf(adId) },                           // Selection args
                null); // Sort

        return cursorToAdvertisement(c);
    }
    
    public ArrayList<Advertisement> getAdvertisements(Context context)
    {
        ArrayList<Advertisement> items = new ArrayList<Advertisement>();
        Cursor c = getAdvertisementsCursor(context);
        while (c.moveToNext()) {
            Advertisement advertisement = cursorToAdvertisement(c);
            items.add(advertisement);
        }

        return items;
    }
    
    public Cursor getAdvertisementsCursor(Context context)
    {
        final ContentResolver contentResolver = context.getContentResolver();

        // Get list of all items
        return contentResolver.query(
                AdvertisementContract.Advertisement.CONTENT_URI, // URI
                AdvertisementContract.Advertisement.PROJECTION,                // Projection
                null,                           // Selection
                null,                           // Selection args
                AdvertisementContract.Advertisement.COLUMN_NAME_CREATED_AT + " asc"); // Sort
    }

    public Advertisement cursorToAdvertisement(Cursor cursor)
    {
        Advertisement advertisement = new Advertisement();
        advertisement.ad_id = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_AD_ID));
        advertisement.created_at =(cursor.getString(AdvertisementContract.Advertisement.COLUMN_CREATED_AT));
        advertisement.visible = ((cursor.getInt(AdvertisementContract.Advertisement.COLUMN_VISIBLE) == 1));
        advertisement.email = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_EMAIL));
        advertisement.location = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_LOCATION));
        advertisement.country_code = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_COUNTRY_CODE));
        advertisement.city = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_CITY));

        advertisement.trade_type = (TradeType.valueOf(cursor.getString(AdvertisementContract.Advertisement.COLUMN_TRADE_TYPE)));
        
        advertisement.online_provider = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_ONLINE_PROVIDER));
        advertisement.sms_verification_required = ((cursor.getInt(AdvertisementContract.Advertisement.COLUMN_SMS_VERIFICATION_REQUIRED) == 1));
        advertisement.price_equation = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_PRICE_EQUATION));
        advertisement.currency = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_CURRENCY));
        advertisement.account_info = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_ACCOUNT_INFO));
        advertisement.lat = (cursor.getFloat(AdvertisementContract.Advertisement.COLUMN_LAT));
        advertisement.lon = (cursor.getFloat(AdvertisementContract.Advertisement.COLUMN_LON));
        advertisement.min_amount = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_MIN_AMOUNT));
        advertisement.max_amount = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_MAX_AMOUNT));
        advertisement.actions.public_view = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_PUBLIC_VIEW));
        advertisement.price = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_PRICE));
        advertisement.profile.last_online = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_PROFILE_ID));
        advertisement.profile.name =(cursor.getString(AdvertisementContract.Advertisement.COLUMN_PROFILE_NAME));
        advertisement.profile.username =(cursor.getString(AdvertisementContract.Advertisement.COLUMN_PROFILE_USERNAME));
        advertisement.bank_name = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_BANK_NAME));
        advertisement.message = (cursor.getString(AdvertisementContract.Advertisement.COLUMN_MESSAGE));
        advertisement.trusted_required = ((cursor.getInt(AdvertisementContract.Advertisement.COLUMN_TRUSTED_REQUIRED) == 1));
        advertisement.track_max_amount = ((cursor.getInt(AdvertisementContract.Advertisement.COLUMN_TRACK_MAX_AMOUNT) == 1));

        return advertisement;
    }

    public boolean hasAdvertisements(Context context)
    {
        final ContentResolver contentResolver = context.getContentResolver();
        Cursor c = contentResolver.query(
                AdvertisementContract.Advertisement.CONTENT_URI, // URI
                AdvertisementContract.Advertisement.PROJECTION,                // Projection
                null,
                null,
                null); // Sort

        return (c.getCount() > 0);
    }

    public void updateAdvertisements(final List<Advertisement> advertisements, Context context)
    {
        final ContentResolver contentResolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        // Build hash table of incoming entries
        HashMap<String, Advertisement> entryMap = new HashMap<String, Advertisement>();
        for (Advertisement item : advertisements) {
            //Log.e(CLASS_NAME + "Advertisement UPDATE: " + item.toString());
            entryMap.put(item.ad_id, item);
        }

        // Get list of all items
        Uri uri = AdvertisementContract.Advertisement.CONTENT_URI; // Get all entries
        Cursor c = contentResolver.query(uri, AdvertisementContract.Advertisement.PROJECTION, null, null, null);
        assert c != null;
    
        // Find stale data
        int id;
        String adId;
        String created_at;
        int visible;
        String location;
        String country_code;
        String city;
        String trade_type;
        String online_provider;

        boolean sms_verification_required;
        String price_equation;
        String currency;
        String account_info;
        double lat;
        double lon;
        String min_amount;
        String max_amount;
        String price;
        String bank_name;
        String ad_message;
        int trustRequired;
        int trackMaxAmount;

        while (c.moveToNext()) {

            id = c.getInt(AdvertisementContract.Advertisement.COLUMN_ID);
            adId = c.getString(AdvertisementContract.Advertisement.COLUMN_AD_ID);
            created_at = c.getString(AdvertisementContract.Advertisement.COLUMN_CREATED_AT);
            visible = c.getInt(AdvertisementContract.Advertisement.COLUMN_VISIBLE);
            location = c.getString(AdvertisementContract.Advertisement.COLUMN_LOCATION);
            country_code = c.getString(AdvertisementContract.Advertisement.COLUMN_COUNTRY_CODE);
            city = c.getString(AdvertisementContract.Advertisement.COLUMN_CITY);
            trade_type = c.getString(AdvertisementContract.Advertisement.COLUMN_TRADE_TYPE);
            online_provider = c.getString(AdvertisementContract.Advertisement.COLUMN_ONLINE_PROVIDER);
    
            sms_verification_required = (c.getInt(AdvertisementContract.Advertisement.COLUMN_SMS_VERIFICATION_REQUIRED) == 1);
            price_equation = c.getString(AdvertisementContract.Advertisement.COLUMN_PRICE_EQUATION);

            currency = c.getString(AdvertisementContract.Advertisement.COLUMN_CURRENCY);
            account_info = c.getString(AdvertisementContract.Advertisement.COLUMN_ACCOUNT_INFO);
            lat = c.getDouble(AdvertisementContract.Advertisement.COLUMN_LAT);
            lon = c.getDouble(AdvertisementContract.Advertisement.COLUMN_LON);
            min_amount = c.getString(AdvertisementContract.Advertisement.COLUMN_MIN_AMOUNT);
            max_amount = c.getString(AdvertisementContract.Advertisement.COLUMN_MAX_AMOUNT);
            price = c.getString(AdvertisementContract.Advertisement.COLUMN_PRICE);

            bank_name = c.getString(AdvertisementContract.Advertisement.COLUMN_BANK_NAME);
            ad_message = c.getString(AdvertisementContract.Advertisement.COLUMN_MESSAGE);
            trustRequired = c.getInt(AdvertisementContract.Advertisement.COLUMN_TRUSTED_REQUIRED);
            trackMaxAmount = c.getInt(AdvertisementContract.Advertisement.COLUMN_TRACK_MAX_AMOUNT);

            Advertisement match = entryMap.get(adId);

            if (match != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                entryMap.remove(adId);

                // Check to see if the entry needs to be updated
                Uri existingUri = AdvertisementContract.Advertisement.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();

                if ((match.created_at != null && !match.created_at.equals(created_at)) ||
                        (match.price_equation != null && !match.price_equation.equals(price_equation)) ||
                        (match.price != null && !match.price.equals(price)) ||
                        (match.city != null && !match.city.equals(city)) ||
                        (match.country_code != null && !match.country_code.equals(country_code)) ||
                        (match.bank_name != null && !match.bank_name.equals(bank_name)) ||
                        (match.currency != null && !match.currency.equals(currency)) ||
                        (match.lat != lat) || (match.lon != lon) ||
                        (match.location != null && !match.location.equals(location)) ||
                        (match.max_amount != null && !match.max_amount.equals(max_amount)) ||
                        (match.min_amount != null && !match.min_amount.equals(min_amount)) ||
                        (match.online_provider != null && !match.online_provider.equals(online_provider)) ||
                        (match.account_info != null && !match.account_info.equals(account_info)) ||
                        (match.trade_type.name() != null && !match.trade_type.name().equals(trade_type)) ||
                        (match.sms_verification_required != sms_verification_required) ||
                        (match.visible != (visible == 1) ||
                        (match.trusted_required != (trustRequired == 1)) ||
                        (match.track_max_amount != (trackMaxAmount == 1)) ||
                        (match.message != null && !match.message.equals(ad_message))))   {

                    // Update existing record
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_CREATED_AT, match.created_at)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_CITY, match.city)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_COUNTRY_CODE, match.country_code)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_CURRENCY, match.currency)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_LAT, match.lat)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_LON, match.lon)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_LOCATION, match.location)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_MAX_AMOUNT, match.max_amount)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_MIN_AMOUNT, match.min_amount)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_ONLINE_PROVIDER, match.online_provider)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PRICE, match.price)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PRICE_EQUATION, match.price_equation)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PUBLIC_VIEW, match.actions.public_view)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_TRADE_TYPE, match.trade_type.name())
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_VISIBLE, match.visible? 1:0)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_ACCOUNT_INFO, match.account_info)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PROFILE_ID, match.profile.last_online)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PROFILE_NAME, match.profile.name)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PROFILE_USERNAME, match.profile.username)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_BANK_NAME, match.bank_name)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_TRUSTED_REQUIRED, match.trusted_required?1:0)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_MESSAGE, match.message)
                            .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_TRACK_MAX_AMOUNT, match.track_max_amount?1:0)
                            .build());
                } else {
                    // do nothing
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = AdvertisementContract.Advertisement.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
            }
        }
        c.close();

        // Add new items
        for (Advertisement item : entryMap.values()) {

            batch.add(ContentProviderOperation.newInsert(AdvertisementContract.Advertisement.CONTENT_URI)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_AD_ID, item.ad_id)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_CREATED_AT, item.created_at)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_CITY, item.city)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_COUNTRY_CODE, item.country_code)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_CURRENCY, item.currency)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_EMAIL, item.email)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_LAT, item.lat)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_LON, item.lon)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_LOCATION, item.location)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_MAX_AMOUNT, item.max_amount)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_MIN_AMOUNT, item.min_amount)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_ONLINE_PROVIDER, item.online_provider)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PRICE, item.price)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PRICE_EQUATION, item.price_equation)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PUBLIC_VIEW, item.actions.public_view)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_SMS_VERIFICATION_REQUIRED, item.sms_verification_required ? 1 : 0)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_TRADE_TYPE, item.trade_type.name())
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_VISIBLE, item.visible ? 1 : 0)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_ACCOUNT_INFO, item.account_info)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PROFILE_ID, item.profile.last_online)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PROFILE_NAME, item.profile.name)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_PROFILE_USERNAME, item.profile.username)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_BANK_NAME, item.bank_name)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_MESSAGE, item.message)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_TRUSTED_REQUIRED, item.trusted_required ? 1 : 0)
                    .withValue(AdvertisementContract.Advertisement.COLUMN_NAME_TRACK_MAX_AMOUNT, item.track_max_amount ? 1 : 0)
                    .build());
        }

        try {
            contentResolver.applyBatch(AdvertisementContract.CONTENT_AUTHORITY, batch);
            contentResolver.notifyChange(
                    AdvertisementContract.Advertisement.CONTENT_URI, // URI where data was modified
                    null,                           // No local observer
                    false);                         // IMPORTANT: Do not sync to network
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    public Wallet getWallet(Context context)
    {
        final ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(WalletContract.Wallet.CONTENT_URI,
                WalletContract.PROJECTION,
                null,
                null,
                null);

        try {
            if (cursor.moveToFirst()) {
                return cursorToWallet(cursor);
            }
            return null;
        } catch (Exception e) {
            Timber.e(e.getMessage());
        } finally {
            cursor.close();
        }
        
        return null;
    }

    public boolean deleteWallet( Context context)
    {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase db = databaseHelper.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db.delete(WalletContract.Wallet.TABLE_NAME, null, null);
        return true;
    }

    public boolean updateWallet(String _id, Wallet wallet, Context context)
    {
        final ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WalletContract.Wallet.COLUMN_WALLET_ADDRESS, wallet.address.address);
        contentValues.put(WalletContract.Wallet.COLUMN_WALLET_ADDRESS_RECEIVABLE, wallet.address.received);
        contentValues.put(WalletContract.Wallet.COLUMN_WALLET_BALANCE, wallet.total.balance);
        contentValues.put(WalletContract.Wallet.COLUMN_WALLET_SENDABLE, wallet.total.sendable);
        contentValues.put(WalletContract.Wallet.COLUMN_WALLET_MESSAGE, wallet.message);

        if(wallet.qrImage != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
            contentValues.put(WalletContract.Wallet.COLUMN_WALLET_QRCODE, baos.toByteArray());
        }

        int noUpdate = resolver.update(WalletContract.Wallet.CONTENT_URI,
                contentValues,
                WalletContract.Wallet._ID + " = ? ",
                new String[]{_id});

        return noUpdate > 0;
    }

    public boolean updateWalletQrCode(String _id, Bitmap qrcode, Context context)
    {
        Timber.d("Insert Wallet");
        
        final ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qrcode.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] byteArray = baos.toByteArray();

        if(byteArray != null) { // FIXME the bytes can't be null but sometimes it appears as though they are
            contentValues.put(WalletContract.Wallet.COLUMN_WALLET_QRCODE, byteArray);
            int noUpdate = resolver.update(WalletContract.Wallet.CONTENT_URI,
                    contentValues,
                    WalletContract.Wallet._ID + " = ? ",
                    new String[]{_id});

            return noUpdate > 0;
        }
        return false;
    }

    public boolean insertWallet(Wallet wallet, Context context)
    {
        Timber.d("Insert Wallet");

        final ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WalletContract.Wallet.COLUMN_WALLET_ADDRESS, wallet.address.address);
        contentValues.put(WalletContract.Wallet.COLUMN_WALLET_ADDRESS_RECEIVABLE, wallet.address.received);
        contentValues.put(WalletContract.Wallet.COLUMN_WALLET_BALANCE, wallet.total.balance);
        contentValues.put(WalletContract.Wallet.COLUMN_WALLET_SENDABLE, wallet.total.sendable);

        if(wallet.qrImage != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
            contentValues.put(WalletContract.Wallet.COLUMN_WALLET_QRCODE, baos.toByteArray());
        }
        
        resolver.insert(WalletContract.Wallet.CONTENT_URI, contentValues);
        
        return true;
    }

    public Wallet cursorToWallet(Cursor cursor)
    {
        Timber.d("cursorToWallet");
        
        Wallet item = new Wallet();

        item.id = (cursor.getString(WalletContract.Wallet.COLUMN_INDEX_ID));
        item.message = (cursor.getString(WalletContract.Wallet.COLUMN_INDEX_WALLET_MESSAGE));
        item.total.balance = (cursor.getString(WalletContract.Wallet.COLUMN_INDEX_WALLET_BALANCE));
        item.total.sendable = (cursor.getString(WalletContract.Wallet.COLUMN_INDEX_WALLET_SENDABLE));
        item.address.address = cursor.getString(WalletContract.Wallet.COLUMN_INDEX_WALLET_ADDRESS);
        item.address.received = (cursor.getString(WalletContract.Wallet.COLUMN_INDEX_WALLET_ADDRESS_RECEIVABLE));

        try {
            byte[] bb = cursor.getBlob(WalletContract.Wallet.COLUMN_INDEX_WALLET_QRCODE);
            item.qrImage = (BitmapFactory.decodeByteArray(bb, 0, bb.length));  
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }

        return item;
    }

    public void updateExchange(final Exchange exchanges, Context context)
    {
        final ContentResolver contentResolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        // Build hash table of incoming entries
        HashMap<String, Exchange> entryMap = new HashMap<String, Exchange>();
        entryMap.put(exchanges.name, exchanges);

        // Get exchange, should only be one
        Uri uri = ExchangeContract.Exchange.CONTENT_URI; // Get all entries
        Cursor c = contentResolver.query(uri, ExchangeContract.Exchange.PROJECTION, null, null, null);
        assert c != null;

        // Find stale data
        int id;
        String name;
        String ask;
        String bid;
        String last;

        while (c.moveToNext()) {

            id = c.getInt(ExchangeContract.Exchange.COLUMN_ID);
            name = c.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_EXCHANGE);
            ask = c.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_ASK);
            bid = c.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_BID);
            last = c.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_LAST);

            Exchange match = entryMap.get(name);

            if (match != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                entryMap.remove(name);

                // Check to see if the entry needs to be updated
                Uri existingUri = ExchangeContract.Exchange.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();

                if ((match.ask != null && !match.ask.equals(ask)) ||
                        (match.bid != null && !match.bid.equals(bid)) ||
                        (match.last != null && !match.last.equals(last))) {

                    // Update existing record
                    // Log.i(CLASS_NAME +  "Scheduling update: " + existingUri);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(ExchangeContract.Exchange.COLUMN_NAME_ASK, match.ask)
                            .withValue(ExchangeContract.Exchange.COLUMN_NAME_BID, match.bid)
                            .withValue(ExchangeContract.Exchange.COLUMN_NAME_LAST, match.last)
                            .withValue(ExchangeContract.Exchange.COLUMN_NAME_TIME_STAMP, match.timestamp)
                            .withValue(ExchangeContract.Exchange.COLUMN_NAME_VOLUME, match.volume)
                            .withValue(ExchangeContract.Exchange.COLUMN_NAME_HIGH, match.high)
                            .withValue(ExchangeContract.Exchange.COLUMN_NAME_LOW, match.low)
                            .withValue(ExchangeContract.Exchange.COLUMN_NAME_MID, match.mid)
                            .build());
                } 
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = ExchangeContract.Exchange.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build();
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
            }
        }
        c.close();

        // Add new items
        for (Exchange item : entryMap.values()) {
            batch.add(ContentProviderOperation.newInsert(ExchangeContract.Exchange.CONTENT_URI)
                    .withValue(ExchangeContract.Exchange.COLUMN_NAME_EXCHANGE, item.name)
                    .withValue(ExchangeContract.Exchange.COLUMN_NAME_ASK, item.ask)
                    .withValue(ExchangeContract.Exchange.COLUMN_NAME_BID, item.bid)
                    .withValue(ExchangeContract.Exchange.COLUMN_NAME_LAST, item.last)
                    .withValue(ExchangeContract.Exchange.COLUMN_NAME_TIME_STAMP, item.timestamp)
                    .withValue(ExchangeContract.Exchange.COLUMN_NAME_VOLUME, item.volume)
                    .withValue(ExchangeContract.Exchange.COLUMN_NAME_HIGH, item.high)
                    .withValue(ExchangeContract.Exchange.COLUMN_NAME_LOW, item.low)
                    .withValue(ExchangeContract.Exchange.COLUMN_NAME_MID, item.mid)
                    .build());
        }

        try {
            contentResolver.applyBatch(ExchangeContract.CONTENT_AUTHORITY, batch);
            contentResolver.notifyChange(
                    ExchangeContract.Exchange.CONTENT_URI, // URI where data was modified
                    null,                           // No local observer
                    false);                         // IMPORTANT: Do not sync to network

        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    public Exchange getExchange(Context context)
    {
        Cursor cursor = getCursorExchange(context);

        try {
            if (cursor.moveToFirst()) {
                return cursorToExchange(cursor);
            }
            return null;
        } catch (Exception e) {
            Timber.e(e.getMessage());
        } finally {
            cursor.close();
        }

        return null;
    }
    
    public Cursor getCursorExchange(Context context)
    {
        final ContentResolver resolver = context.getContentResolver();
        return resolver.query(ExchangeContract.Exchange.CONTENT_URI,
                ExchangeContract.Exchange.PROJECTION,
                null,
                null,
                null);

    }

    public Exchange cursorToExchange(Cursor cursor)
    {
        Exchange item = new Exchange();

        item.name = (cursor.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_EXCHANGE));
        item.bid = (cursor.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_BID));
        item.last = (cursor.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_LAST));
        item.ask = (cursor.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_ASK));
        item.high = (cursor.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_HIGH));
        item.low = (cursor.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_LOW));
        item.mid = (cursor.getString(ExchangeContract.Exchange.COLUMN_INDEX_MID));
        item.volume = (cursor.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_VOLUME));
        item.timestamp = (cursor.getString(ExchangeContract.Exchange.COLUMN_INDEX_NAME_TIME_STAMP));
        
        return item;
    }
    
    public void deleteDatabase(Context context)
    {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        databaseHelper.removeAll();
    }
}
