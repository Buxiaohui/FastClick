/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.bxh.fastclick

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

class FCTransform extends Transform {

    private static final String DEFAULT_NAME = "FCTransform"
    private static final boolean DEBUG_TEST_INSERT_GLIDE_BEFORE = false


    private Project project
    private InjectGro injectGro
    private ConfigExtension configExtension

    FCTransform(Project project) {
        this.project = project
        injectGro = new InjectGro(project)
    }

    void setConfigExtension(ConfigExtension configExtension) {
        this.configExtension = configExtension
        injectGro.setFCConfig(configExtension)
    }

    /**
     * 设置自定义的Transform对应的Task名称
     * @return Task名称
     */
    @Override
    public String getName() {
        return DEFAULT_NAME
    }

    /**
     * 需要处理的数据类型，CONTENT_CLASS代表处理class文件
     * @return
     */
    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 指定Transform的作用范围
     * @return
     */
    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        Set<QualifiedContent.Scope> SCOPES = new HashSet<>();
        SCOPES.add(QualifiedContent.Scope.PROJECT)
        SCOPES.add(QualifiedContent.Scope.SUB_PROJECTS)
        SCOPES.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
        return SCOPES
    }

/**
 * 指明当前Transform是否支持增量编译
 * @return
 */
    @Override
    public boolean isIncremental() {
        return false
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        TransformOutputProvider outputProvider = transformInvocation.outputProvider;
        //**** 1.为了能找到android相关的所有类，添加project.android.bootClasspath 加入android.jar，

        injectGro.appendClassPath(project.android.bootClasspath[0].toString())


        for (TransformInput input : transformInvocation.inputs) {
            if (null == input) {
                continue
            }

            //**** 1.先把所有路径appendClassPath
            for (JarInput jarInput : input.jarInputs) {
                injectGro.appendClassPath(jarInput.file.absolutePath)
                if (jarInput.file.absolutePath.contains("glide")) {
                    // LogUtils.logD("appendClassPath->jarInput:" + jarInput.file.absolutePath)
                }
            }
            for (DirectoryInput directoryInput : input.directoryInputs) {
                injectGro.appendClassPath(directoryInput.file.absolutePath)
                if (directoryInput.file.absolutePath.contains("glide")) {
                    // LogUtils.logD("appendClassPath->directoryInput:" + jdirectoryInput.file.absolutePath)
                }
            }
        }
        testInsertGlide()

        for (TransformInput input : transformInvocation.inputs) {
            //***** 2.再进行织入操作
            for (DirectoryInput directoryInput : input.directoryInputs) {
                // 获取output目录
                def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                //注入代码
                injectGro.injectDir(directoryInput.file.absolutePath, dest.absolutePath)
            }


            for (JarInput jarInput : input.jarInputs) {
                // 重命名输出文件
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                def dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                injectGro.injectJar(jarInput.file.absolutePath, dest.absolutePath)
            }
        }
    }

    private void testInsertGlide() {
        if (!DEBUG_TEST_INSERT_GLIDE_BEFORE) {
            return
        }
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get("com.bumptech.glide.load.data.HttpUrlFetcher")
            LogUtils.logD("get,HttpUrlFetcher,ctClass:" + ctClass)
            CtMethod ctMethod = ctClass.getDeclaredMethod("loadData")
            LogUtils.logD("get,HttpUrlFetcher,ctMethod:" + ctMethod)
            ctMethod.insertAt(59, "new com.buxiaohui.fastclickjavaassist.DataListener().onReady(result);")
            ctClass.writeFile()
        } catch (Exception e) {
            LogUtils.logD("e:" + e)
        }
    }
}
