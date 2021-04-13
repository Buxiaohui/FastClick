/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.bxh.fastclick

import com.squareup.javapoet.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import javax.lang.model.element.Modifier

class GeneratorToolClassTask extends DefaultTask {

    @Override
    String getName() {
        return super.getName()
    }

    @TaskAction
    def action() {
        long start = System.currentTimeMillis()
        // 生成java类
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("FastClickUtils")
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

        // fastClickTimeMap && FastClickLruCache
        ClassName fastClickLruCacheClassName = ClassName.get("", "FastClickLruCache")
        // fastClickTimeMap
        FieldSpec.Builder fastClickTimeMapFieldBuilder = FieldSpec.builder(fastClickLruCacheClassName, "fastClickTimeMap")
        fastClickTimeMapFieldBuilder.initializer('new $T(64)', fastClickLruCacheClassName)
                .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE)
        classBuilder.addField(fastClickTimeMapFieldBuilder.build())

        // FastClickLruCache
        ClassName lruCacheClassName = ClassName.get("android.util", "LruCache")
        TypeSpec.Builder fastClickLruCacheClassBuilder = TypeSpec.classBuilder(fastClickLruCacheClassName)
        fastClickLruCacheClassBuilder
                .superclass(ParameterizedTypeName.get(lruCacheClassName, ClassName.get(String.class), ClassName.get(Long
                        .class)))
                .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE)


        MethodSpec entryRemovedMS = MethodSpec.methodBuilder("entryRemoved")
                .addParameter(TypeName.BOOLEAN, "evicted")
                .addParameter(String.class, "key")
                .addParameter(Long.class, "oldValue")
                .addParameter(Long.class, "newValue")
                .addModifiers(Modifier.PROTECTED)
                .addStatement("super.entryRemoved(evicted, key, oldValue, newValue)")
                .beginControlFlow("if (!evicted)")
                .addStatement("return")
                .endControlFlow()
                .beginControlFlow("if (oldValue != null && key != null)")
                .addStatement("long curTime = android.os.SystemClock.elapsedRealtime()")
                .beginControlFlow("if (curTime - oldValue > 20000)")
                .addStatement("backupMap.clear()")
                .nextControlFlow("else")
                .addStatement("backupMap.put(key, oldValue)")
                .endControlFlow()
                .endControlFlow()
                .returns(TypeName.VOID)
                .build()
        fastClickLruCacheClassBuilder.addMethod(entryRemovedMS)

        MethodSpec superMS = MethodSpec.constructorBuilder()
                .addParameter(TypeName.INT, "maxSize")
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super(maxSize)")
                .build()
        fastClickLruCacheClassBuilder.addMethod(superMS)

        classBuilder.addType(fastClickLruCacheClassBuilder.build())
        // backupMap
        ClassName concurrentHashMapClassName = ClassName.get("java.util.concurrent", "ConcurrentHashMap")
        FieldSpec backupMapFieldSpec = FieldSpec.builder(ParameterizedTypeName.get(
                concurrentHashMapClassName, TypeName.get(String.class), TypeName.get(Long.class)), "backupMap")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer('new $T()', concurrentHashMapClassName)
                .build()
        classBuilder.addField(backupMapFieldSpec)

        TypeSpec classTypeSpec = classBuilder.build()
        JavaFile javaFile = JavaFile.builder("com.buxiaohui.fastclick", classTypeSpec).build()

        // 将java写入到文件夹下
        LogUtils.logD("[project.buildDir is] $project.buildDir")
        LogUtils.logD("[project.projectDir is] $project.projectDir")
        File file = new File(project.buildDir, "generated/source/container")
        if (!file.exists()) {
            file.mkdirs()
        }
        javaFile.writeTo(file)
        LogUtils.logI("[write to] ${file.absolutePath} -> ${javaFile.packageName}.${classTypeSpec.name}")
        LogUtils.logI("[GeneratorToolClassTask cost] ${System.currentTimeMillis() - start} ms")
    }
}
