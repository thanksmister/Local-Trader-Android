package com.thanksmister.bitcoin.localtrader.network.api.model;

/**
 * Created by Michael Ritchie on 4/10/18.
 */
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Error {

    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("errors")
    @Expose
    private Errors errors;
    @SerializedName("error_code")
    @Expose
    private Integer errorCode;
    @SerializedName("error_lists")
    @Expose
    private ErrorLists errorLists;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Errors getErrors() {
        return errors;
    }

    public void setErrors(Errors errors) {
        this.errors = errors;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorLists getErrorLists() {
        return errorLists;
    }

    public void setErrorLists(ErrorLists errorLists) {
        this.errorLists = errorLists;
    }

}