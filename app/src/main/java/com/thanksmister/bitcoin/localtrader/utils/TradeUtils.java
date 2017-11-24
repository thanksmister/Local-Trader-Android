/*
 * Copyright (c) 2015 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class TradeUtils {
    
    public static final String PAYPAL = "PAYPAL";
    public static final String NETELLER = "NETELLER";
    public static final String QIWI = "QIWI";
    public static final String SEPA = "SEPA";
    public static final String ALTCOIN_ETH = "ALTCOIN_ETH";
    public static final String INTERNATIONAL_WIRE_SWIFT = "INTERNATIONAL_WIRE_SWIFT";
    public static final String GIFT_CARD_CODE = "GIFT_CARD_CODE";
    public static final String NATIONAL_BANK = "NATIONAL_BANK";
    public static final String CASH_DEPOSIT = "CASH_DEPOSIT";
    public static final String SPECIFIC_BANK = "SPECIFIC_BANK";
    public static final String OTHER = "OTHER";
    public static final String OTHER_REMITTANCE = "OTHER_REMITTANCE";
    public static final String OTHER_ONLINE_WALLET = "OTHER_ONLINE_WALLET";
    public static final String OTHER_PRE_PAID_DEBIT = "OTHER_PRE_PAID_DEBIT";
    public static final String OTHER_ONLINE_WALLET_GLOBAL = "OTHER_ONLINE_WALLET_GLOBAL";
    public static final String BPAY = "BPAY";
    public static final String PAYTM = "PAYTM";
    public static final String INTERAC = "INTERAC";
    public static final String LYDIA = "LYDIA";
    public static final String ALIPAY = "ALIPAY";
    public static final String EASYPAISA = "EASYPAISA";
    public static final String HAL_CASH = "HAL_CASH";
    public static final String SWISH = "SWISH";
    public static final String MOBILEPAY_DANSKE_BANK_DK = "MOBILEPAY_DANSKE_BANK_DK";
    public static final String MOBILEPAY_DANSKE_BANK = "MOBILEPAY_DANSKE_BANK";
    public static final String MOBILEPAY_DANSKE_BANK_NO = "MOBILEPAY_DANSKE_BANK_NO";
    public static final String VIPPS = "VIPPS";

    public static String getContactDescription(ContactItem contact, Context context) {

         if (isCanceledTrade(contact)) {

             return isLocalTrade(contact) ? context.getString(R.string.order_description_cancel_local) : context.getString(R.string.order_description_cancel);

         } else if (isReleased(contact)) {

            return context.getString(R.string.order_description_released);

        } else if (isDisputed(contact)) {

            return context.getString(R.string.order_description_disputed);

         } else if (isClosedTrade(contact)) {

             return context.getString(R.string.order_description_closed);
             
        } else if (isLocalTrade(contact)) {

            if (youAreAdvertiser(contact) && contact.is_selling()) {

                if (contact.is_funded()) {
                    //return canFundTrade(contact)? context.getString(R.string.order_description_funded_local):context.getString(R.string.order_description_funded_local_no_action);
                    return context.getString(R.string.order_description_funded_local);
                } else {
                    //return canReleaseTrade(contact)? context.getString(R.string.order_description_not_funded_local):context.getString(R.string.order_description_not_funded_local_no_action);  
                    return context.getString(R.string.order_description_not_funded_local);
                }

            } else {

                if (contact.is_funded()) {
                    return context.getString(R.string.order_description_funded_local);
                } else {
                    return context.getString(R.string.order_description_not_funded_local);
                }
            }

        } else if (isOnlineTrade(contact)) {

            if (contact.is_buying()) {
                return isMarkedPaid(contact) ? context.getString(R.string.order_description_paid) : context.getString(R.string.order_description_mark_paid);
            } else {
                return isMarkedPaid(contact) ? context.getString(R.string.order_description_online_paid) : context.getString(R.string.order_description_online_mark_paid);
            }
        }

        return null;
    }

    public static int getTradeActionButtonLabel(ContactItem contact) {
        
        if (isClosedTrade(contact) || isReleased(contact)) {
            return 0;
        }

        if (isLocalTrade(contact)) { // selling or buying locally with ad

            if (contact.is_selling()) { // ad to sell bitcoins locally 

                if (contact.is_funded() || isFunded(contact)) { // TODO is this available for local?
                    return R.string.button_release;
                } else {
                    return R.string.button_fund;
                }
            }

            return R.string.button_cancel;

        } else if (isOnlineTrade(contact)) {   // handle online trade ads

            if (contact.is_buying()) { // ad to buy bitcoins  

                return isMarkedPaid(contact) ? R.string.button_dispute : R.string.button_mark_paid;

            } else { // ad to sell bitcoins 

                return R.string.button_release;
            }
        }

        return 0;
    }

    public static boolean youAreAdvertiser(ContactItem contact) {
        if (contact.is_selling()) { // you are selling
            return contact.advertiser_username().equals(contact.seller_username());
        } else {  // you are buying
            return contact.advertiser_username().equals(contact.buyer_username());
        }
    }
    
    public static boolean tradeIsActive(String closedAt, String canceledAt){
        return (TextUtils.isEmpty(closedAt) && TextUtils.isEmpty(canceledAt));
    }
    
    private static boolean isCanceledTrade(ContactItem contact) {
        return !TextUtils.isEmpty(contact.canceled_at());
    }

    public static boolean isCanceledTrade(Contact contact) {
        return contact.canceled_at != null;
    }

    public static boolean isMarkedPaid(ContactItem contact) {
        return contact.payment_completed_at() != null;
    }

    public static boolean isFunded(ContactItem contact) {
        return contact.funded_at() != null;
    }

    public static boolean isReleased(ContactItem contact) {
        return contact.released_at() != null;
    }

    public static boolean isReleased(Contact contact) {
        return contact.released_at != null;
    }

    public static boolean isDisputed(ContactItem contact) {
        return contact.disputed_at() != null;
    }

    private static boolean isClosedTrade(ContactItem contact) {
        return !TextUtils.isEmpty(contact.closed_at());
    }

    public static boolean isClosedTrade(Contact contact) {
        return contact.closed_at != null;
    }

    public static boolean canDisputeTrade(ContactItem contact) {
        //return isClosedTrade(contact) && !isDisputed(contact) && !TextUtils.isEmpty(contact.dispute_url());
        return !TextUtils.isEmpty(contact.dispute_url());
    }

    public static boolean canCancelTrade(ContactItem contact) {
        //return !isClosedTrade(contact) && !isCanceledTrade(contact) && !TextUtils.isEmpty(contact.cancel_url());
        return !TextUtils.isEmpty(contact.cancel_url());
    }

    public static boolean canReleaseTrade(ContactItem contact) {
        return !isClosedTrade(contact);
    }

    public static boolean canFundTrade(ContactItem contact) {
        return !isClosedTrade(contact);
    }

    public static boolean isLocalTrade(TradeType tradeType) {
        return (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL);
    }

    public static boolean isLocalTrade(Contact contact) {
        TradeType tradeType = contact.advertisement.trade_type;
        return (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL);
    }

    public static boolean isLocalTrade(ContactItem contact) {
        TradeType tradeType = TradeType.valueOf(contact.advertisement_trade_type());
        return (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL);
    }

    public static boolean isLocalTrade(Advertisement advertisement) {
        return (advertisement.trade_type == TradeType.LOCAL_BUY || advertisement.trade_type == TradeType.LOCAL_SELL);
    }

    public static boolean isLocalTrade(AdvertisementItem advertisement) {
        return (advertisement.trade_type().equals(TradeType.LOCAL_BUY.name()) || advertisement.trade_type().equals(TradeType.LOCAL_SELL.name()));
    }

    public static boolean isAtm(AdvertisementItem advertisement) {
        return (!Strings.isBlank(advertisement.atm_model()));
    }

    public static boolean isOnlineTrade(ContactItem contact) {
        TradeType tradeType = TradeType.valueOf(contact.advertisement_trade_type());
        return (tradeType == TradeType.ONLINE_BUY || tradeType == TradeType.ONLINE_SELL);
    }

    public static boolean isOnlineTrade(TradeType tradeType) {
        return (tradeType == TradeType.ONLINE_BUY || tradeType == TradeType.ONLINE_SELL);
    }


    public static boolean isOnlineTrade(Advertisement advertisement) {
        return (advertisement.trade_type == TradeType.ONLINE_BUY || advertisement.trade_type == TradeType.ONLINE_SELL);
    }

    public static boolean isSellTrade(AdvertisementItem advertisement) {
        return (advertisement.trade_type().equals(TradeType.ONLINE_SELL.name()) || advertisement.trade_type().equals(TradeType.LOCAL_SELL.name()));
    }

    public static boolean isBuyTrade(AdvertisementItem advertisement) {
        return (advertisement.trade_type().equals(TradeType.ONLINE_BUY.name()) || advertisement.trade_type().equals(TradeType.LOCAL_BUY.name()));
    }

    public static boolean isOnlineTrade(AdvertisementItem advertisement) {
        return (advertisement.trade_type().equals(TradeType.ONLINE_BUY.name()) || advertisement.trade_type().equals(TradeType.ONLINE_SELL.name()));
    }

    public static Method getMethodForAdvertisement(String online_provider, List<Method> methods) {
        for (Method m : methods) {
            if (online_provider.equals(m.code)) {
                return m;
            }
        }

        return null;
    }

    public static MethodItem getMethodForAdvertisement(Advertisement advertisement, List<MethodItem> methods) {
        for (MethodItem m : methods) {
            if (advertisement.online_provider.equals(m.code())) {
                return m;
            }
        }

        return null;
    }

    public static MethodItem getMethodForAdvertisement(AdvertisementItem advertisement, List<MethodItem> methods) {
        for (MethodItem m : methods) {
            if (advertisement.online_provider().equals(m.code())) {
                return m;
            }
        }

        return null;
    }

    public static String getPaymentMethod(String code, List<MethodItem> methods) {
        for (MethodItem method : methods) {
            if (method.code().equals(code)) {
                if (Strings.isBlank(method.key()))
                    return code;

                return method.key();
            }
        }
        return code;
    }
    
    public static String getPaymentMethodFromItems(AdvertisementItem advertisement, List<MethodItem> methodItems) {
        if (methodItems == null || methodItems.isEmpty()) {
            return "";
        }

        String paymentMethod = "";

        for (MethodItem method : methodItems) {
            if (method.code().equals(advertisement.online_provider())) {
                paymentMethod = getPaymentMethod(advertisement, method);
                break;
            }
        }
        return paymentMethod;
    }

    public static String getPaymentMethod(Advertisement advertisement, List<MethodItem> methods) {
        String paymentMethod = "";
        for (MethodItem method : methods) {
            if (method.code().equals(advertisement.online_provider)) {
                paymentMethod = getPaymentMethod(advertisement, method);
                break;
            }
        }
        return paymentMethod;
    }

    public static String getPaymentMethodName(Advertisement advertisement, MethodItem method) {
        String paymentMethod = "Other";
        if (method != null && method.code().equals(advertisement.online_provider)) {
            paymentMethod = method.name();
        }

        return paymentMethod;
    }

    public static String getPaymentMethodName(AdvertisementItem advertisement, MethodItem method) {
        String paymentMethod = "Other";
        if (method != null && method.code().equals(advertisement.online_provider())) {
            paymentMethod = method.name();
        }

        return paymentMethod;
    }
    
    public static String getPaymentMethodName(String paymentMethod) {
        switch (paymentMethod) {
            case "NATIONAL_BANK":
                return "National Bank transfer";
            case "CASH_DEPOSIT":
                return "Cash deposit";
            case "SPECIFIC_BANK":
                return "Bank transfer";
        }

        return paymentMethod;
    }

    public static String getPaymentMethod(AdvertisementItem advertisement, MethodItem method) {
        String paymentMethod = "Online";
        if (method != null && method.code().equals(advertisement.online_provider())) {
            if (method.code().equals("NATIONAL_BANK")) {
                if (Strings.isBlank(method.countryName()))
                    return "National Bank transfer";

                return "bank transfer in " + method.countryName();
            } else if (method.code().equals("CASH_DEPOSIT")) {
                if (Strings.isBlank(method.countryName()))
                    return "Cash deposit";

                return "cash deposit in " + method.countryName();
            } else if (method.code().equals("SPECIFIC_BANK")) {
                if (Strings.isBlank(method.countryName()))
                    return "Bank transfer";

                return "bank transfer in " + method.countryName();
            }

            paymentMethod = method.name();
        }

        if (!Strings.isBlank(advertisement.bank_name()) && advertisement.online_provider().equals("NATIONAL_BANK")) {
            return paymentMethod + " with " + advertisement.bank_name();
        }

        return paymentMethod;
    }

    public static String getPaymentMethod(Advertisement advertisement, MethodItem method) {
        String paymentMethod = "Online";
        if (method != null && method.code().equals(advertisement.online_provider)) {
            if (method.code().equals("NATIONAL_BANK")) {
                if (Strings.isBlank(method.countryName()))
                    return "National Bank transfer";

                return "bank transfer in " + method.countryName();
            } else if (method.code().equals("CASH_DEPOSIT")) {
                if (Strings.isBlank(method.countryName()))
                    return "Cash deposit";

                return "cash deposit in " + method.countryName();
            } else if (method.code().equals("SPECIFIC_BANK")) {
                if (Strings.isBlank(method.countryName()))
                    return "Bank transfer";

                return "bank transfer in " + method.countryName();
            }

            paymentMethod = method.name();
        }

        if (!Strings.isBlank(advertisement.bank_name) && advertisement.online_provider.equals("NATIONAL_BANK")) {
            return paymentMethod + " with " + advertisement.bank_name;
        }

        return paymentMethod;
    }

    public static String getContactName(Contact contact) {
        if (contact.is_selling) {
            return contact.buyer.username;
        } else {
            return contact.seller.username;
        }
    }

    public static int determineLastSeenIcon(@NonNull String lasOnline) {
        Date now = new Date();
        Date lastSeen = Dates.parseLastSeenDate(lasOnline);

        long diff = now.getTime() - lastSeen.getTime();

        if ((diff > 1800000) && (diff < 10800000)) {
            return R.drawable.last_seen_shortly;
        } else if (diff > 10800000) {
            return R.drawable.last_seen_long;
        }

        return R.drawable.last_seen_recently;
    }
    
    public static String[] parseUserString(String value) {
        String[] nameSplit;
        if (!value.contains(" ")) {
            ArrayList<String> stringArrayList = new ArrayList<String>();
            stringArrayList.add(value);
            nameSplit = stringArrayList.toArray(new String[stringArrayList.size()]);
            return nameSplit;
        }

        // strip out any parenthesis and split on spacing?
        value = value.replaceAll("(\\()", "");
        value = value.replaceAll("(\\))", "");
        value = value.replaceAll("(\\;)", "");
        nameSplit = value.split(" ");
        return nameSplit;
    }

    public static String parsePaymentService(String value) {
        // strip out any parenthesis and split on spacing?
        value = value.replaceAll("(\\_)", " ");

        String[] words = value.split(" ");
        StringBuilder sb = new StringBuilder();
        if (words[0].length() > 0) {
            sb.append(Character.toUpperCase(words[0].charAt(0)) + words[0].subSequence(1, words[0].length()).toString().toLowerCase());
            for (int i = 1; i < words.length; i++) {
                sb.append(" ");
                sb.append(Character.toUpperCase(words[i].charAt(0)) + words[i].subSequence(1, words[i].length()).toString().toLowerCase());
            }
        }

        return sb.toString();
    }

    public static String parsePaymentServiceTitle(String value) {
        value = value.replaceAll("(\\_)", " ");

        String[] words = value.split(" ");
        StringBuilder sb = new StringBuilder();
        if (words[0].length() > 0) {
            sb.append(Character.toUpperCase(words[0].charAt(0)) + words[0].subSequence(1, words[0].length()).toString().toLowerCase());
            for (int i = 1; i < words.length; i++) {
                sb.append(" ");
                sb.append(Character.toUpperCase(words[i].charAt(0)) + words[i].subSequence(1, words[i].length()).toString().toLowerCase());
            }
        }
        String titleCaseValue = sb.toString();
        return titleCaseValue;
    }

    public static String kilometersToMiles(String km) {
        double mi = Doubles.convertToDouble(km) * .62137;
        DecimalFormat precision = new DecimalFormat("0.00");
        return precision.format(mi);
    }

    public static String convertCurrencyAmount(String value) {
        try {
            //Locale locUS = new Locale("en_US");
            NumberFormat numberFormat = NumberFormat.getNumberInstance();
            double result = numberFormat.parse(value).intValue();
            DecimalFormat decimalFormat = new DecimalFormat("#");
            return decimalFormat.format(result);
        } catch (Exception e) {
            return value;
        }
    }
    
    public static String getBankNameTitle(TradeType tradeType, String onlineProvider) {
            String bankTitle = null;
            if (tradeType == TradeType.ONLINE_SELL) {
                switch (onlineProvider) {
                    case TradeUtils.INTERNATIONAL_WIRE_SWIFT:
                        bankTitle = "Bank SWIFT";
                        break;
                    case TradeUtils.CASH_DEPOSIT:
                    case TradeUtils.SPECIFIC_BANK:
                    case TradeUtils.NATIONAL_BANK:
                        bankTitle = "Bank name (required)";
                        break;
                    case TradeUtils.OTHER:
                    case TradeUtils.OTHER_REMITTANCE:
                    case TradeUtils.OTHER_PRE_PAID_DEBIT:
                    case TradeUtils.OTHER_ONLINE_WALLET_GLOBAL:
                    case TradeUtils.OTHER_ONLINE_WALLET:
                        bankTitle = "Payment method name";
                        break;
                    case TradeUtils.GIFT_CARD_CODE:
                        bankTitle = "Gift Card Issuer: [AMC Theatres, Airbnb, American Express, Best Buy, Dell, GA2, GameStop, Google Play, Groupon, Home Depot, Lowe, Lyft, Microsoft Windows Store, Netflix, Other, Papa John's Pizza, PlayStation Store, Regal Cinemas, Skype Credit, Target, Uber, Whole Foods Market, Wolt, Xbox]";
                        break;
                    default:
                        break;
                }
            } else if (tradeType == TradeType.ONLINE_BUY) {
                switch (onlineProvider) {
                    case TradeUtils.NATIONAL_BANK:
                    case TradeUtils.CASH_DEPOSIT:
                    case TradeUtils.SPECIFIC_BANK:
                        bankTitle = "Bank name (required)";
                        break;
                    case TradeUtils.OTHER:
                    case TradeUtils.OTHER_REMITTANCE:
                    case TradeUtils.OTHER_PRE_PAID_DEBIT:
                    case TradeUtils.OTHER_ONLINE_WALLET_GLOBAL:
                    case TradeUtils.OTHER_ONLINE_WALLET:
                        bankTitle = "Payment method name";
                        break;
                    case TradeUtils.INTERNATIONAL_WIRE_SWIFT:
                        bankTitle = "Bank SWIFT";
                        break;
                    case TradeUtils.GIFT_CARD_CODE:
                        bankTitle = "Gift Card Issuer: [AMC Theatres, Airbnb, American Express, Best Buy, Dell, GA2, GameStop, Google Play, Groupon, Home Depot, Lowe, Lyft, Microsoft Windows Store, Netflix, Other, Papa John's Pizza, PlayStation Store, Regal Cinemas, Skype Credit, Target, Uber, Whole Foods Market, Wolt, Xbox]";
                        break;
                    default:
                        break;
                }
            }
            
            return bankTitle;
    }

    // TODO unit test
    public static String getEthereumPriceEquation(TradeType tradeType, String margin) {

        String equation = "btc_in_eth";
        
        if (!Strings.isBlank(margin)) {
            double marginValue = 1.0;
            try {
                marginValue = Doubles.convertToDouble(margin);
            } catch (Exception e) {
                Timber.e(e.getMessage());
            }

            double marginPercent = 1.0;
            if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.ONLINE_BUY) {
                marginPercent = 1 - marginValue / 100;
            } else {
                marginPercent = 1 + marginValue / 100;
            }
            equation = equation + "*" + marginPercent;
        } else {
            equation = equation + "*" + Constants.DEFAULT_MARGIN;
        }

        return equation;
    }

    // TODO unit test
    public static String getPriceEquation(@NonNull TradeType tradeType, @NonNull String margin, @NonNull String currency) {

        String equation = Constants.DEFAULT_PRICE_EQUATION;
        if (!currency.equals(Constants.DEFAULT_CURRENCY)) {
            equation = equation + "*" + Constants.DEFAULT_CURRENCY + "_in_" + currency;
        }
        
        if (!Strings.isBlank(margin)) {

            double marginValue = 1.0;
            try {
                marginValue = Doubles.convertToDouble(margin);
            } catch (Exception e) {
                Timber.e(e.getMessage());
            }

            double marginPercent = 1.0;
            if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.ONLINE_BUY) {
                marginPercent = 1 - marginValue / 100;
            } else {
                marginPercent = 1 + marginValue / 100;
            }
            equation = equation + "*" + marginPercent;
        } else {
            equation = equation + "*" + Constants.DEFAULT_MARGIN;
        }

        return equation;
    }
    
    public static List<MethodItem> sortMethods(List<MethodItem> methods) {
        Collections.sort(methods, new MethodNameComparator());
        return methods;
    }

    private static class MethodNameComparator implements Comparator<MethodItem> {
        @Override
        public int compare(MethodItem o1, MethodItem o2) {
            return o1.name().toLowerCase().compareTo(o2.name().toLowerCase());
        }
    }
}