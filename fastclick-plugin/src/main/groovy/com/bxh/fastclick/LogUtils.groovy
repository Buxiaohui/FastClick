/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.bxh.fastclick

class LogUtils {
    static boolean E = true
    static boolean I = false
    static boolean D = false

    static void logE(String log) {
        if (!E) {
            return
        }
        println("E:${log}")
    }

    static void logI(String log) {
        if (!I) {
            return
        }
        println("I:${log}")
    }

    static void logD(String log) {
        if (!D) {
            return
        }
        println("D:${log}")
    }
}
