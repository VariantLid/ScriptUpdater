package ru.variantsoft.ScriptUpdater.api;

import ru.variantsoft.ScriptUpdater.api.base.Core;

import java.sql.SQLException;

//  Main.java
//  Основной юнит программы скриптера
//  @author "Variant Soft Co." 2018


public class App{
    // public declarations

    // private declarations

    // protected declarations

    //--implementation---------------------------

    public static void main(String[] args) throws SQLException {
        Main_Inc prg = new Main_Inc();

        Core.logger.info(Core.cnst.getString("cnstReadIni"));
        if (prg.Run()) {
            Core.logger.info(Core.cnst.getString("cnstEnd"));
        }
    }
}