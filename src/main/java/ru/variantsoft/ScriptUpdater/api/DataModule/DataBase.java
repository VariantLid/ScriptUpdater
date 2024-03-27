package ru.variantsoft.ScriptUpdater.api.DataModule;

//   DataBase.java
//   Модуль данных системы.
//   Включает в себя соединение с БД и STATEMENT
//
//   @author "Variant Soft Co." 2018

import ru.variantsoft.ScriptUpdater.api.base.Core;
import ru.variantsoft.ScriptUpdater.api.base.ReadProps;

import org.firebirdsql.gds.*;
import org.firebirdsql.gds.impl.GDSFactory;
import org.firebirdsql.jdbc.FBSQLException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

import static org.firebirdsql.management.FBServiceManager.BUFFER_SIZE;
import static org.firebirdsql.management.MaintenanceManager.*;

public class DataBase {

    private final GDS gds = GDSFactory.getGDSForType(GDSFactory.getDefaultGDSType());
    // public declarations
    public Connection connection = null;
    public Statement statement = null;


    // private declarations

    // protected declarations

    //--implementation---------------------------

    public void CloseConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            Core.logger.info(String.format("%s - %s", Core.cnst.getString("cnstDisconnErr"), e.getMessage()));
        }
    }

    public boolean DoConnect() {
        boolean ret;
        ret = setConnectionParam();
        if (ret)
            ret = OpenConnection();

        return ret;
    }

    private boolean OpenConnection() {
        boolean ret;
        try {
            Class.forName(ReadProps.FBDriver);
        } catch (ClassNotFoundException e) {
            Core.logger.info(Core.cnst.getString("cnstDriverNotFound"));
            return false;
        }
        try {
            Properties connInfo = new Properties();

            connInfo.put("user", ReadProps.UserName);
            connInfo.put("password", ReadProps.Password);
            connInfo.put("charSet", ReadProps.CharSet);
            connInfo.put("Role", ReadProps.RoleName);

            connection = java.sql.DriverManager.getConnection(ReadProps.strConnect, connInfo);

            Core.db.connection.setAutoCommit(false);
            Core.db.statement = Core.db.connection.createStatement();

            ret = connection.isValid(1);

            Core.logger.info(Core.cnst.getString("cnstConnJDBC"));

        } catch (SQLException e) {
            int vendorCode = e.getErrorCode();

            switch (vendorCode) {
                case 335544344:// В случае если файл базы данных отсутствует
                    Core.logger.info(e.getMessage());
                    break;
                default:
                    Core.logger.info(Core.cnst.getString("cnstConnJDBCErr"));
                    break;
            }

            return false;
        }
        return ret;
    }

    // Задаем параметры подключения к базе
    private boolean setConnectionParam() {
        if (Objects.equals(ReadProps.strConnect, "")) {
            Core.logger.info(Core.cnst.getString("cnstNoPathDB"));
            return false;
        }
        ReadProps.strConnect = String.format("%s%s%s%s", ReadProps.JDBC, ReadProps.Alias, ReadProps.JDBCPort, ReadProps.CurrentDBName);
        //System.out.println(Core.strConnect);
        Core.logger.info(ReadProps.strConnect);

        return true;
    }

    private ServiceRequestBuffer createDefaultPropertiesSRB() {
        return createPropertiesSRB(0);
    }


    // Database Down / Online
    public void shutdownDatabase(int shutdownMode, int timeout)
            throws SQLException {

        // SHUTDOWN_ATTACH
        // SHUTDOWN_TRANSACTIONAL
        // SHUTDOWN_FORCE

        if (shutdownMode != SHUTDOWN_ATTACH
                && shutdownMode != SHUTDOWN_TRANSACTIONAL
                && shutdownMode != SHUTDOWN_FORCE) {
            Core.logger.info("Shutdown mode must be one of: SHUTDOWN_ATTACH, SHUTDOWN_TRANSACTIONAL, SHUTDOWN_FORCE");
        }
        if (timeout < 0) {
            Core.logger.info("Timeout must be >= 0");
        }


        ServiceRequestBuffer srb = createDefaultPropertiesSRB();
        srb.addArgument(shutdownMode, timeout);
        executeServicesOperation(srb);
    }

    public void bringDatabaseOnline() throws SQLException {
        executePropertiesOperation();
    }

    private String getServiceName() {
        return String.format("%s%s%s", ReadProps.Alias, ReadProps.JDBCPort, "service_mgr");
    }

    private IscSvcHandle attachServiceManager(GDS gds) throws GDSException {
        ServiceParameterBuffer serviceParameterBuffer =
                gds.createServiceParameterBuffer();


        serviceParameterBuffer.addArgument(
                ISCConstants.isc_spb_user_name, ReadProps.UserName);


        serviceParameterBuffer.addArgument(
                ISCConstants.isc_spb_password, ReadProps.Password);

        serviceParameterBuffer.addArgument(
                ISCConstants.isc_spb_dummy_packet_interval, new byte[]{120, 10, 0, 0});

        final IscSvcHandle handle = gds.createIscSvcHandle();
        gds.iscServiceAttach(getServiceName(), handle, serviceParameterBuffer);

        return handle;
    }

    private void executePropertiesOperation()
            throws SQLException {
        ServiceRequestBuffer srb = createPropertiesSRB(ISCConstants.isc_spb_prp_db_online);
        executeServicesOperation(srb);
    }

    private ServiceRequestBuffer createPropertiesSRB(int options) {
        return createRequestBuffer(
                options);
    }

    private void queueService(GDS gds, IscSvcHandle handle) throws GDSException, FBSQLException {
        ServiceRequestBuffer infoSRB = gds.createServiceRequestBuffer(ISCConstants.isc_info_svc_to_eof);

        int bufferSize = BUFFER_SIZE;
        byte[] buffer = new byte[bufferSize];

        boolean processing = true;
        while (processing) {
            gds.iscServiceQuery(handle, gds.createServiceParameterBuffer(), infoSRB, buffer);

            switch (buffer[0]) {

                case ISCConstants.isc_info_svc_to_eof:

                    int dataLength = (buffer[1] & 0xff) | ((buffer[2] & 0xff) << 8);
                    if (dataLength == 0) {
                        if (buffer[3] != ISCConstants.isc_info_end)
                            throw new FBSQLException("Unexpected end of stream reached.");
                        else {
                            processing = false;
                            break;
                        }
                    }

                    Core.logger.info(String.format("%s%s", String.valueOf(buffer), "\r"));

                    break;

                case ISCConstants.isc_info_truncated:
                    bufferSize = bufferSize * 2;
                    buffer = new byte[bufferSize];
                    break;

                case ISCConstants.isc_info_end:
                    processing = false;
                    break;
            }
        }
    }

    private void detachServiceManager(GDS gds, IscSvcHandle handle) throws GDSException {
        gds.iscServiceDetach(handle);
    }


    private ServiceRequestBuffer createRequestBuffer(int options) {

        ServiceRequestBuffer srb = gds.createServiceRequestBuffer(ISCConstants.isc_action_svc_properties);

        srb.addArgument(ISCConstants.isc_spb_dbname, ReadProps.CurrentDBName);
        srb.addArgument(ISCConstants.isc_spb_options, options);
        return srb;
    }

    private void executeServicesOperation(ServiceRequestBuffer srb)
            throws FBSQLException {

        try {
            IscSvcHandle svcHandle = attachServiceManager(gds);
            try {
                gds.iscServiceStart(svcHandle, srb);
                queueService(gds, svcHandle);
            } finally {
                detachServiceManager(gds, svcHandle);
            }
        } catch (GDSException gdse) {
            throw new FBSQLException(gdse);
        }
    }

}

