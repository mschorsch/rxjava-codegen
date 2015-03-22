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
import rx.codegen.internal.util.CodegenUtil;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author Matthias
 */
abstract class AbstractTypeSpec implements TypeSpec {

    protected final CodegenUtil util;
    protected final TypeElement typeElement;

    public AbstractTypeSpec(CodegenUtil util, TypeElement typeElement) {
        this.util = util;
        this.typeElement = typeElement;
    }

    @Override
    public TypeElement getTypeElement() {
        return typeElement;
    }

    @Override
    public String getFQOriClassnameWithGenerics() {
        return util.generateFullQualifiedNameWithGenerics(typeElement);
    }

    @Override
    public String getFQOriClassname() {
        return util.rawTypeElementToString(typeElement);
    }

    @Override
    public String getGeneratedQualifiedClassname() {
        return getGeneratedPackagename() + "." + getGeneratedSimpleClassname();
    }

    @Override
    public String getOriAnnotationComment() {
        return String.format("Orginating type: %s", getFQOriClassname());
    }
}
