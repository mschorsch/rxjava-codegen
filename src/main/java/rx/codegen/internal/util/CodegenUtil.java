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
package rx.codegen.internal.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class CodegenUtil {

    private static final String JAVA_LANG_PACKAGE_NAME = "java.lang";
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\\r?\\n");

    private final ProcessingEnvironment processingEnv;
    private final Types typeUtils;
    private final Elements elementUtils;

    private final PackageElement javaLangPackage;
    private final TypeElement objectTypeElement;
    private final ListMultimap<String, ExecutableElement> objectMethodsByName;

    public CodegenUtil(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.typeUtils = processingEnv.getTypeUtils();
        this.elementUtils = processingEnv.getElementUtils();
        this.objectTypeElement = elementUtils.getTypeElement(Object.class.getName());
        this.javaLangPackage = elementUtils.getPackageElement(JAVA_LANG_PACKAGE_NAME);
        this.objectMethodsByName = initObjectMethodsByName(objectTypeElement);
    }

    private static ListMultimap<String, ExecutableElement> initObjectMethodsByName(TypeElement objectTypeElement) {
        final ListMultimap<String, ExecutableElement> ret = ArrayListMultimap.create();
        for (ExecutableElement method : ElementFilter.methodsIn(objectTypeElement.getEnclosedElements())) {
            ret.put(method.getSimpleName().toString(), method);
        }
        return ret;
    }

    public Messager getMessager() {
        return processingEnv.getMessager();
    }

    public Filer getFiler() {
        return processingEnv.getFiler();
    }

    public Locale getLocale() {
        return processingEnv.getLocale();
    }

    public Types getTypeUtils() {
        return typeUtils;
    }

    public Elements getElementUtils() {
        return elementUtils;
    }

    public String generateSimpleClassname(TypeElement element) {
        return element.getSimpleName().toString() + "_";
    }

    public String createJavadocComment(String rawComment) {
        final StringBuilder comment = new StringBuilder();
        comment.append("    /**").append("\n");
        final List<String> lines = Splitter.on(NEW_LINE_PATTERN).trimResults(CharMatcher.anyOf("\r\n")).splitToList(rawComment);
        final int size = lines.size();
        for (int i = 0; i < size; i++) {
            comment.append("     *").append(lines.get(i)).append("\n");
        }
        comment.append("     */");
        return comment.toString();
    }

    public String generateFullQualifiedNameWithGenerics(TypeElement typeElement) {
        final StringBuilder name = new StringBuilder(rawTypeElementToString(typeElement));

        final List<? extends TypeParameterElement> typeParameters = typeElement.getTypeParameters();
        if (!typeParameters.isEmpty()) {
            name.append("<");
            for (int number = 0; number < typeParameters.size(); number++) {
                if (number != 0) {
                    name.append(", ");
                }
                name.append("C").append(number);
            }
            name.append(">");
        }

        return name.toString();
    }

    public boolean overridesMethodOfObject(TypeElement classOfMethod, ExecutableElement method, String methodname) {
        final boolean staticMethod = method.getModifiers().contains(Modifier.STATIC);

        for (ExecutableElement methodOfObject : objectMethodsByName.get(methodname)) {
            if (staticMethod) {
                final int paramteterCount = methodOfObject.getParameters().size();
                if (paramteterCount == 0) {
                    return true;
                }
            } else if (elementUtils.overrides(method, methodOfObject, classOfMethod) || methodOfObject.equals(method)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeprecatedElement(Element method) {
        return elementUtils.isDeprecated(method);
    }

    public TypeMirror boxTypeIfNeeded(TypeMirror type) {
        return type.getKind().isPrimitive()
                ? typeUtils.boxedClass((PrimitiveType) type).asType()
                : type;
    }

    public List<TypeMirror> elementsToTypes(List<? extends Element> elements) {
        final List<TypeMirror> ret = new ArrayList<TypeMirror>();
        for (Element element : elements) {
            ret.add(boxTypeIfNeeded(element.asType()));
        }
        return ret;
    }

    public List<ExecutableElement> findRecursiveAllMethods(TypeElement rootTypeElement) {
        final List<? extends Element> allMembers = elementUtils.getAllMembers(rootTypeElement);
        return ElementFilter.methodsIn(allMembers);

        /*
         final Map<String, ExecutableElement> ret = new LinkedHashMap<String, ExecutableElement>(); //preserve order

         final LinkedList<TypeElement> queue = new LinkedList<TypeElement>();
         queue.add(rootTypeElement);

         TypeElement current;
         while ((current = queue.poll()) != null) {
         //
         // process current
         for (ExecutableElement method : ElementFilter.methodsIn(current.getEnclosedElements())) {
         final String methodname = method.getSimpleName().toString();
         if (!ret.containsKey(methodname)) {
         ret.put(methodname, method);
         }
         }
         //ret.addAll(ElementFilter.constructorsIn(current.getEnclosedElements()));

         //
         // add supertype
         final TypeMirror superclassType = current.getSuperclass();
         if (superclassType.getKind() != TypeKind.NONE) { // != java.lang.Object
         final TypeElement superclassElement = (TypeElement) typeUtils.asElement(superclassType);
         queue.add(superclassElement);
         }

         //
         // add interfaces
         for (TypeMirror interfaceType : current.getInterfaces()) {
         final TypeElement interfaceElement = (TypeElement) typeUtils.asElement(interfaceType);
         queue.add(interfaceElement);
         }
         }

         return new ArrayList<ExecutableElement>(ret.values());*/
    }

    public Set<TypeVariable> findAllTypeVariables(TypeElement typeElement) {
        return findAllTypeVariables(typeElement.asType());
    }

    public Set<TypeVariable> findAllTypeVariablesFromElements(List<? extends Element> elements) {
        final Set<TypeVariable> ret = new LinkedHashSet<TypeVariable>();
        for (Element element : elements) {
            ret.addAll(findAllTypeVariables(element.asType()));
        }
        return ret;
    }
    
    public Set<TypeVariable> findAllTypeVariables(List<? extends TypeMirror> types) {
        final Set<TypeVariable> ret = new LinkedHashSet<TypeVariable>();
        for (TypeMirror type : types) {
            ret.addAll(findAllTypeVariables(type));
        }
        return ret;
    }

    public Set<TypeVariable> findAllTypeVariables(TypeMirror rootType) {
        final Set<TypeVariable> ret = new LinkedHashSet<TypeVariable>();

        final Set<TypeMirror> visited = new HashSet<TypeMirror>();
        final LinkedList<TypeMirror> queue = new LinkedList<TypeMirror>();
        queue.add(rootType);

        TypeMirror current;
        while ((current = queue.poll()) != null) {
            if (!visited.add(current)) {
                //don't visit types more than onces.
                continue;
            }

            if (current.getKind() == TypeKind.TYPEVAR) {
                final TypeVariable typeVariable = (TypeVariable) current;
                final TypeMirror lowerBound = typeVariable.getLowerBound();
                final TypeMirror upperBound = typeVariable.getUpperBound();

                if (ret.add(typeVariable)) {
                    if (lowerBound.getKind() != TypeKind.NULL && !isObjectType(lowerBound)) {
                        queue.add(lowerBound);
                    }

                    if (!isObjectType(upperBound)) {
                        queue.add(upperBound);
                    }
                }

            } else if (current.getKind() == TypeKind.DECLARED) {
                final DeclaredType declaredType = (DeclaredType) current;

                for (TypeMirror type : declaredType.getTypeArguments()) {
                    queue.add(type);
                }

                /*
                 for (TypeMirror directSupertype : typeUtils.directSupertypes(current)) {
                 if(isObjectType(directSupertype)) {
                 continue;
                 }
                 queue.add(directSupertype);
                 }
                 */
            } else if (current.getKind() == TypeKind.WILDCARD) {
                final WildcardType wildcardType = (WildcardType) current;
                if (wildcardType.getExtendsBound() != null) {
                    queue.add(wildcardType.getExtendsBound());

                } else if (wildcardType.getSuperBound() != null) {
                    queue.add(wildcardType.getSuperBound());
                }

            } else if (current.getKind() == TypeKind.ARRAY) {
                final ArrayType arrayType = (ArrayType) current;
                final TypeMirror componentType = arrayType.getComponentType();
                queue.add(componentType);
            }
        }

        return ret;
    }

    public String typeToString(TypeMirror rootType, Map<TypeVariable, String> typeVariableNameMapping, boolean suppressTypeVarDecl) {
        final StringBuilder ret = new StringBuilder();
        final Set<TypeMirror> definedTypeArgs = new HashSet<TypeMirror>();

        final LinkedList<Context> queue = new LinkedList<Context>();
        queue.add(createTypeContext(rootType));

        Context context;
        while ((context = queue.poll()) != null) {
            if (context instanceof TypeContext) {
                typeToString(queue, (TypeContext) context, definedTypeArgs,
                        typeVariableNameMapping, suppressTypeVarDecl);
            } else {
                ret.append(context.toString());
            }
        }
        return ret.toString();
    }

    private TypeContext createTypeContext(TypeMirror type) {
        switch (type.getKind()) {
            case TYPEVAR:
                return new TypeVarContext(type);

            case DECLARED:
                return new DeclaredTypeContext(type);

            case ARRAY:
                return new ArrayTypeContext(type);

            case WILDCARD:
                return new WildCardTypeContext(type);

            default:
                return new TypeContext(type);
        }
    }

    private void typeToString(LinkedList<Context> queue, TypeContext typeContext,
            Set<TypeMirror> definedTypeArgs, Map<TypeVariable, String> typeVariableNameMapping,
            boolean suppressTypeVarDecl) {
        final TypeMirror type = typeContext.getType();

        if (type.getKind() == TypeKind.TYPEVAR) {
            final TypeVariable typeVariable = (TypeVariable) type;

            //bound
            if (definedTypeArgs.add(typeVariable) && (typeContext instanceof TypeVarContext) && !suppressTypeVarDecl) {
                final TypeMirror upperBound = isObjectType(typeVariable.getUpperBound()) ? typeUtils.getNullType() : typeVariable.getUpperBound();
                final TypeMirror lowerBound = isObjectType(typeVariable.getLowerBound()) ? typeUtils.getNullType() : typeVariable.getLowerBound();

                if (upperBound.getKind() != TypeKind.NULL && definedTypeArgs.add(upperBound)) {
                    queue.addFirst(new TypeVarContext(upperBound));
                    queue.addFirst(TypeBoundContext.UPPER_BOUND);

                } else if (lowerBound.getKind() != TypeKind.NULL && definedTypeArgs.add(lowerBound)) {
                    queue.addFirst(new TypeVarContext(lowerBound));
                    queue.addFirst(TypeBoundContext.LOWER_BOUND);
                }
            }

            //type variable name
            String typeVariableName = typeVariable.asElement().getSimpleName().toString();
            if (typeVariableNameMapping.containsKey(typeVariable)) {
                typeVariableName = typeVariableNameMapping.get(typeVariable);
            }
            queue.addFirst(new CustomContext(typeVariableName));

        } else if (type.getKind() == TypeKind.DECLARED) {
            final DeclaredType declaredType = (DeclaredType) type;

            //bounds
            final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            final List<? extends TypeMirror> intersectionTypes = typeUtils.directSupertypes(declaredType);

            if (!typeArguments.isEmpty()) {
                queue.addFirst(TypeBoundContext.CLOSED_BRACKET);
                for (int i = typeArguments.size() - 1; i >= 0; i--) {
                    queue.addFirst(new DeclaredTypeContext(typeArguments.get(i)));
                    if (i != 0) {
                        queue.addFirst(TypeBoundContext.COMMA);
                    }
                }
                queue.addFirst(TypeBoundContext.OPEN_BRACKET);

            } else if (intersectionTypes.size() > 1 && (typeContext instanceof TypeVarContext)) {
                //check for intersection types (@see DeclaredType)
                //example: T extends Number & Comparable<T>
                for (int i = intersectionTypes.size() - 1; i >= 0; i--) {
                    final TypeMirror intersectionType = intersectionTypes.get(i);
                    queue.addFirst(new DeclaredTypeContext(intersectionType));
                    
                    if (i != 0) {
                        queue.addFirst(TypeBoundContext.INTERSECTION_BOUND);
                    }
                }
            }

            //string representation
            final TypeElement typeElement = (TypeElement) declaredType.asElement();
            queue.addFirst(new CustomContext(rawTypeElementToString(typeElement)));

        } else if (type.getKind() == TypeKind.WILDCARD) {
            final WildcardType wildcardType = (WildcardType) type;

            //bounds
            final TypeMirror extendsBound = wildcardType.getExtendsBound();
            final TypeMirror superBound = wildcardType.getSuperBound();
            if (extendsBound != null) {
                queue.addFirst(new WildCardTypeContext(extendsBound));
                queue.addFirst(TypeBoundContext.UPPER_BOUND);

            } else if (superBound != null) {
                queue.addFirst(new WildCardTypeContext(superBound));
                queue.addFirst(TypeBoundContext.LOWER_BOUND);
            }
            queue.addFirst(TypeBoundContext.WILDCARD);

        } else if (type.getKind() == TypeKind.ARRAY) {
            final ArrayType arrayType = (ArrayType) type;
            final TypeMirror componentType = arrayType.getComponentType();
            queue.addFirst(TypeBoundContext.ARRAY_BRACKETS);

            if (componentType.getKind().isPrimitive()) {
                queue.addFirst(new CustomContext(rawTypeMirrorToString(componentType)));
            } else {
                queue.addFirst(new ArrayTypeContext(componentType));
            }

        } else if (type.getKind().isPrimitive()) {
            final PrimitiveType primitive = (PrimitiveType) type;
            queue.addFirst(new CustomContext(rawTypeMirrorToString(boxTypeIfNeeded(primitive))));
            
        } else if (type.getKind() == TypeKind.VOID) {
            queue.addFirst(new CustomContext("void"));
            
        } /*else {
            //Error (maybe an unknown feature of Java > 7)
            throw new UnknownTypeException(type, String.format("Type '%s' cannot be resolved.", type));
        }*/
    }

    private String rawTypeMirrorToString(TypeMirror typeMirror) {
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            return rawTypeElementToString((TypeElement) typeUtils.asElement(typeMirror));
        }
        return typeMirror.toString();
    }

    public String rawTypeElementToString(TypeElement typeElement) {
        final PackageElement packageOfType = elementUtils.getPackageOf(typeElement);
        final String qualifiedName = typeElement.getQualifiedName().toString();

        if (javaLangPackage.equals(packageOfType)) {
            if (typeElement.getNestingKind().isNested()) {
                return qualifiedName.substring(packageOfType.getQualifiedName().toString().length() + 1);
            }
            return typeElement.getSimpleName().toString();
        }
        return qualifiedName;
    }

    private boolean isObjectType(TypeMirror type) {
        return typeUtils.isSameType(objectTypeElement.asType(), type);
    }
}
