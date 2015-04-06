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

import rx.codegen.RefClass;
import rx.codegen.internal.util.CodegenUtil;
import javax.lang.model.element.TypeElement;
import rx.codegen.NamingStrategy;
import rx.codegen.RxRefCodeGenerator;

/**
 *
 * @author Matthias
 */
class RefSpec extends AbstractTypeSpec {

    private final String destinationPackage;
    private final RxRefCodeGenerator refCodeGenerator;
    private final RefClass refClass;

    public RefSpec(CodegenUtil util, TypeElement typeElement, String destinationPackage,
            RxRefCodeGenerator refCodeGenerator, RefClass refClass) {
        super(util, typeElement);
        this.destinationPackage = destinationPackage;
        this.refCodeGenerator = refCodeGenerator;
        this.refClass = refClass;
    }

    @Override
    public TypeElement getOriginatingElement() {
        return typeElement;
    }

    @Override
    public String getGeneratedPackagename() {
        return destinationPackage;
    }

    @Override
    public String getGeneratedSimpleClassname() {
        return refClass.simpleName().isEmpty()
                ? util.generateSimpleClassname(typeElement)
                : refClass.simpleName();
    }

    @Override
    public String getOriAnnotationname() {
        return RxRefCodeGenerator.class.getName();
    }

    @Override
    public NamingStrategy getMethodNamingStrategy() {
        return refClass.options().strategy();
    }
    
    @Override
    public boolean includeDeprecated() {
        return refClass.options().includeDeprecated();
    }    
}
