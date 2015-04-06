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
import rx.codegen.NamingStrategy;
import rx.codegen.RxCodeGenerator;
import rx.codegen.RxExclude;
import rx.codegen.RxRefCodeGenerator;
import rx.codegen.internal.spec.MethodSpec;
import rx.codegen.internal.spec.TypeSpec;
import rx.codegen.internal.spec.method.MethodSpecFactory;
import rx.codegen.internal.spec.type.TypeSpecFactory;
import rx.codegen.internal.util.CodegenUtil;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

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

            try {
                final MethodSpec spec = MethodSpecFactory.createMethodSpec(util, typeSpec.getTypeElement(), methodElem);
                if (spec != null) {
                    definitions.add(spec);
                }
            } catch (RuntimeException ex) {
                //ignore
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, ex.getMessage(), methodElem);
            }
        }

        //
        // handle duplicate method names
        return applyAutomaticMethodNamingStrategy(typeSpec, definitions);
    }

    private List<MethodSpec> applyAutomaticMethodNamingStrategy(TypeSpec typeSpec, List<MethodSpec> methodSpecs) {
        final Map<String, MethodSpec> map = new LinkedHashMap<String, MethodSpec>(); //preserve order

        for (MethodSpec spec : methodSpecs) {
            final String generatedMethodname = spec.getGeneratedMethodname();

            if (map.containsKey(generatedMethodname)) {
                if (typeSpec.getMethodNamingStrategy() == NamingStrategy.RENAME) {
                    final String newMethodname = generateUniqueName(generatedMethodname, spec);
                    final MethodSpec newDef = MethodSpecFactory.renameMethodDefinition(spec, newMethodname);

                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            String.format("Renaming method in '%s' from '%s' to '%s'", typeSpec.getGeneratedQualifiedClassname(), generatedMethodname, newMethodname),
                            spec.getOriginatingElement());

                    map.put(newMethodname, newDef);

                } else if (typeSpec.getMethodNamingStrategy() == NamingStrategy.EXCLUDE) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            String.format("Filtering ambiguous method '%s' in '%s'", generatedMethodname, typeSpec.getGeneratedQualifiedClassname()),
                            spec.getOriginatingElement());
                }
            } else {
                map.put(generatedMethodname, spec);
            }
        }

        return Lists.newArrayList(map.values());
    }

    private String generateUniqueName(final String generatedMethodname, MethodSpec spec) {
        final StringBuilder ret = new StringBuilder(generatedMethodname);
        ret.append("$");

        final List<? extends VariableElement> parameters = spec.getOriginatingElement().getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (i != 0) {
                ret.append("_");
            }
            final VariableElement parameter = parameters.get(i);
            final TypeMirror paramType = parameter.asType();
            ret.append(kindToString(paramType.getKind()));
        }

        return ret.toString();
    }

    private String kindToString(TypeKind kind) {
        switch (kind) {
            case BOOLEAN:
                return "Bool";
            case BYTE:
                return "Byte";
            case SHORT:
                return "Short";
            case INT:
                return "Int";
            case LONG:
                return "Long";
            case CHAR:
                return "Char";
            case FLOAT:
                return "Float";
            case DOUBLE:
                return "Double";
            case VOID:
                return "Void";
            case ARRAY:
                return "Array";
            case DECLARED:
                return "Obj";
            case TYPEVAR:
                return "TypeVar";

            default:
                return "Unknown";
        }
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

            if (!typeSpec.includeDeprecated() && util.getElementUtils().isDeprecated(element)) {
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

    private class ExecElementComparator implements Comparator<ExecutableElement> {

        @Override
        public int compare(ExecutableElement o1, ExecutableElement o2) {
            final String name1 = o1.getSimpleName().toString();
            final String name2 = o2.getSimpleName().toString();

            //compare by methodname
            int dif = name1.compareTo(name2);
            if (dif != 0) {
                return dif;
            }

            //compare by parameter count
            dif = o1.getParameters().size() - o2.getParameters().size();
            if (dif != 0) {
                return dif;
            }

            //compare by paramter type names
            return Joiner.on("").join(util.elementsToTypes(o1.getParameters()))
                    .compareTo(Joiner.on("").join(util.elementsToTypes(o2.getParameters())));
        }
    }
}
