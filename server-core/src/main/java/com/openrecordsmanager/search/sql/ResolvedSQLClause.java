package com.openrecordsmanager.search.sql;

import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.property.ObjectProperty;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface ResolvedSQLClause {
    SearchClause clause();

    PropertyType<?> type();

    String toSqlClause(
            SqlSearchSupport support,
            ObjectSearchSchema schema,
            List<@Nullable Object> params
    );

    record ResolvedBuiltinPropertySQLClause(
            SearchClause clause,
            ObjectSearchSchema.BuiltinColumn builtin
    ) implements ResolvedSQLClause {
        @Override
        public PropertyType<?> type() {
            return this.builtin.type();
        }

        @Override
        public String toSqlClause(
                SqlSearchSupport support,
                ObjectSearchSchema schema,
                List<@Nullable Object> params
        ) {
            String expr = this.builtin.column().column(support.getDialect());
            if (this.builtin.jsonStored()) {
                boolean numeric = this.builtin.type() == PropertyType.NUMBER
                        || this.builtin.type() == PropertyType.DECIMAL;

                expr = numeric
                        ? support.getDialect().jsonNumeric(expr)
                        : support.getDialect().jsonText(expr);
            }
            return support.compare(
                    expr,
                    this.builtin.type(),
                    this.clause.op(),
                    this.clause.value(),
                    params
            );
        }
    }

    record ResolvedExternalPropertySQLClause(
            SearchClause clause,
            ObjectProperty<?> property
    ) implements ResolvedSQLClause {
        @Override
        public PropertyType<?> type() {
            return this.property.getType();
        }

        @Override
        public String toSqlClause(
                SqlSearchSupport support,
                ObjectSearchSchema schema,
                List<@Nullable Object> params
        ) {
            String eavTable = schema.holderFk().tableName(support.getDialect());
            String holderFk = schema.holderFk().column(support.getDialect());
            String primaryKey = schema.primaryKey().column(support.getDialect());
            String propertyId = schema.propertyId().column(support.getDialect());

            boolean numeric = this.property.getType() == PropertyType.NUMBER
                    || this.property.getType() == PropertyType.DECIMAL;

            String extracted = numeric
                    ? support.getDialect().jsonNumeric(schema.propertyValue().column(support.getDialect()))
                    : support.getDialect().jsonText(schema.propertyValue().column(support.getDialect()));

            // Bind property id before value comparison so placeholders match SQL order.
            params.add(this.property.getId().toString());

            // Domain null = no EAV row
            if (this.clause.op() == SearchOperator.IS_NULL) {
                String present = support.compare(
                        extracted,
                        this.property.getType(),
                        SearchOperator.IS_NOT_NULL,
                        null,
                        params
                );
                return "NOT EXISTS (SELECT 1 FROM %s WHERE %s = %s AND %s = ? AND %s)".formatted(
                        eavTable,
                        holderFk,
                        primaryKey,
                        propertyId,
                        present
                );
            }

            String comparison = support.compare(
                    extracted,
                    this.property.getType(),
                    this.clause.op(),
                    this.clause.value(),
                    params
            );

            return "EXISTS (SELECT 1 FROM %s WHERE %s = %s AND %s = ? AND %s)".formatted(
                    eavTable,
                    holderFk,
                    primaryKey,
                    propertyId,
                    comparison
            );
        }
    }
}
