package com.easysql.engine.dsl;

import com.easysql.engine.model.Template;

import java.util.ArrayList;
import java.util.List;

public class OptionsClause {
    private final Template.Options options = new Template.Options();

    public static OptionsClause create() { return new OptionsClause(); }

    public OptionsClause timeoutMs(int ms) { options.timeoutMs = ms; return this; }
    public OptionsClause maxRows(int maxRows) { options.maxRows = maxRows; return this; }
    public OptionsClause scanPartitions(int scanPartitions) { options.scanPartitions = scanPartitions; return this; }
    public OptionsClause fetchSize(int fetchSize) { options.fetchSize = fetchSize; return this; }
    public OptionsClause readOnly(boolean readOnly) { options.readOnly = readOnly; return this; }
    public OptionsClause hints(List<String> hints) { options.hints = hints; return this; }
    public OptionsClause hint(String hint) {
        if (options.hints == null) options.hints = new ArrayList<>();
        options.hints.add(hint);
        return this;
    }

    Template.Options build() { return options; }
}