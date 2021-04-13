/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.bxh.fastclick

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectCollection

class FCPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        ConfigExtension configExtension
        if (project != null) {
            AppExtension appExtension = project.extensions.getByType(AppExtension.class)
            project.extensions.create("FCConfig", ConfigExtension.class)
            configExtension = project.FCConfig
            if (configExtension != null) {
                LogUtils.D = configExtension.logD
                LogUtils.I = configExtension.logI
                LogUtils.E = configExtension.logE
            }
            LogUtils.logD("[D -> enable]")
            LogUtils.logI("[I -> enable]")
            LogUtils.logE("[E -> enable]")
            if (configExtension.getEnableGen()) {
                project.afterEvaluate {
                    LogUtils.logD("[beforeEvaluate]")
                    project.plugins.each {
                        LogUtils.logD("[project.plugins] ${it.getClass()}")
                    }
                    DefaultDomainObjectCollection<BaseVariant> variants
                    if (project.plugins.hasPlugin(AppPlugin.class)) {
                        LogUtils.logD("[has AppPlugin]")
                        variants = project.android.applicationVariants
                    } else if (project.plugins.hasPlugin(LibraryPlugin.class)) {
                        LogUtils.logD("[has LibraryPlugin]")
                        variants = project.android.libraryVariants
                    } else {
                        LogUtils.logD("[no AppPlugin ,no LibraryPlugin]")
                        return
                    }

                    if (variants == null) {
                        LogUtils.logD("[variants is null")
                        return
                    }
                    variants.each {
                        LogUtils.logD("[variants] $it")
                    }

                    variants.all { variant ->
                        LogUtils.logD("[variant.name is] $variant.name")
                        def generatorToolClassTask = project.tasks.create("create${variant.name.capitalize()}GeneratorToolClassTask", GeneratorToolClassTask.class)
                        // 注册生成java类的task，指定生成地址，需要和task中写入java的地址一致
                        variant.registerJavaGeneratingTask(generatorToolClassTask, new File(project.buildDir, "generated/source/container"))

                        def generatorAnnotationTask = project.tasks.create("create${variant.name.capitalize()}GeneratorAnnotationTask", GeneratorAnnotationTask.class)
                        // 注册生成java类的task，指定生成地址，需要和task中写入java的地址一致
                        variant.registerJavaGeneratingTask(generatorAnnotationTask, new File(project.buildDir, "generated/source/container"))
                    }
                }
            }
            if (configExtension.getEnableInsert()) {
                FCTransform fcTransform = new FCTransform(project)
                project.afterEvaluate {
                    fcTransform.configExtension = project.FCConfig
                }
                appExtension.registerTransform(fcTransform)
            }
        }
    }
}