package com.zinoviev.flowManager.conversion.dto;

public record SubscriptionCheckResultDto  (

    boolean allowed,

    String message

) {}
