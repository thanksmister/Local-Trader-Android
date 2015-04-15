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

package com.thanksmister.bitcoin.localtrader.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.location.Address;
import android.os.Build;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactSync;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;


public class TradeUtils
{
    public static String getContactDescription(Contact contact, Context context)
    {
        Timber.d("Closed " + isClosedTrade(contact));

        if(isCanceledTrade(contact)) {

            Timber.d("Canceled " + isCanceledTrade(contact));
            
            return isLocalTrade(contact)? context.getString(R.string.order_description_cancel_local):context.getString(R.string.order_description_cancel); 
        
        } else if (isReleased(contact)) {

            Timber.d("Released " + isReleased(contact));
            
            return context.getString(R.string.order_description_released);
            
        } else if (isDisputed(contact)) {

            Timber.d("Disputed " + isDisputed(contact));
            
            return context.getString(R.string.order_description_disputed);
            
        } else if (isLocalTrade(contact)) {

           /* if(contact.youAreSelling()) { 
                if(Utils.convertToDouble(wallet.getBalance()) < Utils.convertToDouble(contact.getAmount_btc())) {
                    return context.getString(R.string.order_description_no_funds);
                }
            }*/

            Timber.d("Is Advertiser " + youAreAdvertiser(contact));
            Timber.d("Is Selling " + contact.is_selling);
            Timber.d("Is Funded " + contact.is_funded);
            
            if(youAreAdvertiser(contact) && contact.is_selling) {

                if(contact.is_funded) {
                    //return canFundTrade(contact)? context.getString(R.string.order_description_funded_local):context.getString(R.string.order_description_funded_local_no_action);
                    return  context.getString(R.string.order_description_funded_local);
                } else {
                    //return canReleaseTrade(contact)? context.getString(R.string.order_description_not_funded_local):context.getString(R.string.order_description_not_funded_local_no_action);  
                    return context.getString(R.string.order_description_not_funded_local);  
                }
                
            } else {

                if(contact.is_funded) {
                    return context.getString(R.string.order_description_funded_local);
                } else {
                    return  context.getString(R.string.order_description_not_funded_local);
                }
            }
           
        } else if (isOnlineTrade(contact)) {
             
            if (contact.is_buying) {
                return isMarkedPaid(contact)? context.getString(R.string.order_description_paid):context.getString(R.string.order_description_mark_paid);
            } else {
                return isMarkedPaid(contact)? context.getString(R.string.order_description_online_paid):context.getString(R.string.order_description_online_mark_paid);
            }
        }
        
        return null;
    }

    public static int getTradeActionButtonLabel(Contact contact)
    {
        if(isClosedTrade(contact) || isReleased(contact)) {
            return 0;
        }
        
        if (isLocalTrade(contact)) { // selling or buying locally with ad
            
            if (contact.is_selling) { // ad to sell bitcoins locally 
                
                if(contact.is_funded || isFunded(contact)) { // TODO is this available for local?
                    return R.string.button_release;
                } else {
                    return R.string.button_fund;
                }
            } 

            return R.string.button_cancel;

        } else if (isOnlineTrade(contact)) {   // handle online trade ads
            
            if (contact.is_buying) { // ad to buy bitcoins  

               return isMarkedPaid(contact)? R.string.button_dispute: R.string.button_mark_paid;

            } else { // ad to sell bitcoins 

                return R.string.button_release;
            }
        }
        
        return 0;
    }

    public static boolean youAreAdvertiser(Contact contact)
    {
        if(contact.is_selling) { // you are selling
            return contact.advertisement.advertiser.username.equals(contact.seller.username);
        } else  {  // you are buying
            return contact.advertisement.advertiser.username.equals(contact.buyer.username);
        }
    }

    public static boolean isCanceledTrade(Contact contact)
    {
        return contact.canceled_at != null;
    }

    public static boolean isMarkedPaid(Contact contact)
    {
        return contact.payment_completed_at != null;
    }
    
    public static boolean isFunded(Contact contact)
    {
        return contact.funded_at != null;
    }

    public static boolean isReleased(Contact contact)
    {
        return contact.released_at != null;
    }

    public static boolean isDisputed(Contact contact)
    {
        return contact.disputed_at != null;
    }

    public static boolean isClosedTrade(Contact contact)
    {
        return contact.closed_at != null;
    }

    public static boolean canDisputeTrade(Contact contact)
    {
        if(isClosedTrade(contact) && !isDisputed(contact)) {
            return contact.actions.dispute_url != null;
        }
            
        return false; 
    }

    public static boolean canCancelTrade(Contact contact)
    {
        if(!isClosedTrade(contact) && !isCanceledTrade(contact)) {

            return contact.actions.cancel_url != null;
        }
        
        return false;
    }

    public static boolean canReleaseTrade(Contact contact)
    {
        if(isClosedTrade(contact))
            return false;

        return true;
    }

    public static boolean canFundTrade(Contact contact)
    {
        if(isClosedTrade(contact))
            return false;

        return true;
    }
    
    public static boolean isLocalTrade(Contact contact)
    {
        return (contact.advertisement.trade_type == TradeType.LOCAL_BUY || contact.advertisement.trade_type == TradeType.LOCAL_SELL);
    }

    public static boolean isLocalTrade(ContactSync contact)
    {
        return (contact.advertisement_trade_type.equals(TradeType.LOCAL_BUY.name()) || contact.advertisement_trade_type.equals(TradeType.LOCAL_SELL.name()));
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

    public static boolean isOnlineTrade(Contact contact)
    {
        return (contact.advertisement.trade_type == TradeType.ONLINE_BUY || contact.advertisement.trade_type == TradeType.ONLINE_SELL);
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
    
    public static Method getMethodForAdvertisement(Advertisement advertisement, List<Method> methods)
    {
        for (Method m : methods) {
            if(advertisement.online_provider.equals(m.code)) {
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

    public static Method getPaymentMethod(String code, List<Method> methods)
    {
        for (Method method : methods) {
            if(method.code.equals(code)) {
                return method;
            }
        }
        return null;
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
        String paymentMethod = "";
        for (MethodItem method : methodItems) {
            if(method.code().equals(advertisement.online_provider())) {
                paymentMethod = getPaymentMethod(advertisement, method);
                break;
            }
        }
        return paymentMethod;
    }

    public static String getPaymentMethod(Advertisement advertisement, List<Method> methods)
    {
        String paymentMethod = "";
        for (Method method : methods) {
            if(method.code.equals(advertisement.online_provider)) {
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

    public static String getPaymentMethod(AdvertisementItem advertisement, MethodItem method)
    {
        String paymentMethod = "Online";
        if (method != null && method.code().equals(advertisement.online_provider())) {
            if (method.code().equals("NATIONAL_BANK")) {
                if (Strings.isBlank(method.countryName()))
                    return "national bank transfer";

                return "bank transfer in " + method.countryName();
            } else if (method.code().equals("CASH_DEPOSIT")) {
                if (Strings.isBlank(method.countryName()))
                    return "cash deposit";

                return "cash deposit in " + method.countryName();
            } else if (method.code().equals("SPECIFIC_BANK")) {
                if (Strings.isBlank(method.countryName()))
                    return "bank transfer";

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
                    return "national bank transfer";

                return "bank transfer in " + method.countryName();
            } else if (method.code().equals("CASH_DEPOSIT")) {
                if (Strings.isBlank(method.countryName()))
                    return "cash deposit";

                return "cash deposit in " + method.countryName();
            } else if (method.code().equals("SPECIFIC_BANK")) {
                if (Strings.isBlank(method.countryName()))
                    return "bank transfer";

                return "bank transfer in " + method.countryName();
            }

            paymentMethod = method.name();
        }

        if(!Strings.isBlank(advertisement.bank_name) && advertisement.online_provider.equals("NATIONAL_BANK")) {
            return paymentMethod + " with " + advertisement.bank_name;
        }

        return paymentMethod;
    }

    @Deprecated
    public static String getPaymentMethod(Advertisement advertisement, Method method)
    {
        String paymentMethod = "Online";
        if(method != null && method.code.equals(advertisement.online_provider)) {
            if(method.code.equals("NATIONAL_BANK")) {
                if(method.countryName == null)
                    return "National Bank transfer";
                
                return  "Bank transfer in " + method.countryName;
            } else if (method.code.equals("CASH_DEPOSIT")) {
                if(method.countryName == null)
                    return "Cash deposit";
                
                return "Cash deposit in " + method.countryName;
            } else if (method.code.equals("SPECIFIC_BANK")) {
                if(method.countryName == null)
                    return "Bank transfer";

                return "Bank transfer in " + method.countryName;
            }
            
            paymentMethod = method.name;
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

    public static String getAddressShort(Address address)
    {
        String addressText = String.format(
                "%s, %s, %s",
                // If there's a street address, add it
                address.getMaxAddressLineIndex() > 0 ?
                        address.getAddressLine(0) : "",
                // Locality is usually a city
                address.getLocality(),
                // The country of the address
                address.getCountryName()
        );

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
        String titleCaseValue = sb.toString();

        return titleCaseValue;
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

    public static boolean hasFroyo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isTablet(Context context)
    {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
