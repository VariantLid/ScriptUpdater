package ru.variantsoft.ScriptUpdater.api.base;

//   Core.java
//   Модуль ядра, содержит несколько вспомогательных классов и класс Core, которая является синглетоном ядра системы.
//   а также глобальные переменные и глобальные константы, для работы базовой функциональности.
//   Класс Core содержит вспомогательный класс SysLogger, пишет все хорошее и плохое!
//
//   @author "Variant Soft Co." 2018

import ru.variantsoft.ScriptUpdater.api.DataModule.DataBase;
import ru.variantsoft.ScriptUpdater.api.DataModule.SQLPerformance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.firebirdsql.management.MaintenanceManager.SHUTDOWN_FORCE;

public class Core {

    // public declarations
    //Resoursestring Все константы и строчные обозначения
    public static ResourceBundle cnst;
    // Лог скриптера
    public static DataBase db;
    public final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // private declarations
    private static SQLPerformance sql;
    private static final Core ourInstance = new Core();

    // protected declarations

    //--implementation---------------------------

    // constructor
    private Core() {
        db = new DataBase();
        sql = new SQLPerformance();
        setCnst();
    }


    public static Core getInstance() {
        return ourInstance;
    }

    // Чтение ini с настройками
    public static boolean prepare() {
        ReadProps props = new ReadProps();
        return props.DoRead();
    }

    public static void Updating(String ADBName) throws SQLException {
        logger.info(Core.cnst.getString("cnstConnetToDB"));
        ReadProps.CurrentDBName = ADBName;

        try {
            Core.db.shutdownDatabase(SHUTDOWN_FORCE, 1);// Базу в даун
            logger.info("Database SHUTDOWN_FORCE");

            if (DoConnect())  // Открываем соединение с базой
                DoUpdate();    // Приступаем к операции обновления


        } finally {
            Core.db.bringDatabaseOnline(); // DEBUG             // Базу на бочку
            logger.info("Database ONLINE");
            closeAccessToFB(); // Закрываем соединение с базой
        }
    }

    private static void DoUpdate() throws SQLException {
        String FilePath;

        logger.info(Core.cnst.getString("cnstDoUpdate"));

        for (int i = 0; i < ReadProps.UpDateFiles.length; i++) {// Пробег по списку файлов обновления
            FilePath = GetFilePath(ReadProps.UpDateFiles[i]);

            if (!Objects.equals(FilePath, "")) {
                logger.info(String.format("Executing batch %s", ReadProps.UpDateFiles[i]));
                sql.execBlock(FilePath);
            }
        }
    }

    private static boolean DoConnect() {
        if (db.DoConnect()) {
            logger.info(Core.cnst.getString("cnstAccessGot"));
            return true;
        } else {
            logger.info(Core.cnst.getString("cnstAccessErr"));
            System.exit(1);
            return false;
        }
    }

    private static void closeAccessToFB() {
        db.CloseConnection();
        logger.info(Core.cnst.getString("cnstAccessOff"));
    }

    private static String GetFilePath(String AFileName) {
        //String DatabaseVersion = sql.getDatabaseVersion();

        String sPath = String.format("%s%s%s", ReadProps.UpDatePath, "\\", AFileName);
        //String sPath = getFilePathInRoot(AFileName);
        File f = new File(sPath);
        if (!f.exists()) {
            logger.info(String.format("No such file or directory %s", AFileName));
            sPath = "";
        }
        return sPath;
    }

    // Инициализация ресурса констант
    private void setCnst() {
        cnst = ResourceBundle.getBundle("msgConsts_en", Locale.ENGLISH);
    }
}
