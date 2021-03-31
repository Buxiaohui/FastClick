/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.buxiaohui.fastclick

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
        println("[fc-plugin-apply]")
        if (project != null) {
            project.afterEvaluate {
                println("[beforeEvaluate]")
                project.plugins.each {
                    println("[project.plugins] ${it.getClass()}")
                }
                DefaultDomainObjectCollection<BaseVariant> variants
                if (project.plugins.hasPlugin(AppPlugin.class)) {
                    println("[has AppPlugin]")
                    variants = project.android.applicationVariants
                } else if (project.plugins.hasPlugin(LibraryPlugin.class)) {
                    println("[has LibraryPlugin]")
                    variants = project.android.libraryVariants
                } else {
                    println("[no AppPlugin ,no LibraryPlugin]")
                    return
                }

                if(variants == null){
                    println("[variants is null")
                    return
                }
                variants.each {
                    println("[variants] $it")
                }
//                variants.all { variant ->
//                    println("[variant.name is] $variant.name")
//                    def task = project.tasks.create("create${variant.name.capitalize()}GeneratorTask",
//                            GeneratorTask.class)
//                    // 注册生成java类的task，指定生成地址，需要和task中写入java的地址一致
//                    variant.registerJavaGeneratingTask(task,new File(project.buildDir, "generated/source/container"))
//                }
            }
            AppExtension appExtension = project.extensions.getByType(AppExtension.class)
            FCTransform fcTransform = new FCTransform(project)
            appExtension.registerTransform(fcTransform)
            project.extensions.create("FCConfig", ConfigExtension.class)
            project.afterEvaluate {
                fcTransform.configExtension = project.FCConfig
            }
        }
    }
}