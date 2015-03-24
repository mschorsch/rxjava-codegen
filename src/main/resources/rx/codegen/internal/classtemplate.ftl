<#include "${licensetext}">
package ${packagename};

import javax.annotation.Generated;
import rx.functions.*;

/**
 * Do NOT modify this code. The content of this class is always regenerated.
 */
@Generated(value = "${originatingAnnotationName}", comments = "${originatingAnnotationComments}")
public abstract class ${classname} {

<#list methodSpecifications as mspec>
<#assign methodname = mspec.getGeneratedMethodname()>
<#assign calledMethodname = mspec.getCalledMethodname()>
<#assign javadoc = mspec.getJavadoc()>
<#assign generics = mspec.getGenericsDecl()>
<#assign returnType =  mspec.getReturnTypeOfCallMethod()>
<#assign callType = mspec.getCalledType()>
<#assign varlist = mspec.getParameters()>
<#assign returnVal = generateReturn(callType, mspec.isAction(), varlist, returnType)>
<#if javadoc?has_content>${javadoc}</#if><#t>
<#if callType.isConstructorCall()>
    ${mspec.getModifier()} static <@generateGenericDecls list=generics/> ${returnVal} ${methodname} () {
        return new ${returnVal}() {

            @Override
            public ${returnType} call(<@generateVariableDecls varlist=varlist/>) {
                <@fillReturn p=mspec/> new ${originatingClassnameWithGenerics}(<@generateVariables varlist=varlist/>);
            }
        };
    }
<#elseif callType.isObjectMethodCall()>
    ${mspec.getModifier()} static <@generateGenericDecls list=generics/> ${returnVal} ${methodname} (<@generateVariableDecls varlist=varlist/>) {
        return new ${returnVal}() {

            @Override
            public ${returnType} call(final ${originatingClassnameWithGenerics} obj) {
                <@fillReturn p=mspec/> obj.${calledMethodname}(<@generateVariables varlist=varlist/>);
            }
        };
    }
<#elseif callType.isStaticMethodCall()>
    ${mspec.getModifier()} static <@generateGenericDecls list=generics/> ${returnVal} ${methodname} () {
        return new ${returnVal}() {

            @Override
            public ${returnType} call(<@generateVariableDecls varlist=varlist/>) {
                <@fillReturn p=mspec/> ${originatingClassname}.${calledMethodname}(<@generateVariables varlist=varlist/>);
            }
        };
    }
</#if>
</#list>
}

<#-- ---------------------- -->
<#-- ReturnType             -->
<#-- ---------------------- -->
<#function generateReturn callType isAction varlist returnType>
    <#if callType.isStaticMethodCall() || callType.isConstructorCall()>
        <#return generateStaticMethodReturn(isAction, varlist, returnType)>
    <#else>
        <#return generateObjMethodReturn(isAction, returnType)>
    </#if>
</#function>

<#function generateObjMethodReturn isAction returnType>
    <#if isAction>
        <#return "Action1<${originatingClassnameWithGenerics}>">
    <#else>
        <#return "Func1<${originatingClassnameWithGenerics}, ${returnType}>">
    </#if>
</#function>

<#function generateStaticMethodReturn isAction varlist returnType>
    <#local size = varlist?size>
    <#local ret>
        <#if isAction>
            <#if size gt 9>
                ActionN<#t>
            <#elseif size == 0>
                Action0<#t>
            <#else>
                Action${size}<<#t>
                <#list varlist as var>
                    ${var.getType()}<#if var_has_next>, </#if><#t>
                </#list>
                ><#t>
            </#if>
        <#else>
            <#if size gt 9>
                FuncN<${returnType}><#t>
            <#elseif size == 0>
                Func0<${returnType}><#t>
            <#else>
                Func${size}<<#t>
                <#list varlist as var>
                    ${var.getType()}<#if var_has_next>, </#if><#t>
                </#list>
                , ${returnType}><#t>
            </#if>
        </#if>
    </#local>
    <#return ret>
</#function>


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
