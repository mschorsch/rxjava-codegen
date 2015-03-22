<#include "${licensetext}">
package ${packagename};

import javax.annotation.Generated;
import rx.functions.*;

@Generated(value = "${originatingAnnotationName}", comments = "${originatingAnnotationComments}")
public abstract class ${classname} {

<#list methodSpecifications as mspec>
<#assign javadoc = mspec.getJavadoc()>
<#assign generics = mspec.getGenerics()>
<#assign returnType =  mspec.getReturnType()>
<#assign callType=mspec.getCalledType()>
<#if javadoc?has_content>${javadoc}</#if><#t>
<#if callType.isConstructorCall()>
    ${mspec.getModifier()} static <@generateGenericDecl list=generics/> ${returnType} ${mspec.getGeneratedMethodname()} () {
        return new ${returnType}() {

            @Override
            public ${mspec.getReturnTypeOfCallMethod()} call(${mspec.getVariablesWithTypes()}) {
                <@fillReturn p=mspec/> new ${originatingClassnameWithGenerics}(${mspec.getVariables()});
            }
        };
    }
<#elseif callType.isObjectMethodCall()>
    ${mspec.getModifier()} static <@generateGenericDecl list=generics/> ${returnType} ${mspec.getGeneratedMethodname()} (${mspec.getVariablesWithTypes()}) {
        return new ${returnType}() {

            @Override
            public ${mspec.getReturnTypeOfCallMethod()} call(final ${originatingClassnameWithGenerics} obj) {
                <@fillReturn p=mspec/> obj.${mspec.getCalledMethodname()}(${mspec.getVariables()});
            }
        };
    }
<#elseif callType.isStaticMethodCall()>
    ${mspec.getModifier()} static <@generateGenericDecl list=generics/> ${returnType} ${mspec.getGeneratedMethodname()} () {
        return new ${returnType}() {

            @Override
            public ${mspec.getReturnTypeOfCallMethod()} call(${mspec.getVariablesWithTypes()}) {
                <@fillReturn p=mspec/> ${originatingClassname}.${mspec.getCalledMethodname()}(${mspec.getVariables()});
            }
        };
    }
</#if>
</#list>
}

<#macro generateGenericDecl list>
    <#if list?has_content><</#if>${list?join(", ", "", ">")}<#t>
</#macro>

<#macro fillReturn p>
    <#if !p.isAction()>
        return<#t>
    </#if>
</#macro>
