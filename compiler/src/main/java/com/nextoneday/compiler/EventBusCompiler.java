package com.nextoneday.compiler;
/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2018-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date	                 Author	                Action
 *  	                   	Create/Add/Modify/Delete
 * ===========================================================================================
 */

import com.google.auto.service.AutoService;
import com.google.common.graph.SuccessorsFunction;
import com.nextoneday.annoation.Subscribe;
import com.nextoneday.annoation.SubscriberInfo;
import com.nextoneday.annoation.SubscriberInfoBean;
import com.nextoneday.annoation.SubscriberMethod;
import com.nextoneday.annoation.ThreadMode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;


/**
 * Author: shah
 * Date : 2020/5/27.
 * Desc : EventBusCompiler
 */

@SupportedAnnotationTypes(Constact.SUCBSCRIBE)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedOptions({Constact.PACKAGENAME, Constact.APPNAME})
@AutoService(Processor.class)
public class EventBusCompiler extends AbstractProcessor {

    private Types mTypeUtils;
    private Messager mMessager;
    private Filer mFiler;
    private Elements mElementUtils;
    private String mPackageName;
    private String mClassName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        mTypeUtils = processingEnvironment.getTypeUtils();
        mMessager = processingEnvironment.getMessager();
        mFiler = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
        Map<String, String> options = processingEnvironment.getOptions();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "init");


        if (options.size() > 0) {
            mPackageName = options.get(Constact.PACKAGENAME);
            mClassName = options.get(Constact.APPNAME);
            mMessager.printMessage(Diagnostic.Kind.NOTE, "packageName:" + mPackageName);
            mMessager.printMessage(Diagnostic.Kind.NOTE, "mClassName:" + mClassName);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        if (set.size() <= 0) return false;

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Subscribe.class);
        if (elements.size() > 0) {
            mMessager.printMessage(Diagnostic.Kind.NOTE, "parseElement");
            parseElement(elements);
        }
        try {
            if (tempMap.size() > 0) {
                createIndexFile();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }



    private void createIndexFile() throws IOException {

        TypeName fieldType = ParameterizedTypeName.get(Map.class, Class.class, SubscriberInfo.class);
        TypeElement typeElement = mElementUtils.getTypeElement(Constact.SUPER_INTERFACE);
        CodeBlock.Builder staticBlock = CodeBlock.builder();
        staticBlock.addStatement("$L = new $T<$T,$T>()", Constact.FIELD_NAME, HashMap.class, Class.class, SubscriberInfo.class);
        for (Map.Entry<TypeElement, List<ExecutableElement>> listEntry : tempMap.entrySet()) {

            CodeBlock.Builder contentBlock = CodeBlock.builder();
            CodeBlock contentCode = null;
            String format;
            List<ExecutableElement> elementList = listEntry.getValue();
            for (int x = 0; x < elementList.size(); x++) {

                ExecutableElement element = elementList.get(x);

                Subscribe subscriber = element.getAnnotation(Subscribe.class);

                List<? extends VariableElement> parameters = element.getParameters();
                TypeElement paramElement = (TypeElement) mTypeUtils.asElement(parameters.get(0).asType());

                if (x == elementList.size() - 1) {

                    format = "new $T($T.class,$S, $T.class,$L,$L,$T.$L)";

                } else {

                    format = "new $T($T.class,$S,$T.class,$L,$L,$T.$L),\n";
                }

//                new SubscriberMethod(MainActivity.class,"event",Class.class,false,2, ThreadMode.POSTING);
                contentCode = contentBlock.add(format,
                        SubscriberMethod.class,
                        ClassName.get(listEntry.getKey()),
                        element.getSimpleName().toString(),
                        ClassName.get(paramElement),
                        subscriber.sticky(),
                        subscriber.priority(),
                        ThreadMode.class,
                        subscriber.threadMode())
                        .build();
            }

            if (contentCode != null) {
                staticBlock.beginControlFlow("putIndex(new $T ($T.class,new $T []",
                        SubscriberInfoBean.class,
                        ClassName.get(listEntry.getKey()),
                        SubscriberMethod.class)
                        .add(contentCode)
                        .endControlFlow("))");
            }else{
                mMessager.printMessage(Diagnostic.Kind.ERROR,"注解处理器双层循环发生错误!");
            }

        }


        ParameterSpec putIndexParam = ParameterSpec.builder(SubscriberInfo.class, Constact.PUT_INDEX_PARAM).build();

        MethodSpec putIndex = MethodSpec.methodBuilder(Constact.PUTINDEX)
                .addModifiers(Modifier.PRIVATE)
                .addModifiers(Modifier.STATIC)
                .addParameter(putIndexParam)
                .addStatement("$L.put($L.getSubscriberClass(),$L)", Constact.FIELD_NAME, Constact.PUT_INDEX_PARAM, Constact.PUT_INDEX_PARAM)

                .build();

        ParameterSpec getSubscriberInfoParam = ParameterSpec.builder(Class.class, Constact.GET_PARAM).build();

        MethodSpec getSubscriberInfo = MethodSpec.methodBuilder(Constact.GET_SUBSCRIBER_INFO)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(getSubscriberInfoParam)
                .returns(SubscriberInfo.class)
                .addStatement("return $L.get($L)", Constact.FIELD_NAME, Constact.GET_PARAM)
                .build();

        TypeSpec classType = TypeSpec.classBuilder(mClassName)
                .addSuperinterface(ClassName.get(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(fieldType, Constact.FIELD_NAME,
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addStaticBlock(staticBlock.build())
                .addMethod(putIndex)
                .addMethod(getSubscriberInfo)
                .build();

        JavaFile file = JavaFile.builder(mPackageName, classType).build();
        file.writeTo(mFiler);


    }

    //这是用来存放获取注解的集合，
    private Map<TypeElement, List<ExecutableElement>> tempMap = new HashMap<>();

    private void parseElement(Set<? extends Element> elements) {

        for (Element element : elements) {

            TypeElement typeElement = (TypeElement) element.getEnclosingElement();

            ExecutableElement method = (ExecutableElement) element;

            List<ExecutableElement> list = tempMap.get(typeElement);
            if (list == null) {
                list = new ArrayList<>();
                list.add(method);
                tempMap.put(typeElement, list);
            } else {
                list.add(method);
            }
        }
        mMessager.printMessage(Diagnostic.Kind.NOTE, "size: " + tempMap.toString());
    }
}
