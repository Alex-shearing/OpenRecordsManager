package com.openrecordsmanager.resources;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.api.expression.ExpressionBuilder;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.Record;
import com.openrecordsmanager.model.RecordPropertyValue;
import com.openrecordsmanager.model.User;
import com.openrecordsmanager.model.repositories.ListElementRepository;
import com.openrecordsmanager.resources.types.ResourceTypes;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.CelProtoTypes;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ExpressionsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionsService.class);

    private final ResourceCatalog registry;
    private final CelCompiler celCompiler;
    private final CelRuntime celRuntime;

    public ExpressionsService(ResourceCatalog registry, ListElementRepository listElementRepo) {
        this.registry = registry;
        this.celCompiler = CelCompilerFactory.standardCelCompilerBuilder()
                .addVar("value", SimpleType.DYN)
                .addVar("principal", CelProtoTypes.createMap(CelProtoTypes.STRING, CelProtoTypes.DYN))
                .addVar("resource", CelProtoTypes.createMap(CelProtoTypes.STRING, CelProtoTypes.DYN))
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
                .addFunctionDeclarations(getCompilerDeclaration())
                .build();
        this.celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .addFunctionBindings(getRuntimeBinding(listElementRepo))
                .build();
    }

    public String buildExpression(@Nullable ExpressionBuilder builder) {
        if (builder == null || builder.filter().isBlank()) {
            return null;
        }
        return MessageFormat.format(
                builder.filter(),
                Arrays.stream(builder.dependencies())
                        .map(property -> "'" + registry.getResourceId(ResourceTypes.PROPERTY, property) + "'")
                        .toArray()
        );
    }

    public boolean checkPropertyExpression(UUID id, String filter, Object value, User user) {
        return checkPropertyExpression(id, filter, value, user, null);
    }

    public boolean checkPropertyExpression(UUID id, String filter, @Nullable Object value, User user, @Nullable Record record) {
        if (filter == null || filter.isBlank()) {
            LOGGER.trace("no filter provided for property: {}", id);
            return true;
        }

        LOGGER.info("using filter '{}' for property: {}", filter, id);

        try {
            CelAbstractSyntaxTree ast = this.celCompiler.compile(filter).getAst();
            CelRuntime.Program program = this.celRuntime.createProgram(ast);

            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            builder.put("principal", user.toPropertyMap());
            if (value != null) builder.put("value", value);
            if (record != null) builder.put("resource", record.toPropertyMap());

            Object result = program.eval(builder.build());
            if (result instanceof Boolean) {
                return (Boolean) result;
            }

            LOGGER.warn("Filter '{}' did not return a boolean, returned: {}", filter, result);
            return false;
        } catch (CelValidationException e) {
            LOGGER.error("Failed to compile filter: {}", filter, e);
            return false;
        } catch (CelEvaluationException e) {
            LOGGER.error("Failed to evaluate filter: {}", filter, e);
            return false;
        }
    }

    public boolean checkSecurity(RecordPropertyValue<?> property, User user) {
        return checkPropertyExpression(property.id, property.property.securityFilter, property.value, user);
    }

    public static List<CelFunctionDecl> getCompilerDeclaration() {
        return List.of(
                // List element comparisons
                CelFunctionDecl.newFunctionDeclaration(
                        "_>=_",
                        CelOverloadDecl.newGlobalOverload(
                                "greater_equals_list_element",
                                SimpleType.BOOL,
                                SimpleType.DYN,
                                SimpleType.DYN
                        )
                ),
                CelFunctionDecl.newFunctionDeclaration(
                        "_>_",
                        CelOverloadDecl.newGlobalOverload(
                                "greater_list_element",
                                SimpleType.BOOL,
                                SimpleType.DYN,
                                SimpleType.DYN
                        )
                ),

                CelFunctionDecl.newFunctionDeclaration(
                        "_<=_",
                        CelOverloadDecl.newGlobalOverload(
                                "less_equals_list_element",
                                SimpleType.BOOL,
                                SimpleType.DYN,
                                SimpleType.DYN
                        )
                ),
                CelFunctionDecl.newFunctionDeclaration(
                        "_<_",
                        CelOverloadDecl.newGlobalOverload(
                                "less_list_element",
                                SimpleType.BOOL,
                                SimpleType.DYN,
                                SimpleType.DYN
                        )
                ),

                // List element comparisons
                CelFunctionDecl.newFunctionDeclaration(
                        "list",
                        CelOverloadDecl.newGlobalOverload(
                                "get_list",
                                SimpleType.DYN,
                                SimpleType.STRING
                        )
                )
        );
    }

    public static List<CelFunctionBinding> getRuntimeBinding(ListElementRepository listElementRepo) {
        return List.of(
                // List element comparisons
                CelFunctionBinding.from(
                        "greater_equals_list_element",
                        ListElement.class, ListElement.class,
                        (first, second) -> first.index() >= second.index()
                ),
                CelFunctionBinding.from(
                        "greater_list_element",
                        ListElement.class, ListElement.class,
                        (first, second) -> first.index() > second.index()
                ),

                CelFunctionBinding.from(
                        "less_equals_list_element",
                        ListElement.class, ListElement.class,
                        (first, second) -> first.index() <= second.index()
                ),
                CelFunctionBinding.from(
                        "less_list_element",
                        ListElement.class, ListElement.class,
                        (first, second) -> first.index() < second.index()
                ),

                // Get resource
                CelFunctionBinding.from(
                        "get_list",
                        String.class,
                        (id) -> listElementRepo.findById(ResourceIdentifier.valueOf(id)).orElse(null)
                )
        );
    }
}
