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

import java.util.List;
import rx.codegen.internal.spec.MethodSpec;
import javax.lang.model.element.ElementKind;
import rx.codegen.internal.util.CodegenUtil;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.UnknownTypeException;

/**
 *
 * @author Matthias
 */
public abstract class MethodSpecFactory {

    public static MethodSpec createMethodSpec(CodegenUtil util, TypeElement classElement, ExecutableElement methodElement) throws IllegalArgumentException {
        if (methodElement.getKind() == ElementKind.METHOD) {
            final TypeMirror returnType = methodElement.getReturnType();
            if (returnType.getKind() == TypeKind.VOID) {
                //action
                return new ActionXSpec(util, classElement, methodElement);
            } else {
                //func
                return new FuncXSpec(util, classElement, methodElement);
            }
        } else if (methodElement.getKind() == ElementKind.CONSTRUCTOR) {
            //func
            return new ConstructorSpec(util, classElement, methodElement);
        }
        throw new IllegalArgumentException(String.format("Unsupported method '%s' of kind '%s'", methodElement, methodElement.getKind()));
    }

    public static MethodSpec renameMethodDefinition(MethodSpec definition, String unambiguousGeneratedMethodname) {
        return new RenamedMethodSpec(definition, unambiguousGeneratedMethodname);
    }

    private static class RenamedMethodSpec implements MethodSpec {

        private final MethodSpec spec;
        private final String generatedMethodname;

        public RenamedMethodSpec(MethodSpec definition, String generatedMethodname) {
            this.spec = definition;
            this.generatedMethodname = generatedMethodname;
        }

        @Override
        public ExecutableElement getOriginatingElement() {
            return spec.getOriginatingElement();
        }

        @Override
        public boolean isAction() {
            return spec.isAction();
        }

        @Override
        public CalledType getCalledType() {
            return spec.getCalledType();
        }

        @Override
        public String getGeneratedMethodname() {
            return generatedMethodname;
        }

        @Override
        public String getCalledMethodname() {
            return spec.getCalledMethodname();
        }

        @Override
        public String getModifier() {
            return spec.getModifier();
        }

        @Override
        public List<String> getGenericsDecl() {
            return spec.getGenericsDecl();
        }

//        @Override
//        public String getReturnType() {
//            return spec.getReturnType();
//        }

        @Override
        public List<VariableSpec> getParameters() {
            return spec.getParameters();
        }

        @Override
        public String getReturnTypeOfCallMethod() {
            return spec.getReturnTypeOfCallMethod();
        }

        @Override
        public String getJavadoc() {
            return spec.getJavadoc();
        }
    }
}
