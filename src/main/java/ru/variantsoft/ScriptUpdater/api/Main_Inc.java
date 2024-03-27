package ru.variantsoft.ScriptUpdater.api;

//
//  :$ Модуль реализции для базового юнита смотри Main.java
//   @author "Variant Soft Co." 2018

import ru.variantsoft.ScriptUpdater.api.base.Core;
import ru.variantsoft.ScriptUpdater.api.base.ReadProps;

import java.sql.SQLException;

public class Main_Inc {
    // public declarations

    // private declarations

    // protected declarations

    //--implementation---------------------------

    public Main_Inc() {

    }

    public boolean Run() throws SQLException {
        boolean ret = Core.prepare();
        Core.logger.info(Core.cnst.getString("cnstBegin"));
        if (ret){
            for (int i = 0; i < ReadProps.ListDB.length; i++) {// Пробег по списку баз обновления
                Core.Updating(ReadProps.ListDB[i]);
            }
        }

        return ret;
    }

}
