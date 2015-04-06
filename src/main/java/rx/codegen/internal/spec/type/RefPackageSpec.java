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

import rx.codegen.NamingStrategy;
import rx.codegen.RefPackage;
import rx.codegen.RxCodeGenerator;
import rx.codegen.RxRefCodeGenerator;
import rx.codegen.internal.util.CodegenUtil;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * @author Matthias
 */
class RefPackageSpec extends AbstractTypeSpec {

    private final PackageElement originatingPackage;
    private final RxRefCodeGenerator refCodeGenerator;
    private final RefPackage refPackage;

    public RefPackageSpec(CodegenUtil util, TypeElement typeElement, PackageElement originatingPackage,
                          RxRefCodeGenerator packageCodeGenerator, RefPackage refPackage) {
        super(util, typeElement);
        this.originatingPackage = originatingPackage;
        this.refCodeGenerator = packageCodeGenerator;
        this.refPackage = refPackage;
    }

    @Override
    public PackageElement getOriginatingElement() {
        return originatingPackage;
    }

    @Override
    public String getGeneratedPackagename() {
        final String sourcePackage = refPackage.name();
        final PackageElement packageOfType = util.getElementUtils().getPackageOf(typeElement);
        final String substring = packageOfType.getQualifiedName().toString().substring(sourcePackage.length());
        if (substring.isEmpty()) {
            return originatingPackage.getQualifiedName().toString();
        }
        return String.format("%s.%s", originatingPackage.getQualifiedName().toString(), substring);
    }

    @Override
    public String getGeneratedSimpleClassname() {
        final RxCodeGenerator classCodeGenerator = typeElement.getAnnotation(RxCodeGenerator.class);
        return classCodeGenerator == null || classCodeGenerator.name().isEmpty()
                ? util.generateSimpleClassname(typeElement)
                : classCodeGenerator.name();
    }

    @Override
    public String getOriAnnotationname() {
        return RxRefCodeGenerator.class.getName();
    }

    @Override
    public NamingStrategy getMethodNamingStrategy() {
        final RxCodeGenerator classCodeGenerator = typeElement.getAnnotation(RxCodeGenerator.class);
        return classCodeGenerator != null
                ? classCodeGenerator.options().strategy()
                : refPackage.options().strategy();
    }

    @Override
    public boolean includeDeprecated() {
        final RxCodeGenerator classCodeGenerator = typeElement.getAnnotation(RxCodeGenerator.class);
        return classCodeGenerator != null
                ? classCodeGenerator.options().includeDeprecated()
                : refPackage.options().includeDeprecated();
    }
}
