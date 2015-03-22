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
package rx.codegen.internal.spec.type;

import rx.codegen.internal.spec.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import rx.codegen.RxCodeGenerator;
import rx.codegen.RefClass;
import rx.codegen.RxRefCodeGenerator;
import rx.codegen.RefPackage;
import rx.codegen.internal.util.CodegenUtil;

/**
 *
 * @author Matthias
 */
public abstract class TypeSpecFactory {

    public static List<? extends TypeSpec> createTypeSpecs(CodegenUtil util, Element element) {
        final RxCodeGenerator classCodeGenerator = element.getAnnotation(RxCodeGenerator.class);
        final RxRefCodeGenerator refCodeGenerator = element.getAnnotation(RxRefCodeGenerator.class);

        if (classCodeGenerator != null && isRelevantClass(element)) {
            return createFromType(util, (TypeElement) element, classCodeGenerator);

        } else if (refCodeGenerator != null && element.getKind() == ElementKind.PACKAGE) {
            final List<TypeSpec> ret = new ArrayList<TypeSpec>();
            final PackageElement packageElement = (PackageElement) element;

            ret.addAll(createFromRefClasses(util, packageElement, refCodeGenerator));
            ret.addAll(createFromRefPackage(util, refCodeGenerator, packageElement));
            return ret;
        }
        return Collections.emptyList();
    }

    private static List<StandardSpec> createFromType(CodegenUtil util, TypeElement typeElement, RxCodeGenerator classCodeGenerator) {
        if (!isTypeElementSupported(util, typeElement)) {
            return Collections.emptyList();
        } else if (!classCodeGenerator.options().includeDeprecated() && util.isDeprecatedElement(typeElement)) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new StandardSpec(util, typeElement, classCodeGenerator));
    }

    private static List<RefSpec> createFromRefClasses(CodegenUtil util, PackageElement destinationPackageElement, RxRefCodeGenerator refCodeGenerator) {
        final List<RefSpec> ret = new ArrayList<RefSpec>();
        final String destinationPackageName = destinationPackageElement.getQualifiedName().toString();

        for (RefClass refClass : refCodeGenerator.classes()) {
            final TypeElement typeElement = util.getElementUtils().getTypeElement(refClass.name());
            if (typeElement == null) {
                util.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        String.format("Referenced class '%s' not found", refClass.name()));
                continue;
            }

            if (!isTypeElementSupported(util, typeElement)) {
                continue;
            } else if (!refClass.options().includeDeprecated() && util.isDeprecatedElement(typeElement)) {
                continue;
            }

            ret.add(new RefSpec(util, typeElement, destinationPackageName, refCodeGenerator, refClass));
        }
        return ret;
    }

    private static List<RefPackageSpec> createFromRefPackage(CodegenUtil util,
            RxRefCodeGenerator refCodeGenerator, PackageElement orginatingPackage) {
        final List<RefPackageSpec> ret = new ArrayList<RefPackageSpec>();
        for (RefPackage packageType : refCodeGenerator.packages()) {
            ret.addAll(createFromRefPackage(util, refCodeGenerator, packageType, orginatingPackage));
        }
        return ret;
    }

    private static List<RefPackageSpec> createFromRefPackage(CodegenUtil util,
            RxRefCodeGenerator refCodeGenerator, RefPackage refPackage,
            PackageElement orginatingPackage) {
        // resolve defined package
        final String packageName = refPackage.name();
        final PackageElement packageElement = util.getElementUtils().getPackageElement(packageName);
        if (packageElement == null) {
            util.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Referenced package '%s' not found", packageName), orginatingPackage);
            return Collections.emptyList();
        }

        //find all types in defined package
        final List<RefPackageSpec> ret = new ArrayList<RefPackageSpec>();
        final List<TypeElement> typeElements = ElementFilter.typesIn(packageElement.getEnclosedElements());
        for (TypeElement typeElement : typeElements) {
            if (!isRelevantClass(typeElement)) {
                continue;
            } else if (!isTypeElementSupported(util, typeElement)) {
                continue;
            } else if (!refPackage.options().includeDeprecated() && util.isDeprecatedElement(typeElement)) {
                continue;
            }

            ret.add(new RefPackageSpec(util, typeElement, orginatingPackage, refCodeGenerator, refPackage));
        }
        return ret;
    }

    private static boolean isTypeElementSupported(CodegenUtil util, TypeElement typeElement) {
        boolean supported = true;
        if (!typeElement.getModifiers().contains(Modifier.PUBLIC)) {
            supported = false; //not supported
        }

        if (!supported) {
            util.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    String.format("Processing of '%s' not supported",
                            typeElement.getQualifiedName().toString()),
                    typeElement);
        }
        return supported;
    }

    private static boolean isRelevantClass(final Element element) {
        final ElementKind kind = element.getKind();
        return kind == ElementKind.CLASS || kind == ElementKind.ENUM || kind == ElementKind.INTERFACE;
    }
}
