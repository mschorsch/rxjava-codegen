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
package rx.codegen.internal.spec;

import java.util.List;
import javax.lang.model.element.ExecutableElement;
import rx.codegen.internal.spec.method.CalledType;

/**
 * Specification of an Action or Func generating method.
 * 
 * @author Matthias
 */
public interface MethodSpec {

    ExecutableElement getOriginatingElement();

    boolean isAction();

    CalledType getCalledType();

    String getJavadoc();

    String getModifier();

    List<String> getGenerics();

    String getReturnType();

    String getGeneratedMethodname();

    String getReturnTypeOfCallMethod();

    String getVariablesWithTypes();

    String getCalledMethodname();

    String getVariables();
}
