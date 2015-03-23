<#include "${licensetext}">
package ${packagename};

import javax.annotation.Generated;
import rx.functions.*;

@Generated(value = "${originatingAnnotationName}", comments = "${originatingAnnotationComments}")
public abstract class ${classname} {

<#list methodSpecifications as mspec>
<#assign javadoc = mspec.getJavadoc()>
<#assign generics = mspec.getGenericsDecl()>
<#assign returnType =  mspec.getReturnType()>
<#assign callType=mspec.getCalledType()>
<#if javadoc?has_content>${javadoc}</#if><#t>
<#if callType.isConstructorCall()>
    ${mspec.getModifier()} static <@generateGenericDecls list=generics/> ${returnType} ${mspec.getGeneratedMethodname()} () {
        return new ${returnType}() {

            @Override
            public ${mspec.getReturnTypeOfCallMethod()} call(<@generateVariableDecls varlist=mspec.getParameters()/>) {
                <@fillReturn p=mspec/> new ${originatingClassnameWithGenerics}(<@generateVariables varlist=mspec.getParameters()/>);
            }
        };
    }
<#elseif callType.isObjectMethodCall()>
    ${mspec.getModifier()} static <@generateGenericDecls list=generics/> ${returnType} ${mspec.getGeneratedMethodname()} (<@generateVariableDecls varlist=mspec.getParameters()/>) {
        return new ${returnType}() {

            @Override
            public ${mspec.getReturnTypeOfCallMethod()} call(final ${originatingClassnameWithGenerics} obj) {
                <@fillReturn p=mspec/> obj.${mspec.getCalledMethodname()}(<@generateVariables varlist=mspec.getParameters()/>);
            }
        };
    }
<#elseif callType.isStaticMethodCall()>
    ${mspec.getModifier()} static <@generateGenericDecls list=generics/> ${returnType} ${mspec.getGeneratedMethodname()} () {
        return new ${returnType}() {

            @Override
            public ${mspec.getReturnTypeOfCallMethod()} call(<@generateVariableDecls varlist=mspec.getParameters()/>) {
                <@fillReturn p=mspec/> ${originatingClassname}.${mspec.getCalledMethodname()}(<@generateVariables varlist=mspec.getParameters()/>);
            }
        };
    }
</#if>
</#list>
}

<#-- ---------------------- -->
<#-- Variables              -->
<#-- ---------------------- -->
<#macro generateVariables varlist>
    <#list varlist as var>
        <#if varlist?size gt 9>
            (${var.getType()}) ${var.getName()}<#if var_has_next>, </#if><#t>
        <#else>
            (${var.getUnboxedType()}) ${var.getName()}<#if var_has_next>, </#if><#t>
        </#if>
    </#list>
</#macro>

<#-- ---------------------- -->
<#-- Parameter declarations -->
<#-- ---------------------- -->
<#macro generateVariableDecls varlist>
    <#if varlist?size gt 9>
        final Object... args<#t>
    <#else>
        <#list varlist as var>
            <@generateVariableDecl var=var/><#if var_has_next>, </#if><#t>
        </#list>
    </#if>
</#macro>

<#macro generateVariableDecl var>
    final ${var.getType()} ${var.getName()}<#t>
</#macro>

<#-- --------------------- -->
<#-- Generics declarations -->
<#-- --------------------- -->
<#macro generateGenericDecls list>
    <#if list?has_content><</#if>${list?join(", ", "", ">")}<#t>
</#macro>

<#macro fillReturn p>
    <#if !p.isAction()>
        return<#t>
    </#if>
</#macro>
