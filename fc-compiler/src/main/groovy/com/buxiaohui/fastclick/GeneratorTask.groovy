/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.buxiaohui.fastclick

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import com.squareup.javapoet.ParameterizedTypeName

import javax.lang.model.element.Modifier
import java.lang.reflect.Field

class GeneratorTask extends DefaultTask {

    @Override
    String getName() {
        return super.getName()
    }

    @TaskAction
    def action() {
        // 生成java类
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("TestJava")
        classBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)

        MethodSpec isFastClick1 = MethodSpec.methodBuilder("isFastClick")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String.class, "tag")
                .addParameter(TypeName.LONG, "timeInterval")
                .beginControlFlow("if (android.text.TextUtils.isEmpty(tag) && timeInterval <= 0)")
                .addStatement("return isFastClick()")
                .endControlFlow()
                .beginControlFlow("if (!android.text.TextUtils.isEmpty(tag) && timeInterval <= 0)")
                .addStatement("return isFastClick(tag)")
                .endControlFlow()
                .beginControlFlow("if (!android.text.TextUtils.isEmpty(tag) && timeInterval > 0)")
                .addStatement("return isFastClickReal(tag, timeInterval)")
                .endControlFlow()
                .beginControlFlow("if (android.text.TextUtils.isEmpty(tag) && timeInterval > 0)")
                .addStatement(" return isFastClick(timeInterval)")
                .endControlFlow()
                .addStatement(" return false")
                .returns(TypeName.BOOLEAN)
                .build()

        MethodSpec isFastClick2 = MethodSpec.methodBuilder("isFastClick")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return isFastClick(DEFAULT_TAG, DEFAULT_TIME_INTERVAL)")
                .returns(TypeName.BOOLEAN)
                .build()

        MethodSpec isFastClick3 = MethodSpec.methodBuilder("isFastClick")
                .addParameter(TypeName.LONG, "timeInterval")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return isFastClick(DEFAULT_TAG, timeInterval)")
                .returns(TypeName.BOOLEAN)
                .build()

        MethodSpec isFastClick4 = MethodSpec.methodBuilder("isFastClick")
                .addParameter(String.class, "tag")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return isFastClick(tag, DEFAULT_TIME_INTERVAL)")
                .returns(TypeName.BOOLEAN)
                .build()

        MethodSpec isFastClickReal = MethodSpec.methodBuilder("isFastClickReal")
                .addParameter(String.class, "tag")
                .addParameter(TypeName.LONG, "timeInterval")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addStatement("long curTime = android.os.SystemClock.elapsedRealtime()")
                .beginControlFlow("synchronized(lock)")
                .addStatement("Long lastTime = fastClickTimeMap.get(tag)")
                .beginControlFlow("if (lastTime == null)")
                .addStatement("lastTime = backupMap.remove(tag)")
                .endControlFlow()
                .addStatement("boolean isFastClick = lastTime != null && (curTime - lastTime <= timeInterval)")
                .beginControlFlow("if (!isFastClick)")
                .addStatement("fastClickTimeMap.put(tag, curTime)")
                .endControlFlow()
                .addStatement("return isFastClick")
                .endControlFlow()
                .returns(TypeName.BOOLEAN)
                .build()

        classBuilder.addMethod(isFastClick1)
                .addMethod(isFastClick2)
                .addMethod(isFastClick3)
                .addMethod(isFastClick4)
                .addMethod(isFastClickReal)

        // DEFAULT_TAG
        FieldSpec.Builder defaultTagFieldBuilder = FieldSpec.builder(String.class, "DEFAULT_TAG")
        defaultTagFieldBuilder.initializer('$S', "fast_click_default_tag")
                .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE)
        classBuilder.addField(defaultTagFieldBuilder.build())
        // DEFAULT_TIME_INTERVAL
        FieldSpec.Builder defaultIntervalFieldBuilder = FieldSpec.builder(TypeName.LONG, "DEFAULT_TIME_INTERVAL")
        defaultIntervalFieldBuilder.initializer('$L', "800")
                .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE)
        classBuilder.addField(defaultIntervalFieldBuilder.build())
        // lock
        FieldSpec.Builder lockFieldBuilder = FieldSpec.builder(Object.class, "lock")
        lockFieldBuilder.initializer('new $T()', Object.class)
                .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE)
        classBuilder.addField(lockFieldBuilder.build())
        // fastClickTimeMap TODO
        ClassName.get("com.buxiaohui.fastclick", "FastClickLruCache")
        FieldSpec.Builder fastClickTimeMapFieldBuilder = FieldSpec.builder(FastClickLruCache.class, "lock")
        fastClickTimeMapFieldBuilder.initializer("$T", Object.class)
                .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE)
        classBuilder.addField(fastClickTimeMapFieldBuilder.build())
        // backupMap
        ClassName concurrentHashMapName = ClassName.get("java.util.concurrent", "ConcurrentHashMap")
        FieldSpec backupMapFieldSpec = FieldSpec.builder(ParameterizedTypeName.get(
                concurrentHashMapName, TypeName.get(String.class), TypeName.get(Long.class)), "backupMap")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer('new $T()', concurrentHashMapName)
                .build()
        classBuilder.addField(backupMapFieldSpec)

        TypeSpec.Builder innerStaticClassBuilder = TypeSpec.classBuilder("FastClickLruCache")
        innerStaticClassBuilder.addModifiers(Modifier.STATIC,Modifier.PUBLIC)
        classBuilder.addType(innerStaticClassTypeSpec)
        JavaFile javaFile = JavaFile.builder("com.buxiaohui.fastclick", classBuilder.build()).build()

        // 将java写入到文件夹下
        println("[project.buildDir is] $project.buildDir")
        println("[project.projectDir is] $project.projectDir")
        File file = new File(project.buildDir, "generated/source/container")
        if (!file.exists()) {
            file.mkdirs()
        }
        javaFile.writeTo(file)
        println("[write to] $file.absolutePath")
    }
}
