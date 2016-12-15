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
import android.location.Address;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactSync;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TradeUtils
{
    public static final String PAYPAL = "PAYPAL";
    public static final String NETELLER = "NETELLER";
    public static final String QIWI = "QIWI";
    public static final String SEPA = "SEPA";
    public static final String NATIONAL_BANK = "NATIONAL_BANK";
    public static final String BPAY = "BPAY";
    public static final String INTERAC = "INTERAC";
    public static final String ALIPAY = "ALIPAY";
    public static final String EASYPAISA = "EASYPAISA";
    public static final String HAL_CASH = "HAL_CASH";
    public static final String SWISH = "SWISH";
    public static final String MOBILEPAY_DANSKE_BANK_DK = "MOBILEPAY_DANSKE_BANK_DK";
    public static final String MOBILEPAY_DANSKE_BANK = "MOBILEPAY_DANSKE_BANK";
    public static final String MOBILEPAY_DANSKE_BANK_NO = "MOBILEPAY_DANSKE_BANK_NO";
    public static final String VIPPS = "VIPPS";
    
    public static String getContactDescription(ContactItem contact, Context context)
    {
        if(isCanceledTrade(contact)) {
            
            return isLocalTrade(contact)? context.getString(R.string.order_description_cancel_local):context.getString(R.string.order_description_cancel); 
        
        } else if (isReleased(contact)) {

            return context.getString(R.string.order_description_released);
            
        } else if (isDisputed(contact)) {

            return context.getString(R.string.order_description_disputed);
            
        } else if (isLocalTrade(contact)) {

           /* if(contact.youAreSelling()) { 
                if(Utils.convertToDouble(wallet.getBalance()) < Utils.convertToDouble(contact.getAmount_btc())) {
                    return context.getString(R.string.order_description_no_funds);
                }
            }*/

        
            if(youAreAdvertiser(contact) && contact.is_selling()) {

                if(contact.is_funded()) {
                    //return canFundTrade(contact)? context.getString(R.string.order_description_funded_local):context.getString(R.string.order_description_funded_local_no_action);
                    return  context.getString(R.string.order_description_funded_local);
                } else {
                    //return canReleaseTrade(contact)? context.getString(R.string.order_description_not_funded_local):context.getString(R.string.order_description_not_funded_local_no_action);  
                    return context.getString(R.string.order_description_not_funded_local);  
                }
                
            } else {

                if(contact.is_funded()) {
                    return context.getString(R.string.order_description_funded_local);
                } else {
                    return  context.getString(R.string.order_description_not_funded_local);
                }
            }
           
        } else if (isOnlineTrade(contact)) {
             
            if (contact.is_buying()) {
                return isMarkedPaid(contact)? context.getString(R.string.order_description_paid):context.getString(R.string.order_description_mark_paid);
            } else {
                return isMarkedPaid(contact)? context.getString(R.string.order_description_online_paid):context.getString(R.string.order_description_online_mark_paid);
            }
        }
        
        return null;
    }

    public static int getTradeActionButtonLabel(ContactItem contact)
    {
        if(isClosedTrade(contact) || isReleased(contact)) {
            return 0;
        }
        
        if (isLocalTrade(contact)) { // selling or buying locally with ad
            
            if (contact.is_selling()) { // ad to sell bitcoins locally 
                
                if(contact.is_funded() || isFunded(contact)) { // TODO is this available for local?
                    return R.string.button_release;
                } else {
                    return R.string.button_fund;
                }
            } 

            return R.string.button_cancel;

        } else if (isOnlineTrade(contact)) {   // handle online trade ads
            
            if (contact.is_buying()) { // ad to buy bitcoins  

               return isMarkedPaid(contact)? R.string.button_dispute: R.string.button_mark_paid;

            } else { // ad to sell bitcoins 

                return R.string.button_release;
            }
        }
        
        return 0;
    }

    public static boolean youAreAdvertiser(ContactItem contact)
    {
        if(contact.is_selling()) { // you are selling
            return contact.advertiser_username().equals(contact.seller_username());
        } else  {  // you are buying
            return contact.advertiser_username().equals(contact.buyer_username());
        }
    }
    
    public static boolean isActiveTrade(Contact contact)
    {
        return !(isCanceledTrade(contact) || isClosedTrade(contact));
    }

    public static boolean isCanceledTrade(ContactItem contact)
    {
        return contact.canceled_at() != null;
    }

    public static boolean isCanceledTrade(Contact contact)
    {
        return contact.canceled_at != null;
    }

    public static boolean isMarkedPaid(ContactItem contact)
    {
        return contact.payment_completed_at() != null;
    }
    
    public static boolean isFunded(ContactItem contact)
    {
        return contact.funded_at() != null;
    }

    public static boolean isReleased(ContactItem contact)
    {
        return contact.released_at() != null;
    }
    
    public static boolean isReleased(Contact contact)
    {
        return contact.released_at != null;
    }

    public static boolean isDisputed(ContactItem contact)
    {
        return contact.disputed_at() != null;
    }

    public static boolean isClosedTrade(ContactItem contact)
    {
        return contact.closed_at() != null;
    }

    public static boolean isClosedTrade(Contact contact)
    {
        return contact.closed_at != null;
    }

    public static boolean canDisputeTrade(ContactItem contact)
    {
        if(isClosedTrade(contact) && !isDisputed(contact)) {
            return contact.dispute_url() != null;
        }
            
        return false; 
    }

    public static boolean canCancelTrade(ContactItem contact)
    {
        if(!isClosedTrade(contact) && !isCanceledTrade(contact)) {

            return contact.cancel_url() != null;
        }
        
        return false;
    }

    public static boolean canReleaseTrade(ContactItem contact)
    {
        if(isClosedTrade(contact))
            return false;

        return true;
    }

    public static boolean canFundTrade(ContactItem contact)
    {
        if(isClosedTrade(contact))
            return false;

        return true;
    }

    public static boolean isLocalTrade(Contact contact)
    {
        TradeType tradeType = contact.advertisement.trade_type;
        return (tradeType == TradeType.LOCAL_BUY ||tradeType == TradeType.LOCAL_SELL);
    }
    
    public static boolean isLocalTrade(ContactItem contact)
    {
        TradeType tradeType = TradeType.valueOf(contact.advertisement_trade_type());
        return (tradeType == TradeType.LOCAL_BUY ||tradeType == TradeType.LOCAL_SELL);
    }
    
    public static boolean isLocalTrade(Advertisement advertisement)
    {
        return (advertisement.trade_type == TradeType.LOCAL_BUY || advertisement.trade_type == TradeType.LOCAL_SELL);
    }

    public static boolean isLocalTrade(AdvertisementItem advertisement)
    {
        return (advertisement.trade_type().equals(TradeType.LOCAL_BUY.name()) || advertisement.trade_type().equals(TradeType.LOCAL_SELL.name()));
    }

    public static boolean isAtm(AdvertisementItem advertisement)
    {
        return (!Strings.isBlank(advertisement.atm_model()));
    }

    public static boolean isOnlineTrade(ContactItem contact)
    {
        TradeType tradeType = TradeType.valueOf(contact.advertisement_trade_type());
        return (tradeType == TradeType.ONLINE_BUY || tradeType == TradeType.ONLINE_SELL);
    }

    public static boolean isOnlineTrade(Advertisement advertisement)
    {
        return (advertisement.trade_type == TradeType.ONLINE_BUY || advertisement.trade_type == TradeType.ONLINE_SELL);
    }

    public static boolean isSellTrade(AdvertisementItem advertisement)
    {
        return (advertisement.trade_type().equals(TradeType.ONLINE_SELL.name()) || advertisement.trade_type().equals(TradeType.LOCAL_SELL.name()));
    }

    public static boolean isBuyTrade(AdvertisementItem advertisement)
    {
        return (advertisement.trade_type().equals(TradeType.ONLINE_BUY.name()) || advertisement.trade_type().equals(TradeType.LOCAL_BUY.name()));
    }

    public static boolean isOnlineTrade(AdvertisementItem advertisement)
    {
        return (advertisement.trade_type().equals(TradeType.ONLINE_BUY.name()) || advertisement.trade_type().equals(TradeType.ONLINE_SELL.name()));
    }

    public static Method getMethodForAdvertisement(String online_provider, List<Method> methods)
    {
        for (Method m : methods) {
            if(online_provider.equals(m.code)) {
                return m;
            }
        }

        return null;
    }
    
    public static MethodItem getMethodForAdvertisement(Advertisement advertisement, List<MethodItem> methods)
    {
        for (MethodItem m : methods) {
            if(advertisement.online_provider.equals(m.code())) {
                return m;
            }
        }
        
        return null;
    }

    public static MethodItem getMethodForAdvertisement(AdvertisementItem advertisement, List<MethodItem> methods)
    {
        for (MethodItem m : methods) {
            if(advertisement.online_provider().equals(m.code())) {
                return m;
            }
        }

        return null;
    }

    /*public static Method getPaymentMethod(String code, List<Method> methods)
    {
        for (Method method : methods) {
            if(method.code.equals(code)) {
                return method;
            }
        }
        return null;
    }*/

    public static String getPaymentMethod(String code, List<MethodItem> methods)
    {
        for (MethodItem method : methods) {
            if(method.code().equals(code)) {
                if(Strings.isBlank(method.key()))
                    return code;
                
                return method.key();
            }
        }
        return code;
    }

    public static String getPaymentMethodFromItems(Advertisement advertisement, List<MethodItem> methodItems)
    {
        String paymentMethod = "";
        for (MethodItem method : methodItems) {
            if(method.code().equals(advertisement.online_provider)) {
                paymentMethod = getPaymentMethod(advertisement, method);
                break;
            }
        }
        return paymentMethod;
    }

    public static String getPaymentMethodFromItems(AdvertisementItem advertisement, List<MethodItem> methodItems)
    {
        if(methodItems == null || methodItems.isEmpty()) {
            return "";
        }
        
        String paymentMethod = "";
        
        for (MethodItem method : methodItems) {
            if(method.code().equals(advertisement.online_provider())) {
                paymentMethod = getPaymentMethod(advertisement, method);
                break;
            }
        }
        return paymentMethod;
    }

    /*public static String getPaymentMethod(Advertisement advertisement, List<Method> methods)
    {
        String paymentMethod = "";
        for (Method method : methods) {
            if(method.code.equals(advertisement.online_provider)) {
                paymentMethod = getPaymentMethod(advertisement, method);
                break;
            }
        }
        return paymentMethod;
    }*/

    public static String getPaymentMethod(Advertisement advertisement, List<MethodItem> methods)
    {
        String paymentMethod = "";
        for (MethodItem method : methods) {
            if(method.code().equals(advertisement.online_provider)) {
                paymentMethod = getPaymentMethod(advertisement, method);
                break;
            }
        }
        return paymentMethod;
    }

    public static String getPaymentMethodName(Advertisement advertisement, MethodItem method)
    {
        String paymentMethod = "Other";
        if(method != null && method.code().equals(advertisement.online_provider)) {
            paymentMethod = method.name();
        }

        return paymentMethod;
    }

    public static String getPaymentMethodName(AdvertisementItem advertisement, MethodItem method)
    {
        String paymentMethod = "Other";
        if(method != null && method.code().equals(advertisement.online_provider())) {
            paymentMethod = method.name();
        }

        return paymentMethod;
    }

    public static String getPaymentMethodName(Advertisement advertisement, Method method)
    {
        String paymentMethod = "Other";
        if(method != null && method.code.equals(advertisement.online_provider)) {
            paymentMethod = method.name;
        }

        return paymentMethod;
    }

    public static String getPaymentMethodName(String paymentMethod)
    {
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

    public static String getPaymentMethod(AdvertisementItem advertisement, MethodItem method)
    {
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

        if(!Strings.isBlank(advertisement.bank_name()) && advertisement.online_provider().equals("NATIONAL_BANK")) {
            return paymentMethod + " with " + advertisement.bank_name();
        }

        return paymentMethod;
    }
    
    public static String getPaymentMethod(Advertisement advertisement, MethodItem method)
    {
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

        if(!Strings.isBlank(advertisement.bank_name) && advertisement.online_provider.equals("NATIONAL_BANK")) {
            return paymentMethod + " with " + advertisement.bank_name;
        }

        return paymentMethod;
    }
    
    public static String getContactName(Contact contact)
    {
        if (contact.is_selling) {
            return contact.buyer.username;
        } else {
            return contact.seller.username;
        }
    }

    public static String getContactName(ContactSync contact)
    {
        if (contact.is_selling) {
            return contact.buyer_name;
        } else {
            return contact.seller_name;
        }
    }

    public static int determineLastSeenIcon(Contact contact)
    {
        String lasOnline = (contact.is_selling) ? contact.seller.last_online : contact.buyer.last_online;

        Date now = new Date();
        Date lastSeen = Dates.parseLocalDate(lasOnline);

        long diff = now.getTime() - lastSeen.getTime();

        if ((diff > 1800000) && (diff < 10800000)) {
            return R.drawable.last_seen_shortly;
        } else if (diff > 10800000) {
            return R.drawable.last_seen_long;
        }
        return R.drawable.last_seen_recently;
    }

    public static int determineLastSeenIcon(String lasOnline)
    {
        Date now = new Date();
        Date lastSeen = Dates.parseLastSeenDate(lasOnline);

        long diff = now.getTime() - lastSeen.getTime();

        if(( diff > 1800000) && (diff < 10800000)) {
            return R.drawable.last_seen_shortly;
        } else if (diff > 10800000) {
            return R.drawable.last_seen_long;
        }
        
        return R.drawable.last_seen_recently;
    }

    public static String getAddress(Address address)
    {
        String addressText = String.format(
                "%s, %s, %s",
                // If there's a street address, add it
                address.getMaxAddressLineIndex() > 0 ?
                        address.getAddressLine(0) : "",
                // Locality is usually a city
                address.getLocality(),
                // The country of the address
                address.getCountryName());

        return  addressText;
    }
    
    @Deprecated
    public static String getAddressShort(Address address)
    {
        String addressText = "";

        String addressLine = "0";
        String locality = "0";
        String country = "0";
        
        if(address.getMaxAddressLineIndex() > 0) {

            if(address.getAddressLine(0) != null)
                addressLine = address.getAddressLine(0);

            if(address.getLocality() != null)
                locality = address.getLocality();

            if (address.getCountryName() != null)
                country = address.getCountryName();

            addressText = String.format(
                    "%s, %s, %s",

                    // If there's a street address, add it
                    addressLine,

                    // Locality is usually a city
                    locality,

                    // The country of the address
                    country
            );
        } 
        
        addressText = addressText.replace("0,", "");
        
        return addressText;
    }

    public static String [] parseUserString(String value)
    {
        String [] nameSplit;
        if(!value.contains(" ")) {
            ArrayList<String> stringArrayList = new ArrayList<String>();
            stringArrayList.add(value);
            nameSplit = stringArrayList.toArray(new String[stringArrayList.size()]);
            return  nameSplit;
        }

        // strip out any parenthesis and split on spacing?
        value = value.replaceAll("(\\()", "");
        value = value.replaceAll("(\\))", "");
        value = value.replaceAll("(\\;)", "");
        nameSplit = value.split(" ");
        return nameSplit;
    }

    public static String parsePaymentService(String value)
    {
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

    public static String parsePaymentServiceTitle(String value)
    {
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
    
    public static String kilometersToMiles(String km)
    {
        double mi = Doubles.convertToDouble(km) * .62137;
        DecimalFormat precision = new DecimalFormat("0.00");
        return precision.format(mi);
    }

    public static String convertCurrencyAmount(String value)
    {
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
}
