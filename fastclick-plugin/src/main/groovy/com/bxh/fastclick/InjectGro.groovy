/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.bxh.fastclick

import com.google.common.io.ByteStreams
import com.google.common.io.Files
import javassist.*
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.MethodInfo
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.LongMemberValue
import javassist.bytecode.annotation.StringMemberValue
import org.apache.http.util.TextUtils
import org.gradle.api.Project

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * buxiaohui
 * TODO 优化点：1.获取method的方式，不使用遍历
 */
class InjectGro {
    //初始化类池,以单例模式获取
    private ClassPool pool
    private Project project
    private ConfigExtension configExtension

    InjectGro(Project project) {
        this.pool = ClassPool.default
        this.project = project
    }

    void setFCConfig(ConfigExtension configExtension) {
        this.configExtension = configExtension
    }

    void appendClassPath(String path) {
        LogUtils.logD("appendClassPath->appendClassPath = " + path)
        pool.appendClassPath(path);
    }

    void injectJar(String inputPath, String outPutPath) throws NotFoundException, CannotCompileException {
        ArrayList entries = new ArrayList()
        Files.createParentDirs(new File(outPutPath))
        FileInputStream fis = null
        ZipInputStream zis = null
        FileOutputStream fos = null
        ZipOutputStream zos = null
        try {
            fis = new FileInputStream(new File(inputPath))
            zis = new ZipInputStream(fis)
            fos = new FileOutputStream(new File(outPutPath))
            zos = new ZipOutputStream(fos)
            ZipEntry entry = zis.getNextEntry()
            while (entry != null) {
                String fileName = entry.getName()
                if (!entries.contains(fileName)) {
                    entries.add(fileName);
                    zos.putNextEntry(new ZipEntry(fileName));
                    if (!entry.isDirectory()
                            && fileName.endsWith(".class")
                            && !fileName.contains('R$')
                            && !fileName.contains("R.class")
                            && !fileName.contains("BuildConfig.class")) {
                        transformJar(zis, zos, pool)
                    } else {
                        ByteStreams.copy(zis, zos)
                    }
                }
                entry = zis.getNextEntry()
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            if (zos != null) {
                zos.close()
            }
            if (fos != null) {
                fos.close()
            }
            if (zis != null) {
                zis.close()
            }
            if (fis != null) {
                fis.close()
            }
        }
    }

    void injectDir(String inputPath, String outPutPath) throws NotFoundException, CannotCompileException {
        File dir = new File(inputPath)
        LogUtils.logD("injectDir->inputPath = " + inputPath)
        LogUtils.logD("injectDir->outPutPath = " + outPutPath)
        //判断如果是文件夹，则遍历文件夹
        if (dir.isDirectory()) {
            //开始遍历
            dir.eachFileRecurse { File file ->
                if (file.isFile()) {
                    String filePath = file.getAbsolutePath()
                    File outPutFile = new File(outPutPath + filePath.substring(inputPath.length()))
                    Files.createParentDirs(outPutFile);
                    if (filePath.endsWith(".class")
                            && !filePath.contains('R$')
                            && !filePath.contains('R.class')
                            && !filePath.contains("BuildConfig.class")) {

                        LogUtils.logD("injectDir->file = " + file)
                        LogUtils.logD("injectDir->outPutFile = " + outPutFile)
                        FileInputStream inputStream = new FileInputStream(file)
                        FileOutputStream outputStream = new FileOutputStream(outPutFile)
                        transformDir(inputStream, outputStream, pool)
                    }
                }
            }
        }
    }

    void transformJar(ZipInputStream inputStream, ZipOutputStream outputStream, ClassPool classPool) {
        try {
            CtClass c = classPool.makeClass(inputStream);
            injectReal(c, classPool);
            outputStream.write(c.toBytecode());
            c.detach()
        } catch (Exception e) {
            e.printStackTrace()
            inputStream.close()
            outputStream.close()
            throw new RuntimeException(e.getMessage())
        }
    }

    void transformDir(FileInputStream inputStream, FileOutputStream outputStream, ClassPool classPool) {
        try {
            CtClass c = classPool.makeClass(inputStream);
            injectReal(c, classPool);
            outputStream.write(c.toBytecode());
            c.detach()
        } catch (Exception e) {
            e.printStackTrace()
            inputStream.close()
            outputStream.close()
            throw new RuntimeException(e.getMessage())
        }
    }

    void injectReal(CtClass ctClass, ClassPool classPool) {
        if (ctClass.name.contains("glide")) {
            LogUtils.logD("injectReal,ctClass:" + ctClass.name)
        }
        if (ctClass.isFrozen()) {
            ctClass.defrost()
        }
        CtMethod[] methods = ctClass.getDeclaredMethods()
        if (methods != null && methods.length > 0) {
            for (CtMethod ctMethod : methods) {
                if (ctMethod != null) {
                    MethodInfo methodInfo = ctMethod.getMethodInfo()
                    // 快速点击
                    if (checkMethod(ctMethod.getModifiers())) {
                        if (methodInfo != null) {
                            insertWithAnnotation(methodInfo, ctMethod)
                        }
                    }
                }
            }
        }
    }

    private void insertWithAnnotation(MethodInfo methodInfo, CtMethod ctMethod) {
        AnnotationsAttribute attr = (AnnotationsAttribute) methodInfo
                .getAttribute(AnnotationsAttribute.visibleTag);
        if (attr != null) {
            Annotation[] annotations = attr.getAnnotations();
            if (annotations != null && annotations.length > 0) {
                for (int j = 0; j < annotations.length; j++) {
                    Annotation annotation = annotations[j]
                    if (annotation != null) {
                        String injectFCCode = ""
                        if ("com.buxiaohui.fastclick.FC".equalsIgnoreCase(annotation.typeName)) {
                            LongMemberValue timeIntervalMemberValue = annotation.getMemberValue("timeInterval")
                            StringMemberValue tagMemberValue = annotation.getMemberValue("tag")
                            def isTagInvalid = tagMemberValue == null || TextUtils.isEmpty(tagMemberValue.value)
                            def isTimeIntervalInvalid = timeIntervalMemberValue == null || timeIntervalMemberValue.value <= 0
                            String tag = isTagInvalid ? "" : tagMemberValue.value
                            long timeInterval = isTimeIntervalInvalid ? 0 : timeIntervalMemberValue.value
                            injectFCCode = "if (com.buxiaohui.fastclick.FastClickUtils.isFastClick(\"$tag\",${timeInterval}L)) {\n" +
                                    "                                                    return;\n" +
                                    "                                                }"
                            LogUtils.logD("annotation:$annotation")
                            LogUtils.logD("tagMemberValue:$tagMemberValue")
                            LogUtils.logD("timeIntervalMemberValue:$timeIntervalMemberValue")
                        }
                        if (ctMethod != null && !TextUtils.isEmpty(injectFCCode)) {
                            LogUtils.logD("injectFCCode:$injectFCCode")
                            ctMethod.insertBefore(injectFCCode) // 在方法开始注入代码
                        }
                    }
                }
            }
        }
    }

    private static boolean checkMethod(int modifiers) {
        return !Modifier.isStatic(modifiers) && !Modifier.isNative(modifiers) && !Modifier.isAbstract(modifiers) && !Modifier.isEnum(modifiers) && !Modifier.isInterface(modifiers)
    }

}