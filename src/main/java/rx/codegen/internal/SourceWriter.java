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
package rx.codegen.internal;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import rx.codegen.internal.spec.MethodSpec;
import rx.codegen.internal.spec.TypeSpec;
import rx.codegen.internal.util.CodegenUtil;

/**
 *
 * @author Matthias
 */
class SourceWriter {

    private static final String TEMPLATE_BASE_PACKAGE_PATH = "/rx/codegen/internal";
    private static final String TEMPLATE_NAME = "classtemplate.ftl";
    private static final String LICENSE_NAME = "license.txt";

    private final CodegenUtil util;
    private final Configuration configuration;

    public SourceWriter(CodegenUtil util) throws IOException {
        this.util = util;
        this.configuration = initConfiguration();
    }

    private static Configuration initConfiguration() {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setClassForTemplateLoading(SourceWriter.class, TEMPLATE_BASE_PACKAGE_PATH);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER); //TODO change for production to RETHROW_HANDLER
        cfg.setWhitespaceStripping(true);
        return cfg;
    }

    public void writeSourceFile(TypeSpec typeSpec, List<MethodSpec> methodSpecifications) throws IOException {
        util.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Generating %s...", typeSpec.getGeneratedQualifiedClassname()));
        writeSourceFile(typeSpec, createDataModel(typeSpec, methodSpecifications));
    }

    private Map<Object, Object> createDataModel(TypeSpec typeSpec, List<MethodSpec> methodSpecifications) {
        final Map<Object, Object> dataModel = new HashMap<Object, Object>();
        dataModel.put("licensetext", LICENSE_NAME);
        dataModel.put("packagename", typeSpec.getGeneratedPackagename());
        dataModel.put("originatingAnnotationName", typeSpec.getOriAnnotationname());
        dataModel.put("originatingAnnotationComments", typeSpec.getOriAnnotationComment());
        dataModel.put("classname", typeSpec.getGeneratedSimpleClassname());
        dataModel.put("originatingClassnameWithGenerics", typeSpec.getFQOriClassnameWithGenerics());
        dataModel.put("originatingClassname", typeSpec.getFQOriClassname());
        dataModel.put("methodSpecifications", methodSpecifications);
        return dataModel;
    }

    private void writeSourceFile(TypeSpec classInfo, Map<Object, Object> dataModel) throws IOException {
        final Template template = configuration.getTemplate(TEMPLATE_NAME);
        final JavaFileObject jfo = createJFO(classInfo);

        final Writer writer = jfo.openWriter();
        try {
            template.process(dataModel, writer);
            writer.flush();
        } catch (TemplateException ex) {
            throw new IOException(ex);
        } finally {
            writer.close();
        }
    }

    private JavaFileObject createJFO(TypeSpec classInfo) throws IOException {
        return util.getFiler().createSourceFile(classInfo.getGeneratedQualifiedClassname(),
                classInfo.getOriginatingElement(), classInfo.getTypeElement());
    }
}
