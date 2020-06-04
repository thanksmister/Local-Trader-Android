/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */
package com.thanksmister.bitcoin.localtrader.network.api.model;

import java.util.ArrayList;
import java.util.List;

public class ContactNetworkData {
    
    private Contact contact;
    private List<Message> messages;
    
    public void setMessages(List<Message> messagesList) {
        messages = messagesList;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<Message> getMessagesList() {
        if (messages == null) {
            return new ArrayList<>();
        }
        return messages;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }
}
