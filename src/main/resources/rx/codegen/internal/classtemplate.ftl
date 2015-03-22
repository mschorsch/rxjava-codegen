<#include "${licensetext}">
package ${packagename};

import javax.annotation.Generated;
import rx.functions.*;

@Generated(value = "${originatingAnnotationName}", comments = "${originatingAnnotationComments}")
public abstract class ${classname} {

<#list methodSpecifications as mspec>
<#assign javadoc = mspec.getJavadoc()>
<#assign anonymousClassname =  mspec.getAnonymousClassname()>
<#assign callType=mspec.getCalledType()>
<#if javadoc?has_content>${javadoc}</#if><#t>
<#if callType.isConstructorCall()>
    ${mspec.getMethodModifier()} static ${mspec.getGenerics()} ${anonymousClassname} ${mspec.getGeneratedMethodname()} () {
        return new ${anonymousClassname}() {

            @Override
            public ${mspec.getReturnTypeOfAnonClass()} call(${mspec.getVariablesWithTypes()}) {
                return new ${originatingClassnameWithGenerics}(${mspec.getVariables()});
            }
        };
    }
<#elseif callType.isObjectMethodCall()>
    ${mspec.getMethodModifier()} static ${mspec.getGenerics()} ${anonymousClassname} ${mspec.getGeneratedMethodname()} (${mspec.getVariablesWithTypes()}) {
        return new ${anonymousClassname}() {

            @Override
            public ${mspec.getReturnTypeOfAnonClass()} call(final ${originatingClassnameWithGenerics} obj) {
                <@fillReturn p=mspec/> obj.${mspec.getCalledMethodname()}(${mspec.getVariables()});
            }
        };
    }
<#elseif callType.isStaticMethodCall()>
    ${mspec.getMethodModifier()} static ${mspec.getGenerics()} ${anonymousClassname} ${mspec.getGeneratedMethodname()} () {
        return new ${anonymousClassname}() {

            @Override
            public ${mspec.getReturnTypeOfAnonClass()} call(${mspec.getVariablesWithTypes()}) {
                <@fillReturn p=mspec/> ${originatingClassname}.${mspec.getCalledMethodname()}(${mspec.getVariables()});
            }
        };
    }
</#if>
</#list>
}

<#macro fillReturn p>
    <#if !p.isAction()>
        return<#t>
    </#if>
</#macro>
