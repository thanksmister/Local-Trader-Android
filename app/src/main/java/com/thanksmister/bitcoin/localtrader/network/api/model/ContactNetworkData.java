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
