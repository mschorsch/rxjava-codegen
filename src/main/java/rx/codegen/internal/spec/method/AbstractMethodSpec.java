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

import rx.codegen.RxMethod;
import rx.codegen.internal.spec.MethodSpec;
import rx.codegen.internal.util.CodegenUtil;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import java.util.*;

/**
 *
 * @author Matthias
 */
abstract class AbstractMethodSpec implements MethodSpec {

    protected final CodegenUtil util;
    protected final TypeElement classElement;
    protected final ExecutableElement methodElement;
    protected final Map<TypeVariable, String> typeVariableNameMapping;
    protected final RxMethod methodGenerator;

    protected AbstractMethodSpec(CodegenUtil util, TypeElement classElement, ExecutableElement methodElement) {
        this.util = util;
        this.classElement = classElement;
        this.methodElement = methodElement;
        this.methodGenerator = methodElement.getAnnotation(RxMethod.class);
        this.typeVariableNameMapping = initTypeVariableNameMapping();
    }

    private Map<TypeVariable, String> initTypeVariableNameMapping() {
        //FIXME ugly
        
        final Map<TypeVariable, String> ret = new HashMap<TypeVariable, String>();
        final Set<TypeVariable> classTypeVariables = util.findAllTypeVariables(classElement);

        int i = 0;
        for (TypeVariable classTypeVariable : classTypeVariables) {
            ret.put(classTypeVariable, String.format("C%d", i++));
        }

        final Set<TypeVariable> methodTypeVariables = util.findAllTypeVariablesFromElements(methodElement.getTypeParameters());
        methodTypeVariables.removeAll(classTypeVariables);
        int j = 0;
        for (TypeVariable methodTypeVariable : methodTypeVariables) {
            ret.put(methodTypeVariable, String.format("M%d", j++));
        }
        return ret;
    }

    @Override
    public ExecutableElement getOriginatingElement() {
        return methodElement;
    }

    @Override
    public CalledType getCalledType() {
        final boolean staticMethod = methodElement.getModifiers().contains(Modifier.STATIC);
        return staticMethod ? CalledType.STATIC_METHOD : CalledType.OBJECT_METHOD;
    }

    @Override
    public String getGeneratedMethodname() {
        // get name of generated method
        final String name = (methodGenerator == null || methodGenerator.name().isEmpty())
                ? getCalledMethodname()
                : methodGenerator.name();

        // if the new method would override a method of object,
        // then we have to rename our newly generated method
        return util.overridesMethodOfObject(classElement, methodElement, name) ? name + "_" : name;
    }

    @Override
    public String getCalledMethodname() {
        return methodElement.getSimpleName().toString();
    }

    @Override
    public String getModifier() {
        final Set<Modifier> modifiers = methodElement.getModifiers();
        Modifier ret = null;
        if (modifiers.contains(Modifier.PUBLIC)) {
            ret = Modifier.PUBLIC;
        } else if (modifiers.contains(Modifier.PROTECTED)) {
            ret = Modifier.PROTECTED;
        } else if (modifiers.contains(Modifier.PRIVATE)) {
            ret = Modifier.PRIVATE;
        }
        return ret != null ? ret.name().toLowerCase(Locale.US) : "";
    }

    @Override
    public String getReturnTypeOfCallMethod() {
        final TypeMirror type = methodElement.getReturnType();
        return getTypeParameterName(type, true);
    }

    private String getTypeParameterName(TypeMirror type, boolean suppressTypeVarDecl) {
        return util.typeToString(type, typeVariableNameMapping, suppressTypeVarDecl);
    }

    @Override
    public List<String> getGenericsDecl() {
        final Set<TypeVariable> relevantTypeVariables = new LinkedHashSet<TypeVariable>();

        if (getCalledType().isStaticMethodCall()) {
            relevantTypeVariables.addAll(util.findAllTypeVariablesFromElements(methodElement.getParameters()));
            relevantTypeVariables.addAll(util.findAllTypeVariables(methodElement.getReturnType()));

        } else {
            final Set<TypeVariable> classTypeVariables = util.findAllTypeVariables(classElement);
            relevantTypeVariables.addAll(classTypeVariables);
            relevantTypeVariables.addAll(util.findAllTypeVariablesFromElements(methodElement.getParameters()));
            relevantTypeVariables.addAll(util.findAllTypeVariables(methodElement.getReturnType()));
        }

        final List<String> ret = new ArrayList<String>();
        for (TypeVariable typeVariable : relevantTypeVariables) {
            ret.add(getTypeParameterName(typeVariable, false));
        }

        return ret;
    }

    @Override
    public List<VariableSpec> getParameters() {
        final List<? extends VariableElement> variables = methodElement.getParameters();
        final List<VariableSpec> variableSpecs = new ArrayList<VariableSpec>();

        for (int idx = 0; idx < variables.size(); idx++) {
            final VariableElement variable = variables.get(idx);
            final TypeMirror variableType = variable.asType();

            final String typeName = getTypeParameterName(variableType, true);
            final String unboxedTypeName = variableType.getKind().isPrimitive() ? variableType.toString() : typeName;
            final String varName = isMethodN() ? String.format("args[%d]", idx) : variable.getSimpleName().toString();

            variableSpecs.add(new DefaultVariableSpec(typeName, unboxedTypeName, varName));
        }

        return variableSpecs;
    }

    private boolean isMethodN() {
        return getNumberOfParameters() > 9;
    }

    private int getNumberOfParameters() {
        return methodElement.getParameters().size();
    }

    @Override
    public String getJavadoc() {
        final String docComment = util.getElementUtils().getDocComment(methodElement);
        return docComment != null && !docComment.isEmpty() ? util.createJavadocComment(docComment) : "";
    }
}
