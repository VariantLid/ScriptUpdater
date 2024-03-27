package ru.variantsoft.ScriptUpdater.api.DataModule;

import ru.variantsoft.ScriptUpdater.api.base.Core;

import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;

public class SQLPerformance {

    // public declarations

    // private declarations
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");
    // protected declarations

    //--implementation---------------------------

    // constructor
    public SQLPerformance() {

    }
    // constructor

    public void execBlock(String upDateFile) throws SQLException {
        long start = System.currentTimeMillis();

        SqlParser par = new SqlParser();

        par.setUpDateFile(upDateFile);
        par.execSql();
        outputQueryStats(Core.db.statement, System.currentTimeMillis() - start);
    }

    private void outputQueryStats(Statement statement, long ms) throws SQLException {
        Throwable warning = statement.getWarnings();
        if (warning != null)
            Core.logger.info(String.format("- SERVER: %s", warning.getMessage()));

        String timeString;
        if (ms < 1000)
            timeString = ms + " ms";
        else if (ms < 60000)
            timeString = decimalFormat.format(ms / 1000d) + " seconds";
        else if (ms < 3600000)
            timeString = decimalFormat.format(ms / 60000d) + " minutes";
        else
            timeString = decimalFormat.format(ms / 3600000d) + " hours";

        Core.logger.info(String.format(" - Batch completed in %s", timeString));
    }

}
