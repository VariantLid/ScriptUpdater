package ru.variantsoft.ScriptUpdater.api.DataModule;

//   SqlParser.java
//   Модуль SQL парсера системы, содержит класс SqlParser, который осуществляет разбор на комманды файла metadata.sql.
//
//   @author "Variant Soft Co." 2018


import ru.variantsoft.ScriptUpdater.api.base.Core;
import ru.variantsoft.ScriptUpdater.api.base.ReadProps;
import static ru.variantsoft.ScriptUpdater.api.Utils.FileUtils.*;

import java.io.*;
import java.sql.SQLException;
import java.util.Objects;



class SqlParser {

/*
    SET TERM ^ ; -- Begin Block
    CREATE PROCEDURE DOCUMENT_BASE_MAKE_KNP(BASEID TYPE OF TBIGINT, DOCID TYPE OF TBIGINT, BASETYPEID TYPE OF TBIGINT)
    AS
    BEGIN EXIT; END
    ^
    SET TERM ; ^ -- End Block
*/


    // public declarations

    // private declarations
    private final StringBuilder SqlCommand;
    private String UpdateFile;
    private boolean CanCommit = false;
    private boolean isCommentOn = false;
    private boolean isExecuteBlock = false;
    private FileWriter writer;

    // protected declarations


    //--implementation---------------------------

    // constructor
    public SqlParser() {
        SqlCommand = new StringBuilder();
    }
    // constructor

    public void setUpDateFile(String upDateFile) {
        UpdateFile = upDateFile;

        try {
            GetTerm(UpdateFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void GetSqlCommand(String AValue) {
        setExecuteBlock(AValue);
        setCommentOn(AValue);

        if (!AValue.toUpperCase().startsWith("SET")) {

            if (Objects.equals(AValue.toUpperCase(), "COMMIT;") || Objects.equals(AValue.toUpperCase(), "COMMIT WORK;"))
                AValue = "";

            if (!Objects.equals(AValue, ReadProps.Term)) {
                SqlCommand.append(AValue);
                SqlCommand.append("\n");// И даже не думай менять символ "\n" на любой другой
            }
            setCanCommit(AValue);
        }
    }

    public void execSql() {
        CreateSqlFile(getFilePathInRoot(String.format("STATEMENT_NOTE_%s", getUpdateFileName(UpdateFile))));//DEBUG

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(UpdateFile), "WINDOWS-1251"))) {
            String Sqlline;
            while ((Sqlline = bufferedReader.readLine()) != null) {
                GetSqlCommand(Sqlline);
                if (CanCommit) {
                    try {
                        Core.db.statement.execute(SqlCommand.toString());
                        Core.db.connection.commit();

                    } catch (SQLException sqx) {
                        int vendorCode = sqx.getErrorCode();

                        try {
                            Core.db.connection.rollback();
                            switch (vendorCode) {
                                case 335544569:// Скрипт не содержит выполняемых операторов
                                    //sqx.fillInStackTrace();
                                    Core.logger.info(String.format("Скрипт не содержит выполняемых операторов: %s \n %s", sqx.getMessage(), SqlCommand.toString()));
                                    break;
                                case 335544351:// В случае если выполнили, и объект уже есть в базе (процедура, поле, таблица и прочее) игнорируем
                                    Core.logger.info(sqx.getMessage());
                                    break;
                                default:
                                    if (vendorCode != 0)
                                        Core.logger.info(String.format("Error execute batch : %s \r %s", sqx.getMessage(), SqlCommand.toString()));
                                    break;
                            }
                        } catch (SQLException e) {
                            Core.logger.info(String.format("Rollback failed: %s", e.getMessage()));
                        }
                    }
                    if (SqlCommand.length() > 1)
                        SqlWriter(SqlCommand.toString());//DEBUG

                    ClearSqlCommand();
                }
            }

        } catch (IOException e) {
            Core.logger.info(e.getMessage());
        }
        SqlFlush();//DEBUG
        ClearSqlCommand();
    }

    private void setCanCommit(String AValue) {
        CanCommit = false;

        if (isExecuteBlock) {
            if ((Objects.equals(AValue, ReadProps.Term)))
                CanCommit = (SqlCommand.length() > 1);  // final line of a block statement

            if ((Objects.equals(AValue.toUpperCase(), "END;"))) {
                CanCommit = (SqlCommand.length() > 1);  // final line of a block statement
                isExecuteBlock = false;
            }

        } else {
            if (isCommentOn) {
                if (AValue.trim().endsWith("';"))// final line of a comment statement
                    CanCommit = (SqlCommand.length() > 1);

            } else if (AValue.trim().endsWith(";"))
                CanCommit = (SqlCommand.length() > 1);
        }
    }


    private void setExecuteBlock(String AValue) {
        if (Objects.equals(AValue, (String.format("SET TERM %s ;", ReadProps.Term)))) // Begin Block объект (процедура, таблица, триггер и прочее) Если будет в нижнем регистре, вся процедура поломается думай, возможно по зарезервированным словам
            isExecuteBlock = true;

        if ((AValue.toUpperCase().startsWith("EXECUTE BLOCK")))
            isExecuteBlock = true;

        if (Objects.equals(AValue, (String.format("SET TERM ; %s", ReadProps.Term)))) { // End Block объекта (процедура, таблица, триггер и прочее)
            isExecuteBlock = false;
        }
    }

    private void setCommentOn(String AValue) {
        if (AValue.toUpperCase().startsWith("COMMENT ON"))
            isCommentOn = true;
    }

    private void ClearSqlCommand() {
        SqlCommand.delete(0, SqlCommand.length());
        isCommentOn = false;
    }

    // --Commented out by Inspection START (16.03.2018 11:35):
    private void CreateSqlFile(String AFileName) {

        try {
            writer = new FileWriter(AFileName, false);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
// --Commented out by Inspection STOP (16.03.2018 11:35)

    // --Commented out by Inspection START (16.03.2018 11:35):
    private void SqlWriter(String AValue) {

        try {
            writer.append("______________________________________________________________________________________________ Begin Statement --\r");
            writer.append(AValue);
            writer.append("______________________________________________________________________________________________ End Statement --\r\r\r");
        } catch (IOException ex) {

            System.out.println(ex.getMessage());
        }
    }
// --Commented out by Inspection STOP (16.03.2018 11:35)

    // --Commented out by Inspection START (16.03.2018 11:35):
    private void SqlFlush() {

        try {
            writer.flush();
            writer.close();
        } catch (IOException ex) {

            System.out.println(ex.getMessage());
        }
    }
// --Commented out by Inspection STOP (16.03.2018 11:35)

}
