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

import rx.codegen.RxCodeGenerator;
import rx.codegen.internal.util.CodegenUtil;
import javax.lang.model.element.TypeElement;
import rx.codegen.NamingStrategy;

/**
 *
 * @author Matthias
 */
class StandardSpec extends AbstractTypeSpec {

    private final RxCodeGenerator classCodeGenerator;

    public StandardSpec(CodegenUtil util, TypeElement typeElement, RxCodeGenerator classCodeGenerator) {
        super(util, typeElement);
        this.classCodeGenerator = classCodeGenerator;
    }

    @Override
    public TypeElement getOriginatingElement() {
        return typeElement;
    }

    @Override
    public String getGeneratedPackagename() {
        return util.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
    }

    @Override
    public String getGeneratedSimpleClassname() {
        return classCodeGenerator.name().isEmpty()
                ? util.generateSimpleClassname(typeElement)
                : classCodeGenerator.name();
    }

    @Override
    public String getOriAnnotationname() {
        return RxCodeGenerator.class.getName();
    }

    @Override
    public NamingStrategy getMethodNamingStrategy() {
        return classCodeGenerator.options().strategy();
    }

    @Override
    public boolean includeDeprecated() {
        return classCodeGenerator.options().includeDeprecated();
    }
}
