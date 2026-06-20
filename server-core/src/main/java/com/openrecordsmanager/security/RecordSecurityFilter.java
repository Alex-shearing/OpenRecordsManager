package com.openrecordsmanager.security;

import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.RecordPropertyValue;
import com.openrecordsmanager.model.User;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RecordSecurityFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordSecurityFilter.class);

    private static final CelCompiler CEL_COMPILER = CelCompilerFactory.standardCelCompilerBuilder()
            .addVar("value", SimpleType.DYN)
            .addVar("user", SimpleType.ANY)
            .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
            .addFunctionDeclarations(User.getCompilerDeclaration())
            .addFunctionDeclarations(ListElement.getCompilerDeclaration())
            .build();
    private static final CelRuntime CEL_RUNTIME = CelRuntimeFactory.standardCelRuntimeBuilder()
            .addFunctionBindings(User.getRuntimeBinding())
            .addFunctionBindings(ListElement.getRuntimeBinding())
            .build();

    public static boolean securityFilter(RecordPropertyValue<?> property, User user) {
        String filter = property.property.securityFilter;
        if (filter == null || filter.isBlank()) {
            LOGGER.trace("no securityFilter for property: {}", property.id);
            return true;
        }

        LOGGER.info("using securityFilter filter: {}", filter);

        try {
            CelAbstractSyntaxTree ast = CEL_COMPILER.compile(filter).getAst();
            CelRuntime.Program program = CEL_RUNTIME.createProgram(ast);

            Object result = program.eval(Map.of("value", property.value, "user", user));
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return false;
        } catch (CelValidationException e) {
            LOGGER.error("Failed to compile filter: {}", filter, e);
            return false;
        } catch (CelEvaluationException e) {
            LOGGER.error("Failed to evaluate filter: {}", filter, e);
            return false;
        }


    }
}
