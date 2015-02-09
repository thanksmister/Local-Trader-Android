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
package com.thanksmister.bitcoin.localtrader.data.api.model;

public class User
{
    public String username;
    public String age_text;
    public int trading_partners_count;
    public int feedbacks_unconfirmed_count;
    public String trade_volume_text;
    public boolean has_common_trades;
    public String confirmed_trade_count_text;
    public int blocked_count;
    public int feedback_count;
    public String feedback_score;
    public int trusted_count;
    public String url;
    public String created_at;
}
