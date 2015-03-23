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
import rx.codegen.internal.util.CodegenUtil;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author Matthias
 */
class ActionXSpec extends AbstractMethodSpec {
    
    public ActionXSpec(CodegenUtil util, TypeElement classElement, ExecutableElement methodElement) {
        super(util, classElement, methodElement);
    }

    @Override
    public String getReturnType() {
        final int numberOfParameters = getNumberOfParameters();
        final List<TypeMirror> parameterTypes = util.elementsToTypes(methodElement.getParameters());

        if (getCalledType().isStaticMethodCall()) {
            if(numberOfParameters == 0) {
                return "Action0";
                
            } else if(numberOfParameters <= 9) {
                final String genericParameters = String.format("<%s>", Joiner.on(", ").join(getTypeParameterNames(parameterTypes, true)));
                return String.format("Action%d%s", numberOfParameters, genericParameters);
                
            } else {
                return "ActionN";
            }            
        } else {
            return String.format("Action1<%s>", util.generateFullQualifiedNameWithGenerics(classElement));
        }        
    }
}
