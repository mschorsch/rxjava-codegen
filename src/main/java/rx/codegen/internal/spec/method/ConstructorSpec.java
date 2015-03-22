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
package rx.codegen.internal.spec.method;

import com.google.common.base.Joiner;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import rx.codegen.internal.util.CodegenUtil;

/**
 * Constructor call.
 *
 * @author Matthias
 */
class ConstructorSpec extends AbstractMethodSpec {

    public ConstructorSpec(CodegenUtil util, TypeElement classElement, ExecutableElement methodElement) {
        super(util, classElement, methodElement);
    }

    @Override
    public boolean isAction() {
        return false;
    }

    @Override
    public CalledType getCalledType() {
        return CalledType.CONSTRUCTOR;
    }

    @Override
    public String getReturnTypeOfCallMethod() {
        return util.generateFullQualifiedNameWithGenerics(classElement);
    }

    @Override
    public String getReturnType() {
        final int numberOfParameters = getNumberOfParameters();
        final String returnTypeAsString = getReturnTypeOfCallMethod();
        final List<TypeMirror> parameterTypes = util.elementsToTypes(methodElement.getParameters());
        
        if (numberOfParameters == 0) {
            return String.format("Func0<%s>", returnTypeAsString);

        } else if (numberOfParameters <= 9) {
            final String genericParameters = String.format("<%s, %s>", Joiner.on(", ").join(getTypeParameterNames(parameterTypes, true)), returnTypeAsString);
            return String.format("Func%d%s", numberOfParameters, genericParameters);

        } else {
            return "FuncN";
        }
    }

    @Override
    public String getGeneratedMethodname() {
        return String.format("new%s", classElement.getSimpleName().toString());
    }

    @Override
    public String getCalledMethodname() {
        return "";
    }
}
