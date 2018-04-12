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
package com.thanksmister.bitcoin.localtrader.data.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;

import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact;

import java.util.ArrayList;
import java.util.List;

import auto.parcel.AutoParcel;
import rx.functions.Func1;
import timber.log.Timber;

@AutoParcel
public abstract class ContactItem {
    public static final String TABLE = "contact_item";

    public static final String _ID = "_id";

    public static final String CONTACT_ID = "contact_id";
    public static final String ADVERTISEMENT_TRADE_TYPE = "trade_type";
    public static final String CREATED_AT = "created_at";

    public static final String REFERENCE_CODE = "reference_code";
    public static final String CURRENCY = "currency";
    public static final String AMOUNT = "amount";
    public static final String AMOUNT_BTC = "amount_btc";

    public static final String PAYMENT_COMPLETED_AT = "payment_completed_at";
    public static final String CLOSED_AT = "closed_at";
    public static final String DISPUTED_AT = "disputed_at";
    public static final String FUNDED_AT = "funded_at";
    public static final String ESCROWED_AT = "escrowed_at";
    public static final String RELEASED_AT = "released_at";
    public static final String EXCHANGE_RATE_UPDATED_AT = "exchange_rate_updated_at";
    public static final String CANCELED_AT = "canceled_at";

    // BUYER
    public static final String BUYER_USERNAME = "buyer_username";
    public static final String BUYER_TRADES = "buyer_trade_count";
    public static final String BUYER_FEEDBACK = "buyer_feedback_score";
    public static final String BUYER_NAME = "buyer_name";
    public static final String BUYER_LAST_SEEN = "buyer_last_seen_on";

    // SELLER
    public static final String SELLER_USERNAME = "seller_username";
    public static final String SELLER_TRADES = "seller_trade_count";
    public static final String SELLER_FEEDBACK = "seller_feedback_score";
    public static final String SELLER_NAME = "seller_name";
    public static final String SELLER_LAST_SEEN = "seller_last_seen_on";

    // details
    public static final String IBAN = "iban";
    public static final String SWIFT_BIC = "swift_bic";
    public static final String REFERENCE = "reference";
    public static final String RECEIVER_EMAIL = "receiver_email";

    // TODO add these to database update
    public static final String RECEIVER_NAME = "receiver_name";
    public static final String ETHEREUM_ADDRESS = "ethereum_address";
    public static final String PHONE_NUMBER = "phone_number";
    public static final String BILLER_CODE = "biller_code";
    public static final String ACCOUNT_NUMBER = "account_number";
    public static final String SORT_CODE = "sort_code";
    public static final String MESSAGE = "message";
    public static final String BSB = "bsb";

    // editAdvertisement 3
    public static final String ADVERTISEMENT_ID = "ad_id";
    public static final String ADVERTISEMENT_PAYMENT_METHOD = "payment_method";

    // ADVERTISER
    public static final String ADVERTISER_USERNAME = "advertiser_username";
    public static final String ADVERTISER_TRADES = "advertiser_trade_count";
    public static final String ADVERTISER_FEEDBACK = "advertiser_feedback_score";
    public static final String ADVERTISER_NAME = "advertiser_name";
    public static final String ADVERTISER_LAST_SEEN = "advertiser_last_seen_on";

    // actions
    public static final String RELEASE_URL = "release_url";
    public static final String MESSAGE_URL = "messages_url";
    public static final String MESSAGE_POST_URL = "message_post_url";
    public static final String MARK_PAID_URL = "mark_as_paid_url";
    public static final String ADVERTISEMENT_URL = "ad_url";
    public static final String DISPUTE_URL = "dispute_url";
    public static final String CANCEL_URL = "cancel_url";
    public static final String FUND_URL = "fund_url";
    public static final String IS_FUNDED = "is_funded";
    public static final String IS_SELLING = "is_selling";
    public static final String IS_BUYING = "is_buying";

    public static final String MESSAGE_COUNT = "message_count";
    public static final String UNSEEN_MESSAGES = "unseen_messages";


    public static final String QUERY = "SELECT *"
            + " FROM "
            + ContactItem.TABLE
            + " WHERE "
            + ContactItem.CONTACT_ID
            + " = ?";

    public abstract long id();

    public abstract String contact_id();

    public abstract String reference_code();

    public abstract String currency();

    public abstract String amount();

    public abstract String amount_btc();

    public abstract boolean is_funded(); //contacts with escrow enabled and funded

    public abstract boolean is_selling(); // you are selling

    public abstract boolean is_buying(); // you are buying

    public abstract String created_at(); // TODO should never be null

    @Nullable
    public abstract String closed_at();

    @Nullable
    public abstract String disputed_at();

    @Nullable
    public abstract String funded_at();

    @Nullable
    public abstract String escrowed_at();

    @Nullable
    public abstract String canceled_at();

    @Nullable
    public abstract String released_at();

    @Nullable
    public abstract String payment_completed_at();

    @Nullable
    public abstract String exchange_rate_updated_at();

    public abstract String seller_username();

    public abstract String seller_feedback_score();

    public abstract String seller_trade_count();

    public abstract String seller_last_online();

    public abstract String seller_name();

    public abstract String buyer_username();

    public abstract String buyer_feedback_score();

    public abstract String buyer_trade_count();

    public abstract String buyer_last_online();

    public abstract String buyer_name();

    @Nullable
    public abstract String release_url(); // ONLINE_SELL escrows only

    public abstract String advertisement_public_view();

    public abstract String message_url();

    public abstract String message_post_url();

    @Nullable
    public abstract String mark_as_paid_url(); // ONLINE_BUY

    @Nullable
    public abstract String dispute_url(); // if eligible for dispute

    @Nullable
    public abstract String cancel_url(); //  if eligible for canceling

    @Nullable
    public abstract String fund_url(); //  contacts with escrow enabled but not funded

    @Nullable
    public abstract String details_receiver_name();

    @Nullable
    public abstract String details_receiver_email();

    @Nullable
    public abstract String details_iban();

    @Nullable
    public abstract String details_swift_bic();

    @Nullable
    public abstract String details_reference();

    @Nullable
    public abstract String details_ethereum_address();

    @Nullable
    public abstract String details_bsb();

    @Nullable
    public abstract String details_phone_number();

    @Nullable
    public abstract String details_biller_code();

    @Nullable
    public abstract String details_account_number();

    @Nullable
    public abstract String details_message();

    @Nullable
    public abstract String details_sort_code();

    public abstract String advertisement_id();

    @Nullable
    public abstract String advertisement_payment_method(); // online trades

    public abstract String advertisement_trade_type();

    public abstract String advertiser_username();

    public abstract String advertiser_feedback_score();

    public abstract String advertiser_trade_count();

    public abstract String advertiser_last_online();

    public abstract String advertiser_name();

    public abstract Boolean hasUnseenMessages();

    public abstract int messageCount();

    public static final Func1<SqlBrite.Query, ContactItem> MAP_SINGLE = new Func1<SqlBrite.Query, ContactItem>() {
        @Override
        public ContactItem call(SqlBrite.Query query) {
            Cursor cursor = query.run();
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, _ID);
                String contact_id = Db.getString(cursor, CONTACT_ID);
                String reference_code = Db.getString(cursor, REFERENCE_CODE);
                String currency = Db.getString(cursor, CURRENCY);
                String amount = Db.getString(cursor, AMOUNT);
                String amount_btc = Db.getString(cursor, AMOUNT_BTC);

                boolean is_funded = Db.getBoolean(cursor, IS_FUNDED);
                boolean is_selling = Db.getBoolean(cursor, IS_SELLING);
                boolean is_buying = Db.getBoolean(cursor, IS_BUYING);

                String created_at = Db.getString(cursor, CREATED_AT);
                String closed_at = Db.getString(cursor, CLOSED_AT);
                String disputed_at = Db.getString(cursor, DISPUTED_AT);
                String funded_at = Db.getString(cursor, FUNDED_AT);
                String escrowed_at = Db.getString(cursor, ESCROWED_AT);
                String canceled_at = Db.getString(cursor, CANCELED_AT);
                String released_at = Db.getString(cursor, RELEASED_AT);
                String payment_completed_at = Db.getString(cursor, PAYMENT_COMPLETED_AT);
                String exchange_rate_updated_at = Db.getString(cursor, EXCHANGE_RATE_UPDATED_AT);

                String seller_username = Db.getString(cursor, SELLER_USERNAME);
                String seller_feedback_score = Db.getString(cursor, SELLER_FEEDBACK);
                String seller_trade_count = Db.getString(cursor, SELLER_TRADES);
                String seller_last_online = Db.getString(cursor, SELLER_LAST_SEEN);
                String seller_name = Db.getString(cursor, SELLER_NAME);

                String buyer_username = Db.getString(cursor, BUYER_USERNAME);
                String buyer_feedback_score = Db.getString(cursor, BUYER_FEEDBACK);
                String buyer_trade_count = Db.getString(cursor, BUYER_TRADES);
                String buyer_last_online = Db.getString(cursor, BUYER_LAST_SEEN);
                String buyer_name = Db.getString(cursor, BUYER_NAME);

                String release_url = Db.getString(cursor, RELEASE_URL);
                String advertisement_public_view = Db.getString(cursor, ADVERTISEMENT_URL);
                String message_url = Db.getString(cursor, MESSAGE_URL);
                String message_post_url = Db.getString(cursor, MESSAGE_POST_URL);
                String mark_as_paid_url = Db.getString(cursor, MARK_PAID_URL);
                String dispute_url = Db.getString(cursor, DISPUTE_URL);
                String cancel_url = Db.getString(cursor, CANCEL_URL);
                String fund_url = Db.getString(cursor, FUND_URL);

                String details_receiver_name = Db.getString(cursor, RECEIVER_NAME);
                String details_receiver_email = Db.getString(cursor, RECEIVER_EMAIL);
                String details_iban = Db.getString(cursor, IBAN);
                String details_swift_bic = Db.getString(cursor, SWIFT_BIC);
                String details_reference = Db.getString(cursor, REFERENCE);
                String details_ethereum_address = Db.getString(cursor, ETHEREUM_ADDRESS);
                String details_bsb = Db.getString(cursor, BSB);
                String details_phone_number = Db.getString(cursor, PHONE_NUMBER);
                String details_biller_code = Db.getString(cursor, BILLER_CODE);
                String details_account_number = Db.getString(cursor, ACCOUNT_NUMBER);
                String details_message = Db.getString(cursor, MESSAGE);
                String details_sort_code = Db.getString(cursor, SORT_CODE);

                String advertisement_id = Db.getString(cursor, ADVERTISEMENT_ID);
                String advertisement_payment_method = Db.getString(cursor, ADVERTISEMENT_PAYMENT_METHOD);
                String advertisement_trade_type = Db.getString(cursor, ADVERTISEMENT_TRADE_TYPE);
                String advertiser_username = Db.getString(cursor, ADVERTISER_USERNAME);
                String advertiser_feedback_score = Db.getString(cursor, ADVERTISER_FEEDBACK);
                String advertiser_trade_count = Db.getString(cursor, ADVERTISER_TRADES);
                String advertiser_last_online = Db.getString(cursor, ADVERTISER_LAST_SEEN);
                String advertiser_name = Db.getString(cursor, ADVERTISER_NAME);

                boolean hasUnseenMessages = Db.getBoolean(cursor, UNSEEN_MESSAGES);
                int messageCount = Db.getInt(cursor, MESSAGE_COUNT);

                return new AutoParcel_ContactItem(id, contact_id, reference_code, currency, amount, amount_btc, is_funded, is_selling, is_buying,
                        created_at, closed_at, disputed_at, funded_at, escrowed_at, canceled_at, released_at, payment_completed_at, exchange_rate_updated_at,
                        seller_username, seller_feedback_score, seller_trade_count, seller_last_online, seller_name, buyer_username, buyer_feedback_score,
                        buyer_trade_count, buyer_last_online, buyer_name, release_url, advertisement_public_view, message_url, message_post_url, mark_as_paid_url,
                        dispute_url, cancel_url, fund_url, details_receiver_name, details_receiver_email, details_iban, details_swift_bic, details_reference,
                        details_ethereum_address, details_bsb, details_phone_number, details_biller_code, details_account_number, details_message, details_sort_code,
                        advertisement_id, advertisement_payment_method, advertisement_trade_type, advertiser_username, advertiser_feedback_score, advertiser_trade_count, advertiser_last_online,
                        advertiser_name, hasUnseenMessages, messageCount);
            }
            return null;
        }
    };

    public static final Func1<SqlBrite.Query, List<ContactItem>> MAP = new Func1<SqlBrite.Query, List<ContactItem>>() {
        @Override
        public List<ContactItem> call(SqlBrite.Query query) {
            Cursor cursor = query.run();
            List<ContactItem> values = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                Timber.d("SyncAdapter Cursor Count: " + cursor.getCount());
                long id = Db.getLong(cursor, _ID);
                String contact_id = Db.getString(cursor, CONTACT_ID);
                String reference_code = Db.getString(cursor, REFERENCE_CODE);
                String currency = Db.getString(cursor, CURRENCY);
                String amount = Db.getString(cursor, AMOUNT);
                String amount_btc = Db.getString(cursor, AMOUNT_BTC);

                boolean is_funded = Db.getBoolean(cursor, IS_FUNDED);
                boolean is_selling = Db.getBoolean(cursor, IS_SELLING);
                boolean is_buying = Db.getBoolean(cursor, IS_BUYING);

                String created_at = Db.getString(cursor, CREATED_AT);
                String closed_at = Db.getString(cursor, CLOSED_AT);
                String disputed_at = Db.getString(cursor, DISPUTED_AT);
                String funded_at = Db.getString(cursor, FUNDED_AT);
                String escrowed_at = Db.getString(cursor, ESCROWED_AT);
                String canceled_at = Db.getString(cursor, CANCELED_AT);
                String released_at = Db.getString(cursor, RELEASED_AT);
                String payment_completed_at = Db.getString(cursor, PAYMENT_COMPLETED_AT);
                String exchange_rate_updated_at = Db.getString(cursor, EXCHANGE_RATE_UPDATED_AT);

                String seller_username = Db.getString(cursor, SELLER_USERNAME);
                String seller_feedback_score = Db.getString(cursor, SELLER_FEEDBACK);
                String seller_trade_count = Db.getString(cursor, SELLER_TRADES);
                String seller_last_online = Db.getString(cursor, SELLER_LAST_SEEN);
                String seller_name = Db.getString(cursor, SELLER_NAME);

                String buyer_username = Db.getString(cursor, BUYER_USERNAME);
                String buyer_feedback_score = Db.getString(cursor, BUYER_FEEDBACK);
                String buyer_trade_count = Db.getString(cursor, BUYER_TRADES);
                String buyer_last_online = Db.getString(cursor, BUYER_LAST_SEEN);
                String buyer_name = Db.getString(cursor, BUYER_NAME);

                String release_url = Db.getString(cursor, RELEASE_URL);
                String advertisement_public_view = Db.getString(cursor, ADVERTISEMENT_URL);
                String message_url = Db.getString(cursor, MESSAGE_URL);
                String message_post_url = Db.getString(cursor, MESSAGE_POST_URL);
                String mark_as_paid_url = Db.getString(cursor, MARK_PAID_URL);
                String dispute_url = Db.getString(cursor, DISPUTE_URL);
                String cancel_url = Db.getString(cursor, CANCEL_URL);
                String fund_url = Db.getString(cursor, FUND_URL);

                String details_receiver_name = Db.getString(cursor, RECEIVER_NAME);
                String details_receiver_email = Db.getString(cursor, RECEIVER_EMAIL);
                String details_iban = Db.getString(cursor, IBAN);
                String details_swift_bic = Db.getString(cursor, SWIFT_BIC);
                String details_reference = Db.getString(cursor, REFERENCE);
                String details_ethereum_address = Db.getString(cursor, ETHEREUM_ADDRESS);
                String details_bsb = Db.getString(cursor, BSB);
                String details_phone_number = Db.getString(cursor, PHONE_NUMBER);
                String details_biller_code = Db.getString(cursor, BILLER_CODE);
                String details_account_number = Db.getString(cursor, ACCOUNT_NUMBER);
                String details_message = Db.getString(cursor, MESSAGE);
                String details_sort_code = Db.getString(cursor, SORT_CODE);

                String advertisement_id = Db.getString(cursor, ADVERTISEMENT_ID);
                String advertisement_payment_method = Db.getString(cursor, ADVERTISEMENT_PAYMENT_METHOD);
                String advertisement_trade_type = Db.getString(cursor, ADVERTISEMENT_TRADE_TYPE);
                String advertiser_username = Db.getString(cursor, ADVERTISER_USERNAME);
                String advertiser_feedback_score = Db.getString(cursor, ADVERTISER_FEEDBACK);
                String advertiser_trade_count = Db.getString(cursor, ADVERTISER_TRADES);
                String advertiser_last_online = Db.getString(cursor, ADVERTISER_LAST_SEEN);
                String advertiser_name = Db.getString(cursor, ADVERTISER_NAME);

                boolean hasUnseenMessages = Db.getBoolean(cursor, UNSEEN_MESSAGES);
                int messageCount = Db.getInt(cursor, MESSAGE_COUNT);

                values.add(new AutoParcel_ContactItem(id, contact_id, reference_code, currency, amount, amount_btc, is_funded, is_selling, is_buying,
                        created_at, closed_at, disputed_at, funded_at, escrowed_at, canceled_at, released_at, payment_completed_at, exchange_rate_updated_at,
                        seller_username, seller_feedback_score, seller_trade_count, seller_last_online, seller_name, buyer_username, buyer_feedback_score,
                        buyer_trade_count, buyer_last_online, buyer_name, release_url, advertisement_public_view, message_url, message_post_url, mark_as_paid_url,
                        dispute_url, cancel_url, fund_url, details_receiver_name, details_receiver_email, details_iban, details_swift_bic, details_reference,
                        details_ethereum_address, details_bsb, details_phone_number, details_biller_code, details_account_number, details_message, details_sort_code,
                        advertisement_id, advertisement_payment_method, advertisement_trade_type, advertiser_username, advertiser_feedback_score, advertiser_trade_count, advertiser_last_online,
                        advertiser_name, hasUnseenMessages, messageCount));
            }
            return values;
        }
    };

    public static ContactItem convertContact(Contact contact) {
        return new AutoParcel_ContactItem(0, contact.contact_id, contact.reference_code, contact.currency, contact.amount, contact.amount_btc, contact.is_funded, contact.is_selling, contact.is_buying,
                contact.created_at, contact.closed_at, contact.disputed_at, contact.funded_at, contact.escrowed_at, contact.canceled_at, contact.released_at, contact.payment_completed_at, contact.exchange_rate_updated_at,
                contact.seller.username, contact.seller.feedback_score, contact.seller.trade_count, contact.seller.last_online, contact.seller.name, contact.buyer.username, contact.buyer.feedback_score,
                contact.buyer.trade_count, contact.buyer.last_online, contact.buyer.name, contact.actions.release_url, contact.actions.advertisement_public_view, contact.actions.messages_url, contact.actions.message_post_url, contact.actions.mark_as_paid_url,
                contact.actions.dispute_url, contact.actions.cancel_url, contact.actions.fund_url,
                contact.account_details.receiver_name, contact.account_details.receiver_email, contact.account_details.iban, contact.account_details.sort_code, contact.account_details.reference, contact.account_details.ethereum_address,
                contact.account_details.bsb, contact.account_details.phone_number, contact.account_details.biller_code, contact.account_details.account_number, contact.account_details.message, contact.account_details.sort_code,
                contact.advertisement.id, contact.advertisement.payment_method, contact.advertisement.trade_type.name(), contact.advertisement.advertiser.username, contact.advertisement.advertiser.feedback_score, contact.advertisement.advertiser.trade_count, contact.advertisement.advertiser.last_online,
                contact.advertisement.advertiser.name, contact.hasUnseenMessages, contact.messageCount);
    }

    public static Builder createBuilder(Contact item, int messageCount, boolean hasUnseenMessages) {
        return new Builder()
                .contact_id(item.contact_id)
                .created_at(item.created_at)
                .amount(item.amount)
                .amount_btc(item.amount_btc)
                .currency(item.currency)
                .reference_code(item.reference_code)
                .is_buying(item.is_buying)
                .is_selling(item.is_selling)
                .payment_completed_at(item.payment_completed_at)
                .contact_id(item.contact_id)
                .disputed_at(item.disputed_at)
                .funded_at(item.funded_at)
                .escrowed_at(item.escrowed_at)
                .released_at(item.released_at)
                .canceled_at(item.canceled_at)
                .closed_at(item.closed_at)
                .disputed_at(item.disputed_at)
                .is_funded(item.is_funded)
                .fund_url(item.actions.fund_url)
                .release_url(item.actions.release_url)
                .advertisement_public_view(item.actions.advertisement_public_view)
                .message_url(item.actions.messages_url)
                .message_post_url(item.actions.message_post_url)
                .mark_as_paid_url(item.actions.mark_as_paid_url)
                .dispute_url(item.actions.dispute_url)
                .cancel_url(item.actions.cancel_url)
                .buyer_name(item.buyer.name)
                .buyer_username(item.buyer.username)
                .buyer_trade_count(item.buyer.trade_count)
                .buyer_feedback_score(item.buyer.feedback_score)
                .buyer_last_online(item.buyer.last_online)
                .seller_name(item.seller.name)
                .seller_username(item.seller.username)
                .seller_trade_count(item.seller.trade_count)
                .seller_feedback_score(item.seller.feedback_score)
                .seller_last_online(item.seller.last_online)

                .detail_receiver_email(item.account_details.receiver_email)
                .detail_receiver_name(item.account_details.receiver_name)
                .detail_iban(item.account_details.iban)
                .detail_swift_bic(item.account_details.swift_bic)
                .detail_account_number(item.account_details.account_number)
                .detail_biller_code(item.account_details.biller_code)
                .detail_bsb(item.account_details.bsb)
                .detail_message(item.account_details.message)
                .detail_ethereum_address(item.account_details.ethereum_address)
                .detail_phone_number(item.account_details.phone_number)
                .detail_sort_code(item.account_details.sort_code)
                .detail_reference(item.account_details.reference)

                .advertisement_id(item.advertisement.id)
                .advertisement_trade_type(item.advertisement.trade_type.name())
                .advertisement_payment_method(item.advertisement.payment_method)
                .advertiser_name(item.advertisement.advertiser.name)
                .advertiser_username(item.advertisement.advertiser.username)
                .advertiser_trade_count(item.advertisement.advertiser.trade_count)
                .advertiser_feedback_score(item.advertisement.advertiser.feedback_score)
                .advertiser_last_online(item.advertisement.advertiser.last_online)
                .unseen_messages(hasUnseenMessages)
                .message_count(messageCount);
    }

    public static ContactItem getModel(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            long id = Db.getLong(cursor, _ID);
            String contact_id = Db.getString(cursor, CONTACT_ID);
            String reference_code = Db.getString(cursor, REFERENCE_CODE);
            String currency = Db.getString(cursor, CURRENCY);
            String amount = Db.getString(cursor, AMOUNT);
            String amount_btc = Db.getString(cursor, AMOUNT_BTC);

            boolean is_funded = Db.getBoolean(cursor, IS_FUNDED);
            boolean is_selling = Db.getBoolean(cursor, IS_SELLING);
            boolean is_buying = Db.getBoolean(cursor, IS_BUYING);

            String created_at = Db.getString(cursor, CREATED_AT);
            String closed_at = Db.getString(cursor, CLOSED_AT);
            String disputed_at = Db.getString(cursor, DISPUTED_AT);
            String funded_at = Db.getString(cursor, FUNDED_AT);
            String escrowed_at = Db.getString(cursor, ESCROWED_AT);
            String canceled_at = Db.getString(cursor, CANCELED_AT);
            String released_at = Db.getString(cursor, RELEASED_AT);
            String payment_completed_at = Db.getString(cursor, PAYMENT_COMPLETED_AT);
            String exchange_rate_updated_at = Db.getString(cursor, EXCHANGE_RATE_UPDATED_AT);

            String seller_username = Db.getString(cursor, SELLER_USERNAME);
            String seller_feedback_score = Db.getString(cursor, SELLER_FEEDBACK);
            String seller_trade_count = Db.getString(cursor, SELLER_TRADES);
            String seller_last_online = Db.getString(cursor, SELLER_LAST_SEEN);
            String seller_name = Db.getString(cursor, SELLER_NAME);

            String buyer_username = Db.getString(cursor, BUYER_USERNAME);
            String buyer_feedback_score = Db.getString(cursor, BUYER_FEEDBACK);
            String buyer_trade_count = Db.getString(cursor, BUYER_TRADES);
            String buyer_last_online = Db.getString(cursor, BUYER_LAST_SEEN);
            String buyer_name = Db.getString(cursor, BUYER_NAME);

            String release_url = Db.getString(cursor, RELEASE_URL);
            String advertisement_public_view = Db.getString(cursor, ADVERTISEMENT_URL);
            String message_url = Db.getString(cursor, MESSAGE_URL);
            String message_post_url = Db.getString(cursor, MESSAGE_POST_URL);
            String mark_as_paid_url = Db.getString(cursor, MARK_PAID_URL);
            String dispute_url = Db.getString(cursor, DISPUTE_URL);
            String cancel_url = Db.getString(cursor, CANCEL_URL);
            String fund_url = Db.getString(cursor, FUND_URL);

            String details_receiver_name = Db.getString(cursor, RECEIVER_NAME);
            String details_receiver_email = Db.getString(cursor, RECEIVER_EMAIL);
            String details_iban = Db.getString(cursor, IBAN);
            String details_swift_bic = Db.getString(cursor, SWIFT_BIC);
            String details_reference = Db.getString(cursor, REFERENCE);
            String details_ethereum_address = Db.getString(cursor, ETHEREUM_ADDRESS);
            String details_bsb = Db.getString(cursor, BSB);
            String details_phone_number = Db.getString(cursor, PHONE_NUMBER);
            String details_biller_code = Db.getString(cursor, BILLER_CODE);
            String details_account_number = Db.getString(cursor, ACCOUNT_NUMBER);
            String details_message = Db.getString(cursor, MESSAGE);
            String details_sort_code = Db.getString(cursor, SORT_CODE);

            String advertisement_id = Db.getString(cursor, ADVERTISEMENT_ID);
            String advertisement_payment_method = Db.getString(cursor, ADVERTISEMENT_PAYMENT_METHOD);
            String advertisement_trade_type = Db.getString(cursor, ADVERTISEMENT_TRADE_TYPE);
            String advertiser_username = Db.getString(cursor, ADVERTISER_USERNAME);
            String advertiser_feedback_score = Db.getString(cursor, ADVERTISER_FEEDBACK);
            String advertiser_trade_count = Db.getString(cursor, ADVERTISER_TRADES);
            String advertiser_last_online = Db.getString(cursor, ADVERTISER_LAST_SEEN);
            String advertiser_name = Db.getString(cursor, ADVERTISER_NAME);

            boolean hasUnseenMessages = Db.getBoolean(cursor, UNSEEN_MESSAGES);
            int messageCount = Db.getInt(cursor, MESSAGE_COUNT);

            return new AutoParcel_ContactItem(id, contact_id, reference_code, currency, amount, amount_btc, is_funded, is_selling, is_buying,
                    created_at, closed_at, disputed_at, funded_at, escrowed_at, canceled_at, released_at, payment_completed_at, exchange_rate_updated_at,
                    seller_username, seller_feedback_score, seller_trade_count, seller_last_online, seller_name, buyer_username, buyer_feedback_score,
                    buyer_trade_count, buyer_last_online, buyer_name, release_url, advertisement_public_view, message_url, message_post_url, mark_as_paid_url,
                    dispute_url, cancel_url, fund_url, details_receiver_name, details_receiver_email, details_iban, details_swift_bic, details_reference,
                    details_ethereum_address, details_bsb, details_phone_number, details_biller_code, details_account_number, details_message, details_sort_code,
                    advertisement_id, advertisement_payment_method, advertisement_trade_type, advertiser_username, advertiser_feedback_score, advertiser_trade_count, advertiser_last_online,
                    advertiser_name, hasUnseenMessages, messageCount);
        }
        return null;
    }

    public static final class Builder {

        private final ContentValues values = new ContentValues();

        public Builder id(long id) {
            values.put(_ID, id);
            return this;
        }

        public Builder contact_id(String value) {
            values.put(CONTACT_ID, value);
            return this;
        }

        public Builder reference_code(String value) {
            values.put(REFERENCE_CODE, value);
            return this;
        }

        public Builder currency(String value) {
            values.put(CURRENCY, value);
            return this;
        }

        public Builder amount(String value) {
            values.put(AMOUNT, value);
            return this;
        }

        public Builder amount_btc(String value) {
            values.put(AMOUNT_BTC, value);
            return this;
        }

        public Builder is_funded(boolean value) {
            values.put(IS_FUNDED, value);
            return this;
        }

        public Builder is_selling(boolean value) {
            values.put(IS_SELLING, value);
            return this;
        }

        public Builder is_buying(boolean value) {
            values.put(IS_BUYING, value);
            return this;
        }

        public Builder created_at(String value) {
            values.put(CREATED_AT, value);
            return this;
        }

        public Builder closed_at(String value) {
            values.put(CLOSED_AT, value);
            return this;
        }

        public Builder disputed_at(String value) {
            values.put(DISPUTED_AT, value);
            return this;
        }

        public Builder funded_at(String value) {
            values.put(FUNDED_AT, value);
            return this;
        }

        public Builder escrowed_at(String value) {
            values.put(ESCROWED_AT, value);
            return this;
        }

        public Builder canceled_at(String value) {
            values.put(CANCELED_AT, value);
            return this;
        }

        public Builder released_at(String value) {
            values.put(RELEASED_AT, value);
            return this;
        }

        public Builder payment_completed_at(String value) {
            values.put(PAYMENT_COMPLETED_AT, value);
            return this;
        }

        public Builder exchange_rate_updated_at(String value) {
            values.put(EXCHANGE_RATE_UPDATED_AT, value);
            return this;
        }

        public Builder seller_username(String value) {
            values.put(SELLER_USERNAME, value);
            return this;
        }

        public Builder seller_feedback_score(String value) {
            values.put(SELLER_FEEDBACK, value);
            return this;
        }

        public Builder seller_trade_count(String value) {
            values.put(SELLER_TRADES, value);
            return this;
        }

        public Builder seller_last_online(String value) {
            values.put(SELLER_LAST_SEEN, value);
            return this;
        }

        public Builder seller_name(String value) {
            values.put(SELLER_NAME, value);
            return this;
        }

        public Builder buyer_username(String value) {
            values.put(BUYER_USERNAME, value);
            return this;
        }

        public Builder buyer_feedback_score(String value) {
            values.put(BUYER_FEEDBACK, value);
            return this;
        }

        public Builder buyer_trade_count(String value) {
            values.put(BUYER_TRADES, value);
            return this;
        }

        public Builder buyer_last_online(String value) {
            values.put(BUYER_LAST_SEEN, value);
            return this;
        }

        public Builder buyer_name(String value) {
            values.put(BUYER_NAME, value);
            return this;
        }

        public Builder release_url(String value) {
            values.put(RELEASE_URL, value);
            return this;
        }

        public Builder advertisement_public_view(String value) {
            values.put(ADVERTISEMENT_URL, value);
            return this;
        }

        public Builder message_url(String value) {
            values.put(MESSAGE_URL, value);
            return this;
        }

        public Builder message_post_url(String value) {
            values.put(MESSAGE_POST_URL, value);
            return this;
        }

        public Builder mark_as_paid_url(String value) {
            values.put(MARK_PAID_URL, value);
            return this;
        }

        public Builder dispute_url(String value) {
            values.put(DISPUTE_URL, value);
            return this;
        }

        public Builder cancel_url(String value) {
            values.put(CANCEL_URL, value);
            return this;
        }

        public Builder fund_url(String value) {
            values.put(FUND_URL, value);
            return this;
        }

        public Builder advertisement_id(String value) {
            values.put(ADVERTISEMENT_ID, value);
            return this;
        }

        public Builder advertisement_payment_method(String value) {
            values.put(ADVERTISEMENT_PAYMENT_METHOD, value);
            return this;
        }

        public Builder advertisement_trade_type(String value) {
            values.put(ADVERTISEMENT_TRADE_TYPE, value);
            return this;
        }

        public Builder advertiser_username(String value) {
            values.put(ADVERTISER_USERNAME, value);
            return this;
        }

        public Builder advertiser_feedback_score(String value) {
            values.put(ADVERTISER_FEEDBACK, value);
            return this;
        }

        public Builder advertiser_trade_count(String value) {
            values.put(ADVERTISER_TRADES, value);
            return this;
        }

        public Builder advertiser_last_online(String value) {
            values.put(ADVERTISER_LAST_SEEN, value);
            return this;
        }

        public Builder advertiser_name(String value) {
            values.put(ADVERTISER_NAME, value);
            return this;
        }

        public Builder message_count(int value) {
            values.put(MESSAGE_COUNT, value);
            return this;
        }

        public Builder unseen_messages(boolean value) {
            values.put(UNSEEN_MESSAGES, value);
            return this;
        }

        public Builder detail_ethereum_address(String value) {
            values.put(ETHEREUM_ADDRESS, value);
            return this;
        }

        public Builder detail_phone_number(String value) {
            values.put(PHONE_NUMBER, value);
            return this;
        }

        public Builder detail_biller_code(String value) {
            values.put(BILLER_CODE, value);
            return this;
        }

        public Builder detail_account_number(String value) {
            values.put(ACCOUNT_NUMBER, value);
            return this;
        }

        public Builder detail_sort_code(String value) {
            values.put(SORT_CODE, value);
            return this;
        }

        public Builder detail_message(String value) {
            values.put(MESSAGE, value);
            return this;
        }

        public Builder detail_bsb(String value) {
            values.put(BSB, value);
            return this;
        }

        public Builder detail_receiver_name(String value) {
            values.put(RECEIVER_NAME, value);
            return this;
        }

        public Builder detail_iban(String value) {
            values.put(IBAN, value);
            return this;
        }

        public Builder detail_swift_bic(String value) {
            values.put(SWIFT_BIC, value);
            return this;
        }

        public Builder detail_reference(String value) {
            values.put(REFERENCE, value);
            return this;
        }

        public Builder detail_receiver_email(String value) {
            values.put(RECEIVER_EMAIL, value);
            return this;
        }


        public ContentValues build() {
            return values;
        }
    }
}

