package ru.variantsoft.ScriptUpdater.api.base;


//  ReadProps.java
//  Чтение настроек скриптера находящихся в INI
//  @author "Variant Soft Co." 2018

import static ru.variantsoft.ScriptUpdater.api.Utils.FileUtils.findFileOnClassPath;

import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;
import java.util.Properties;

public class ReadProps {

    // public declarations
    public static String CurrentDBName;
    public static String[] ListDB;
    public static String[] UpDateFiles;
    public static String UpDatePath;
    public static String Alias;
    public static String UserName;
    public static String Password;
    public static String RoleName;
    public static String CharSet;
    public static String JDBC;
    public static String JDBCPort;
    public static String FBDriver;
    public static String strConnect;
    public static String Term;

    // protected declarations

    // private declarations
    private String IniFileName;

    //--implementation---------------------------

    public ReadProps() {
        setIniFileName();
        Term = "";
    }

    // Читаем ini с настройками
    public boolean DoRead() {
        boolean ret = false;
        File classpathFile = findFileOnClassPath(getIniFileName());

        if (Objects.requireNonNull(classpathFile).exists()) {
            try {
                Properties ini = new Properties();
                ini.load(new FileInputStream(classpathFile));
                String sDelim = ini.getProperty(Core.cnst.getString("cnstDelimiter"));
                String sIni = ini.getProperty(Core.cnst.getString("cnstDatabases"));
                String sUpdFile = ini.getProperty(Core.cnst.getString("cnstUpDateFile"));
                UserName = ini.getProperty(Core.cnst.getString("cnstUserName"));
                Password = ini.getProperty(Core.cnst.getString("cnstPassword"));
                RoleName = ini.getProperty(Core.cnst.getString("cnstRoleName"));
                CharSet = ini.getProperty(Core.cnst.getString("cnstCharSet"));
                JDBC = ini.getProperty(Core.cnst.getString("cnstJDBC"));
                JDBCPort = ini.getProperty(Core.cnst.getString("cnstJDBCPort"));
                FBDriver = ini.getProperty(Core.cnst.getString("cnstFBDriver"));
                UpDatePath = ini.getProperty(Core.cnst.getString("cnstUpDatePath"));
                Alias = ini.getProperty(Core.cnst.getString("cnstAlias"));
                ListDB = GetList(sIni, sDelim);
                UpDateFiles = GetList(sUpdFile, sDelim);
                ret = !ini.isEmpty();
            } catch (Exception e) {
                Core.logger.info(e.getMessage());
            }
        }
        return ret;
    }

    // Список баз обновления из INI
    private String[] GetList(String AValue, String ADelim) {
        return AValue.split(ADelim);
    }

    // Отдаем имя файла ini с настройками
    private String getIniFileName() {
        return IniFileName;
    }

    // Указываем ini файл настроек скриптера
    private void setIniFileName() {
        IniFileName = Core.cnst.getString("cnstIniFile");
    }

}
