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

package com.thanksmister.bitcoin.localtrader.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.trello.rxlifecycle.ActivityEvent;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static android.view.View.GONE;

public class MessageActivity extends BaseActivity {
    public static final String EXTRA_ID = "com.thanksmister.extras.EXTRA_ID";
    public static final String EXTRA_NAME = "com.thanksmister.extras.EXTRA_NAME";
    public static final String EXTRA_MESSAGE = "com.thanksmister.extras.EXTRA_MESSAGE";

    public static final int REQUEST_MESSAGE_CODE = 760;
    public static final int RESULT_MESSAGE_SENT = 765;
    public static final int RESULT_MESSAGE_CANCELED = 768;
    public static final int GALLERY_INTENT_CALLED = 112;
    public static final int GALLERY_KITKAT_INTENT_CALLED = 113;

    @Inject
    DataService dataService;

    @BindView((R.id.messageTitle))
    TextView messageTitle;

    @BindView((R.id.editMessageText))
    EditText messageText;

    private String contactId;
    private String contactName;

    @OnClick(R.id.messageButton)
    public void sendMessageButton() {
        validateMessage();
    }

    @BindView((R.id.attachmentLayout))
    View attachmentLayout;

    @BindView((R.id.attachButton))
    View attachButton;

    @Nullable
    @BindView((R.id.attachmentName))
    TextView attachmentName;

    @BindView((R.id.removeAttachmentButton))
    ImageButton removeAttachmentButton;

    @OnClick(R.id.attachButton)
    public void attacheButtonClicked() {
        attachFile();
    }

    @OnClick(R.id.removeAttachmentButton)
    public void removeAttachmentButtonClicked() {
        removeAttachment();
    }

    private Subscription subscription = Subscriptions.empty();
    private String message;
    private Uri mUri;
    private String mFileName;

    public static Intent createStartIntent(@NonNull Context context, @NonNull String contactId, @NonNull String contactName) {
        Intent intent = new Intent(context, MessageActivity.class);
        intent.putExtra(EXTRA_ID, contactId);
        intent.putExtra(EXTRA_NAME, contactName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_message);

        ButterKnife.bind(this);

        if (savedInstanceState == null) {
            contactId = getIntent().getStringExtra(EXTRA_ID);
            contactName = getIntent().getStringExtra(EXTRA_NAME);
            message = getIntent().getStringExtra(EXTRA_MESSAGE);
        } else {
            contactId = savedInstanceState.getString(EXTRA_ID);
            contactName = savedInstanceState.getString(EXTRA_NAME);
            message = savedInstanceState.getString(EXTRA_MESSAGE);
        }

        if (!TextUtils.isEmpty(message)) {
            messageText.setText(message);
        }

        messageTitle.setText(getString(R.string.title_message_to, contactName));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        subscription.unsubscribe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ID, contactId);
        outState.putString(EXTRA_NAME, contactName);
        outState.putString(EXTRA_MESSAGE, message);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_MESSAGE_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GALLERY_INTENT_CALLED:
                if (resultCode != RESULT_CANCELED) {
                    mUri = data.getData();
                    mFileName = getDocumentName(mUri);
                    attachButton.setVisibility(GONE);
                    attachmentLayout.setVisibility(View.VISIBLE);
                    attachmentName.setText(mFileName);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void validateMessage() {
        message = messageText.getText().toString();

        if (Strings.isBlank(message) && mUri == null) {
            toast(getString(R.string.toast_message_blank));
            return;
        }

        if (mUri != null) {
            showProgressDialog(new ProgressDialogEvent(getString(R.string.dialog_send_message)));
            getBitmapFromStream(mUri);
        } else {
            postMessage(message);
        }
    }

    private void handleMessageSent() {
        messageText.setText("");

        // hide keyboard and notify
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            Timber.e("Error closing keyboard");
        }

        setResult(RESULT_MESSAGE_SENT);
        finish();
    }

    private void postMessage(String message) {
        showProgressDialog(new ProgressDialogEvent(getString(R.string.dialog_send_message)));

        dataService.postMessage(contactId, message)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Post message subscription safely unsubscribed");
                    }
                })
                .compose(this.<JSONObject>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                    @Override
                    public void call(JSONObject jsonObject) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                handleMessageSent();
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                toast(R.string.toast_error_message);
                            }
                        });
                    }
                });
    }

    private void postMessageWithAttachment(final String message, final File file) {
        dataService.postMessageWithAttachment(contactId, message, file)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Post message subscription safely unsubscribed");
                    }
                })
                .compose(this.<JSONObject>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                    @Override
                    public void call(JSONObject jsonObject) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                handleMessageSent();
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                toast(R.string.toast_error_message);
                            }
                        });
                    }
                });
    }

    private void removeAttachment() {
        attachButton.setVisibility(View.VISIBLE);
        attachmentLayout.setVisibility(GONE);
        mUri = null;
        mFileName = null;
    }

    private void attachFile() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            try {
                startActivityForResult(Intent.createChooser(intent, getString(R.string.chooser_select_image)), GALLERY_INTENT_CALLED);
            } catch (android.content.ActivityNotFoundException ex) {
                toast(getString(R.string.toast_no_file_manager));
            }
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            try {
                startActivityForResult(Intent.createChooser(intent, getString(R.string.chooser_select_image)), GALLERY_INTENT_CALLED);
            } catch (android.content.ActivityNotFoundException ex) {
                toast(getString(R.string.toast_no_file_manager));
            }
        }
    }

    public String getDocumentName(Uri uri) {
        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        try {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {
                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                return displayName;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return "";
    }

    private void getBitmapFromStream(Uri uri) {
        BitmapGenerationTask bitmapGenerationTask = new BitmapGenerationTask(MessageActivity.this);
        bitmapGenerationTask.setOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(File file) {
                postMessageWithAttachment(message, file);
            }
        });
        bitmapGenerationTask.setOnExceptionListener(new OnExceptionListener() {
            @Override
            public void onException(Exception exception) {
                hideProgressDialog();
                toast(getString(R.string.toast_file_no_upload));
            }
        });

        bitmapGenerationTask.execute(uri);
    }

    private interface OnCompleteListener {
        void onComplete(File result);
    }

    private interface OnExceptionListener {
        void onException(Exception exception);
    }

    private class BitmapGenerationTask extends AsyncTask<Uri, Void, File> {
        private Context context;
        private Exception exception;
        private OnCompleteListener onCompleteListener;
        private OnExceptionListener onExceptionListener;

        BitmapGenerationTask(Context context) {
            super();
            this.context = context;
        }

        void setOnCompleteListener(OnCompleteListener onCompleteListener) {
            this.onCompleteListener = onCompleteListener;
        }

        void setOnExceptionListener(OnExceptionListener exceptionListener) {
            this.onExceptionListener = exceptionListener;
        }

        private File doBitmapConversion(Uri... uris) throws Exception {
            if (uris.length != 1) {
                throw new Exception("Wrong number of uris");
            }

            Uri uri = uris[0];
            Bitmap bitmap = null;
            try {
                BitmapFactory.Options outDimens = getBitmapDimensions(context, uri);
                int sampleSize = calculateSampleSize(outDimens.outWidth, outDimens.outHeight, 1200, 1200);
                bitmap = downSampleBitmap(context, uri, sampleSize);

                File file = new File(context.getCacheDir(), Strings.removeExtension(mFileName));
                file.createNewFile();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                byte[] bitmapdata = bos.toByteArray();

                //write the bytes in file
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();

                return file;

            } catch (Exception e) {
                Timber.e("File Exception: " + e.getMessage());
                throw new Exception(e.getMessage());
            }
        }

        @Override
        protected File doInBackground(Uri... uris) {
            if (isCancelled()) {
                return null;
            }

            try {
                return doBitmapConversion(uris);
            } catch (Exception e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(File result) {
            super.onPostExecute(result);
            if (isCancelled()) {
                return;
            }
            if (exception != null && onExceptionListener != null) {
                onExceptionListener.onException(exception);
            } else if (onCompleteListener != null) {
                onCompleteListener.onComplete(result);
            }
        }
    }

    private static BitmapFactory.Options getBitmapDimensions(Context context, Uri uri) throws FileNotFoundException, IOException {
        BitmapFactory.Options outDimens = new BitmapFactory.Options();
        outDimens.inJustDecodeBounds = true; // the decoder will return null (no bitmap)

        InputStream is = context.getContentResolver().openInputStream(uri);
        // if Options requested only the size will be returned
        BitmapFactory.decodeStream(is, null, outDimens);
        is.close();

        return outDimens;
    }

    private static int calculateSampleSize(int width, int height, int targetWidth, int targetHeight) {
        int inSampleSize = 1;

        if (height > targetHeight || width > targetWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height / (float) targetHeight);
            final int widthRatio = Math.round((float) width / (float) targetWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private static Bitmap downSampleBitmap(Context context, Uri uri, int sampleSize) throws FileNotFoundException, IOException {
        Bitmap resizedBitmap;
        BitmapFactory.Options outBitmap = new BitmapFactory.Options();
        outBitmap.inJustDecodeBounds = false; // the decoder will return a bitmap
        outBitmap.inSampleSize = sampleSize;

        InputStream is = context.getContentResolver().openInputStream(uri);
        resizedBitmap = BitmapFactory.decodeStream(is, null, outBitmap);

        if (is != null) {
            is.close();
        }

        return resizedBitmap;
    }
}