/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.buxiaohui.fastclick

import org.gradle.api.Project

class AndroidJarPathGro {

    static String getPath(Project project) {
        return project.android.bootClasspath[0].toString()
    }
}
