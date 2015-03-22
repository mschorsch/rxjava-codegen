/*
 * Copyright 2015 Matthias Schorsch.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.codegen.internal;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import rx.codegen.internal.spec.MethodSpec;
import rx.codegen.internal.spec.method.MethodSpecFactory;
import rx.codegen.internal.util.CodegenUtil;
import rx.codegen.RxCodeGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import rx.codegen.NamingStrategy;
import rx.codegen.RxExclude;
import rx.codegen.RxRefCodeGenerator;
import rx.codegen.internal.spec.TypeSpec;
import rx.codegen.internal.spec.type.TypeSpecFactory;

/**
 *
 * @author Matthias
 */
@SupportedAnnotationTypes({
    "rx.codegen.RxCodeGenerator",
    "rx.codegen.RxRefCodeGenerator"
})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class RxJavaProcessor extends AbstractProcessor {

    private static final boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = false;

    private CodegenUtil util;
    private SourceWriter sourceWriter;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.util = new CodegenUtil(processingEnv);
        try {
            this.sourceWriter = new SourceWriter(util);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || annotations.isEmpty()) {
            return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
        }

        for (Element element : findElementsForProcessing(roundEnv)) {
            processClassInfos(TypeSpecFactory.createTypeSpecs(util, element));
        }

        return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
    }

    private Set<Element> findElementsForProcessing(RoundEnvironment roundEnv) {
        final Set<Element> ret = new LinkedHashSet<Element>();
        ret.addAll(roundEnv.getElementsAnnotatedWith(RxRefCodeGenerator.class));
        ret.addAll(roundEnv.getElementsAnnotatedWith(RxCodeGenerator.class));
        return ret;
    }

    private void processClassInfos(List<? extends TypeSpec> classInfos) {
        for (TypeSpec classInfo : classInfos) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "Processing ... ", classInfo.getTypeElement());
            processClassInfo(classInfo);
        }
    }

    private void processClassInfo(TypeSpec typeSpec) {
        try {
            sourceWriter.writeSourceFile(typeSpec, collectMethodDefinitions(typeSpec));
        } catch (IOException ex) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    ex.getMessage(), typeSpec.getTypeElement());
            throw new RuntimeException(ex);
        }
    }

    private List<MethodSpec> collectMethodDefinitions(TypeSpec typeSpec) {
        //
        // Get supported methods
        final List<ExecutableElement> supportedMethods = getSupportedMethods(typeSpec);

        //
        // Create all method defintions
        final List<MethodSpec> definitions = new ArrayList<MethodSpec>();
        for (ExecutableElement methodElem : supportedMethods) {
            if (methodElem.getAnnotation(RxExclude.class) != null) {
                continue; //exclude method from processing
            }

            final MethodSpec definition = MethodSpecFactory.createMethodSpec(util, typeSpec.getTypeElement(), methodElem);
            if (definition != null) {
                definitions.add(definition);
            }
        }

        //
        // handle duplicate method names
        return applyAutomaticMethodNamingStrategy(typeSpec, definitions);
    }

    private List<MethodSpec> applyAutomaticMethodNamingStrategy(TypeSpec typeSpec, List<MethodSpec> methodSpecs) {
        final Map<String, MethodSpec> map = new LinkedHashMap<String, MethodSpec>(); //preserve order

        for (MethodSpec definition : methodSpecs) {
            final String generatedMethodname = definition.getGeneratedMethodname();

            if (map.containsKey(generatedMethodname)) {
                if (typeSpec.getMethodNamingStrategy() == NamingStrategy.RENAME) {
                    final String newMethodname = String.format("%s$%s", generatedMethodname,
                            Joiner.on("_").join(definition.getOriginatingElement().getParameters()));
                    final MethodSpec newDef = MethodSpecFactory.renameMethodDefinition(definition, newMethodname);

                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            String.format("Renaming method in '%s' from '%s' to '%s'", typeSpec.getGeneratedQualifiedClassname(), generatedMethodname, newMethodname),
                            definition.getOriginatingElement());

                    map.put(newMethodname, newDef);

                } else if (typeSpec.getMethodNamingStrategy() == NamingStrategy.EXCLUDE) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            String.format("Filtering ambiguous method '%s' in '%s'", generatedMethodname, typeSpec.getGeneratedQualifiedClassname()),
                            definition.getOriginatingElement());
                }
            } else {
                map.put(generatedMethodname, definition);
            }
        }

        return Lists.newArrayList(map.values());
    }

    private List<ExecutableElement> getSupportedMethods(TypeSpec typeSpec) {
        final List<ExecutableElement> ret = new ArrayList<ExecutableElement>();

        // collect elements
        final List<? extends Element> enclosedElements = typeSpec.getTypeElement().getEnclosedElements();
        final List<ExecutableElement> methods = ElementFilter.methodsIn(enclosedElements);
        final List<ExecutableElement> constructors = ElementFilter.constructorsIn(enclosedElements);

        //union
        final List<ExecutableElement> execElements = Lists.newArrayList(methods);
        execElements.addAll(constructors);

        //sortby by name and number of parameters
        Collections.sort(execElements, new ExecElementComparator());

        for (ExecutableElement element : execElements) {
            if (!element.getThrownTypes().isEmpty()) {
                continue; //not supported
            }

            if (!element.getModifiers().contains(Modifier.PUBLIC)) {
                continue; //not supported
            }

            if (element.getKind() == ElementKind.CONSTRUCTOR && !isInstantiable(typeSpec.getTypeElement())) {
                //only constructors from static nested classes are allowed
                continue; //not supported
            }
            
            if(!typeSpec.includeDeprecated() && util.getElementUtils().isDeprecated(element)) {
                continue; //filter out deprecated methods
            }

            ret.add(element);
        }
        return ret;
    }

    private boolean isInstantiable(TypeElement typeElement) {
        final boolean nested = typeElement.getNestingKind().isNested();
        final Set<Modifier> modifiers = typeElement.getModifiers();

        if (nested && !modifiers.contains(Modifier.STATIC)) {
            return false;

        } else if (modifiers.contains(Modifier.ABSTRACT)) {
            return false;
        }

        return true;
    }

    private static class ExecElementComparator implements Comparator<ExecutableElement> {

        @Override
        public int compare(ExecutableElement o1, ExecutableElement o2) {
            final String name1 = o1.getSimpleName().toString();
            final String name2 = o2.getSimpleName().toString();
            int dif = name1.compareTo(name2);
            if (dif != 0) {
                return dif;
            }
            return o1.getParameters().size() - o2.getParameters().size();
        }
    }
}
