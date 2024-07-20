package com.scaler.userservice.models;

public enum SessionStatus {
    //Session is active
    ACTIVE, //0
    //Session is ended
    ENDED, //1
    //Session is logged out due to timeout
    LOGGED_OUT, //2
    //Session is Invalid
    INVALID

}
