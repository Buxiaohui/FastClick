/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.buxiaohui.fastclick;

import java.util.concurrent.ConcurrentHashMap;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

/**
 * @author: buxiaohui
 * @date: 2020/12/8
 */
public class FastClickUtils {
    private static final String DEFAULT_TAG = "fast_click_default_tag";
    private static final long DEFAULT_TIME_INTERVAL = 800;

    private static final Object lock = new Object();
    private static final FastClickLruCache fastClickTimeMap = new FastClickLruCache(64);
    private static final ConcurrentHashMap<String, Long> backupMap = new ConcurrentHashMap<>();

    public static boolean isFastClick() {
        return isFastClick(DEFAULT_TAG, DEFAULT_TIME_INTERVAL);
    }

    public static boolean isFastClick(long timeInterval) {
        return isFastClick(DEFAULT_TAG, timeInterval);
    }

    public static boolean isFastClick(String tag) {
        return isFastClick(tag, DEFAULT_TIME_INTERVAL);
    }

    public static boolean isFastClick(String tag, long timeInterval) {
        if (TextUtils.isEmpty(tag) && timeInterval <= 0) {
            return isFastClick();
        }
        if (!TextUtils.isEmpty(tag) && timeInterval <= 0) {
            return isFastClick(tag);
        }
        if (!TextUtils.isEmpty(tag) && timeInterval > 0) {
            return isFastClickReal(tag, timeInterval);
        }
        if (TextUtils.isEmpty(tag) && timeInterval > 0) {
            return isFastClick(timeInterval);
        }
        return false;
    }

    private static boolean isFastClickReal(String tag, long timeInterval) {
        long curTime = SystemClock.elapsedRealtime();
        synchronized(lock) {
            Long lastTime = fastClickTimeMap.get(tag);
            if (lastTime == null) {
                lastTime = backupMap.remove(tag);
            }
            boolean isFastClick = lastTime != null && (curTime - lastTime <= timeInterval);
            if (!isFastClick) {
                fastClickTimeMap.put(tag, curTime);
            }
            System.out.println("isFastClick");
            return isFastClick;
        }
    }

    static class FastClickLruCache extends LruCache<String, Long> {

        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public FastClickLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Long oldValue, Long newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
            if (!evicted) {
                return;
            }

            if (oldValue != null && key != null) {
                long curTime = SystemClock.elapsedRealtime();
                if (curTime - oldValue > 20000) {
                    backupMap.clear();
                } else {
                    backupMap.put(key, oldValue);
                }
            }
        }
    }

    public static void print() {
        Log.e(DEFAULT_TAG, "hahaha");
    }
}
