package com.aiproject.smartcampus.commons.ratelimiter;

//滑动窗口设计


import java.util.concurrent.TimeUnit;

public interface RateLimiter {

     boolean isAllow();



}
