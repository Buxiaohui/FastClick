/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.bxh.fastclick

import com.squareup.javapoet.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import javax.lang.model.element.Modifier
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

class GeneratorAnnotationTask extends DefaultTask {

    @Override
    String getName() {
        return super.getName()
    }

    @TaskAction
    def action() {
        long start = System.currentTimeMillis()
        TypeSpec classTypeSpec = TypeSpec
                .annotationBuilder("FC")
                .addAnnotation(AnnotationSpec.builder(Retention.class)
                        .addMember("value", '$T.$L', RetentionPolicy.class, "RUNTIME")
                        .build())
                .addAnnotation(AnnotationSpec.builder(Target.class)
                        .addMember("value", '$T.$L', ElementType.class, "METHOD")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.methodBuilder("tag")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .defaultValue('$S', "")
                        .returns(String.class)
                        .build())
                .addMethod(MethodSpec.methodBuilder("timeInterval")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .defaultValue('$L', -1)
                        .returns(TypeName.LONG)
                        .build())
                .build()
        JavaFile javaFile = JavaFile.builder("com.buxiaohui.fastclick",classTypeSpec).build()

        // 将java写入到文件夹下
        LogUtils.logD("[project.buildDir is] $project.buildDir")
        LogUtils.logD("[project.projectDir is] $project.projectDir")
        File file = new File(project.buildDir, "generated/source/container")
        if (!file.exists()) {
            file.mkdirs()
        }
        javaFile.writeTo(file)
        LogUtils.logI("[write to] ${file.absolutePath} -> ${javaFile.packageName}.${classTypeSpec.name}")
        LogUtils.logI("[GeneratorAnnotationTask cost] ${System.currentTimeMillis() - start} ms")
    }

}
