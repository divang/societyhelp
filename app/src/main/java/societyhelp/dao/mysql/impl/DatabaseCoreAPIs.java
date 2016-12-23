package societyhelp.dao.mysql.impl;

import android.util.Log;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import societyhelp.app.util.SocietyHelpConstant;
import societyhelp.core.SocietyAuthorization;
import societyhelp.dao.DatabaseConstant;
import societyhelp.dao.SocietyHelpDatabaseFactory;

public class DatabaseCoreAPIs extends Queries implements DatabaseConstant, SocietyHelpConstant {

    private String databaseDBURL;
    private String databaseUser;
    private String databasePassword;
    private boolean isInitialized;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            e.getStackTrace();
            Log.e("Error", "Driver Initialization failed", e);
        }
    }

    public DatabaseCoreAPIs(String strDatabaseDBURL, String strDatabaseUser, String strDatabasePassword) {
        init(strDatabaseDBURL, strDatabaseUser, strDatabasePassword);
        isInitialized = true;
    }

    private void init(String strDatabaseDBURL, String strDatabaseUser, String strDatabasePassword) {
        databaseDBURL = strDatabaseDBURL;
        databaseDBURL = strDatabaseDBURL;
        databaseUser = strDatabaseUser;
        databasePassword = strDatabasePassword;
    }

    public Connection getDBInstance() throws Exception {

        if (!isInitialized)
            throw new ExceptionInInitializerError("DBURL, User and Password are not initialize.");

        try {
            Connection connection = DriverManager.getConnection(databaseDBURL, databaseUser, databasePassword);
            return connection;
        } catch (Exception e) {
            throw e;
        }
    }

    private void close(Connection connection, PreparedStatement pStat, ResultSet result) {

        try {
            if (connection != null) {
                connection.close();
            }

            if (pStat != null) {
                pStat.close();
            }

            if (result != null) {
                result.close();
            }
        } catch (SQLException e) {
            Log.e("Error", "Connection close has some problem.", e);
        }
    }

    private void close(Connection connection, Statement pStat, ResultSet result) {

        try {
            if (connection != null) {
                connection.close();
            }

            if (pStat != null) {
                pStat.close();
            }

            if (result != null) {
                result.close();
            }
        } catch (SQLException e) {
            Log.e("Error", "Connection close has some problem.", e);
        }
    }

    public boolean loginDB(String userName, String password) throws Exception {

        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            /*
			select s.Database_URL, s.Database_User, s.Database_Password, s.Society_Name, l.Authorised_Activity from
			Login l
			inner join
			Society s
			on l.Society_Id = s.Society_Id
			where l.Login_Id='divangd'
			and l.Password = '1'
			and l.Status = 1
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(loginQuery);
            pStat.setString(1, userName);
            pStat.setString(2, password);
            result = pStat.executeQuery();
            if (result != null && result.next()) {
                //reset the DB URL, user and password
                String url = result.getString(1);
                String user = result.getString(2);
                String pass = result.getString(3);
                //String societyNmae = result.getString(4);
                //int societyId = result.getInt(5);
                init(url, user, pass);
                //Reinitialize the factory class, so always new instance will use these configuration.
                SocietyHelpDatabaseFactory.init(url, user, pass);
                return true;
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return false;
    }

    public void activityLoggingDB(Object activity) throws Exception {

        if (activity instanceof UserActivity) {
            UserActivity objActivity = (UserActivity) activity;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                pStat = con.prepareStatement(activityLoggingQuery);
                pStat.setString(1, objActivity.userName);
                pStat.setString(2, objActivity.mobileNo);
                pStat.setString(3, objActivity.activity.name());
                pStat.setString(4, objActivity.comment);
                pStat.setDate(5, new Date(System.currentTimeMillis()));
                res = pStat.executeQuery();

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public boolean isStatementAlreadyProcessedDB(String monthlyStatementFileName) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(isStatementProcessedQuery);
            pStat.setString(1, monthlyStatementFileName);
            res = pStat.executeQuery();
            if (res != null && res.next()) return true;

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
        return false;
    }

    public boolean uploadMonthlyTransactionsDB(Object transactions) throws Exception {

        if (transactions instanceof BankStatement) {
            BankStatement bankStatement = (BankStatement) transactions;
            Connection con = null;
            PreparedStatement pStat = null;

            try {
                con = getDBInstance();

                //cleanTransactionStagingDataQuery
                System.out.println("Clearing Transaction Staging table data.");
                pStat = con.prepareStatement(cleanTransactionStagingDataQuery);
                pStat.execute();
                System.out.println("Cleared Transaction Staging table data.");

                pStat = con.prepareStatement(transactionStagingQuery);
                for (SocietyHelpTransaction t : bankStatement.allTransactions) {
                    pStat.setInt(1, t.srNo);
                    pStat.setString(2, t.name);
                    pStat.setFloat(3, t.amount);
                    if (t.transactionDate != null)
                        pStat.setDate(4, new Date(t.transactionDate.getTime()));
                    else pStat.setDate(4, new Date(System.currentTimeMillis()));
                    pStat.setString(5, t.transactionFlow);
                    pStat.setString(6, t.type);
                    pStat.setString(7, t.reference);
                    pStat.setString(8, bankStatement.uploadedLoginId);
                    pStat.addBatch();
                    pStat.clearParameters();
                }

                System.out.println("Executing Batch operations");
                pStat.executeBatch();
                System.out.println("Executed Batch operations");
                saveStatementProcessedDB(bankStatement.bankStatementFileName, bankStatement.uploadedLoginId);

                return true;
            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, null);
            }

        }
        return false;
    }

    public List<StagingTransaction> getAllStaggingTransaction() throws Exception {
        List<StagingTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			SELECT `Transaction_ID`, `StatementID`, `Name`, `Amount`, `Transaction_Date`,
			`Transaction_Flow`, `Transaction_Mode`, `Transaction_Reference`,
			`Upload_Date`, `Uploaded_LoginId` FROM `Transactions_Staging_Data`
			*/
            connection = getDBInstance();
            pStat = connection.prepareStatement(allTransactionStagingDataQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                StagingTransaction t = new StagingTransaction();
                t.transactionId = result.getInt(1);
                t.srNo = result.getInt(2);
                t.name = result.getString(3);
                t.amount = result.getFloat(4);
                t.transactionDate = result.getDate(5);
                t.transactionFlow = result.getString(6);
                t.type = result.getString(7);
                t.reference = result.getString(8);
                t.updloadedDate = result.getTimestamp(9);
                t.uploadedBy = result.getString(10);
                t.verifiedBy = "";
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }


    public void saveStatementProcessedDB(String monthlyStatementFileName, String loginId) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(statementProcessedQuery);
            pStat.setString(1, monthlyStatementFileName);
            pStat.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pStat.setString(3, loginId);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public void insertRawData(List<String> data) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(insertRawDataQuery);
            for (String d : data) {
                pStat.setString(1, d);
                pStat.addBatch();
                pStat.clearParameters();
            }
            pStat.executeBatch();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }

    }

    public List<String> showRawData() throws Exception {
        List<String> allRawData = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectAllRawQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                allRawData.add(result.getString(1));
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return allRawData;
    }

    public void deleteAllRawData() throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(deleteAllRawQuery);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }

    }

    public List<UserDetails> getAllUsers() throws Exception {
        List<UserDetails> users = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(allUsersQuery);
            result = pStat.executeQuery();
            while (result.next()) {
				/*
				SELECT `User_Id`, `Login_Id`, `User_Type_Id`,
				`Status`, `Flat_Id`, `Name`,
				LEFT(`Name_Alias`, 256), `Mobile_No`, `Moble_No_Alternate`,
				`Email_Id`, `Address`, `Flat_Join_Date`,
				`Flat_Left_Date`
				FROM `sql6134070`.`User_Details` ORDER BY `Login_Id` ASC LIMIT 0, 1000;
				 */
                UserDetails u = new UserDetails();
                u.userId = result.getString(1);
                u.loginId = result.getString(2);
                u.userType = result.getString(3);

                u.isActive = result.getInt(4) == 1 ? true : false;
                u.flatId = result.getString(5);
                u.userName = result.getString(6);

                u.nameAlias = result.getString(7);
                u.mobileNo = result.getLong(8);
                u.mobileNoAlternative = result.getLong(9);

                u.emailId = result.getString(10);
                u.address = result.getString(11);
                u.flatJoinDate = result.getDate(12);

                u.flatLeftDate = result.getDate(13);

                users.add(u);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return users;
    }


    public List<ExpenseType> getExpenseType() throws Exception {
        List<ExpenseType> types = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(expenseTypeQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                ExpenseType t = new ExpenseType();
                t.expenseTypeId = result.getInt(1);
                t.type = result.getString(2);
                t.transactionPriority = result.getInt(3);
                types.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return types;
    }

    public void errorLogging(String data) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(errorLoggingQuery);
            pStat.setString(1, data);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }


    public List<String> getAllTransactionStagingUsers() throws Exception {
        List<String> uniqueNames = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(allTransactionStagingUsersQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                uniqueNames.add(result.getString(1));
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return uniqueNames;
    }

    public void setAliasOfUserId(String userId, String alias) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(updateAliasUserId);
            pStat.setString(1, alias);
            pStat.setString(2, userId);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public void createUserLogin(String loginId, String password, String adminLoginId) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(createLoginQuery);
            pStat.setString(1, loginId);
            pStat.setString(2, password);
            pStat.setString(3, adminLoginId);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public void addFlatDetails(Object oflat) throws Exception {

        if (oflat instanceof Flat) {
            Flat flat = (Flat) oflat;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                pStat = con.prepareStatement(insertFlatDetailsQuery);
                pStat.setString(1, flat.flatId);
                pStat.setString(2, flat.flatNumber);
                pStat.setInt(3, flat.area);
                pStat.setFloat(4, flat.maintenanceAmount);
                pStat.setString(5, flat.block);

                pStat.executeUpdate();

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public List<Login> getAllLogin(String loginId) throws Exception {
        List<Login> logins = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			SELECT `Login_Id`, `Password`, `Status`, `Authorised_Activity` FROM `sql6134070`.`Login` LIMIT 0, 1000;
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectAllLoginQuery);
            pStat.setString(1, loginId);
            result = pStat.executeQuery();
            while (result.next()) {
                Login l = new Login();
                l.loginId = result.getString(1);
                l.password = result.getString(2);
                l.status = result.getBoolean(3);
                l.societyId = result.getString(4);
                logins.add(l);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return logins;
    }

    public List<Flat> getAllFlats() throws Exception {
        List<Flat> flats = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			SELECT `Flat_Id`, `Flat_Number`, `Area`, `Maintenance_Amount`, `Block_Number`, `Status`
			FROM `sql6134070`.`Flat` LIMIT 0, 1000;
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectAllFlatQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                Flat l = new Flat();
                l.flatId = result.getString(1);
                l.flatNumber = result.getString(2);
                l.area = result.getInt(3);
                l.maintenanceAmount = result.getFloat(4);
                l.block = result.getString(5);
                l.status = result.getBoolean(6);

                flats.add(l);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return flats;
    }

    public void addUserDetails(Object oflat) throws Exception {

        if (oflat instanceof UserDetails) {
            UserDetails ud = (UserDetails) oflat;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                pStat = con.prepareStatement(insertUserDetailsQuery);
				/*
		        User_Id, User_Type, Flat_Id,
                Name, Name_Alias, Mobile_No,
                Moble_No_Alternate,Email_Id,Address,
                Flat_Join_Date,Flat_Left_Date, Login_Id)
				 */
                pStat.setString(1, ud.userId);
                pStat.setString(2, ud.userType);
                pStat.setString(3, ud.flatId);

                pStat.setString(4, ud.userName);
                pStat.setString(5, ud.nameAlias);
                pStat.setLong(6, ud.mobileNo);

                pStat.setLong(7, ud.mobileNoAlternative);
                pStat.setString(8, ud.emailId);
                pStat.setString(9, ud.address);
                pStat.setDate(10, ud.flatJoinDate == null ? new Date(System.currentTimeMillis()) : ud.flatJoinDate);
                pStat.setDate(11, ud.flatLeftDate == null ? new Date(System.currentTimeMillis()) : ud.flatLeftDate);
                pStat.setString(12, ud.loginId);

                StringBuilder au = new StringBuilder();
                boolean isFirst = true;
                for (SocietyAuthorization.Type auT : ud.sAuthorizations) {
                    if (isFirst) {
                        au.append(auT.ordinal());
                        isFirst = false;
                    } else au.append(",").append(auT.ordinal());
                }
                pStat.setString(13, au.toString());
                pStat.executeUpdate();

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public List<SocietyHelpTransaction> getAllDetailTransaction() throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			Transaction_ID,StatementID,ud.Name,Amount," +
            "Transaction_Date,Transaction_Flow,Transaction_Mode,Transaction_Reference," +
            "ud.User_Id,ud.Flat_Id,Admin_Approved,Admin_Comment," +
                "User_Comment
		    */
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectFinalTransactionQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.transactionId = result.getInt(1);
                t.srNo = result.getInt(2);
                t.name = result.getString(3);
                t.amount = result.getFloat(4);
                t.transactionDate = result.getDate(5);
                t.transactionFlow = result.getString(6);
                t.type = result.getString(7);
                t.reference = result.getString(8);
                t.userId = result.getString(9);
                t.flatId = result.getString(10);
                t.verifiedBy = "";
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<SocietyHelpTransaction> saveVerifiedTransactionsDB(Object transactions) throws Exception {

        List<SocietyHelpTransaction> uploadedTransactions = new ArrayList<>();

        if (transactions instanceof BankStatement) {
            BankStatement bankStatement = (BankStatement) transactions;
            Connection con = null;
            PreparedStatement pStat = null;

            try {
                con = getDBInstance();

                pStat = con.prepareStatement(insertFinalTransactionQuery);
                for (SocietyHelpTransaction t : bankStatement.allTransactions) {
                    /*
                    "Insert into Transactions_Verified (" +
					"Amount, Transaction_Date,Transaction_Flow,Transaction_Mode,Transaction_Reference," +
					"User_Id, Flat_Id,verified_by, splitted) " +
					"values (? , ?, ?, ?, ?, " +
					"? , ?, ?, ?) ";
                     */
                    if (t.userId != null) {
                        pStat.setFloat(1, t.amount);
                        if (t.transactionDate != null)
                            pStat.setDate(2, new Date(t.transactionDate.getTime()));
                        else pStat.setDate(2, new Date(System.currentTimeMillis()));
                        pStat.setString(3, t.transactionFlow);
                        pStat.setString(4, t.type);
                        pStat.setString(5, t.reference);
                        pStat.setString(6, t.userId);
                        pStat.setString(7, t.flatId);
                        pStat.setString(8, t.verifiedBy);
                        pStat.setBoolean(9, t.splitted);

                        pStat.addBatch();
                        pStat.clearParameters();
                        uploadedTransactions.add(t);
                    }
                }

                System.out.println("Executing Batch operations");
                pStat.executeBatch();
                System.out.println("Executed Batch operations");

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, null);
            }

        }
        return uploadedTransactions;
    }

    public List<SocietyHelpTransaction> getMyTransactions(String userId) throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(myTransactionsQuery);
            pStat.setString(1, userId);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
               SELECT `Transaction_ID`, `Amount`, `Transaction_Date`,
               `Transaction_Flow`, `Transaction_Mode`, `Transaction_Reference`,
               `Flat_Id`, `Admin_Comment`,`User_Comment`
               FROM Transactions_Verified
                 */
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.transactionId = result.getInt(1);
                t.amount = result.getFloat(2);
                t.transactionDate = result.getDate(3);
                t.transactionFlow = result.getString(4);
                t.type = result.getString(5);
                t.reference = result.getString(6);
                t.flatId = result.getString(7);
                t.verifiedBy = result.getString(8);
                t.splitted = result.getBoolean(9);
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }


    public float myDue(String flatId) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(myDueQuery);
            pStat.setString(1, flatId);
            pStat.setString(2, flatId);
            pStat.setString(3, flatId);
            pStat.setString(4, flatId);
            res = pStat.executeQuery();
            if (res != null && res.next()) {
                return res.getFloat("MyDue");
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
        return 0f;
    }

    public UserDetails getMyDetails(String loginId) throws Exception {

        UserDetails u = null;
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(myDetailsQuery);
            pStat.setString(1, loginId);
            result = pStat.executeQuery();
            if (result.next()) {
                u = new UserDetails();
                u.userId = result.getString(1);
                u.loginId = result.getString(2);
                u.userType = result.getString(3);

                u.isActive = result.getInt(4) == 1 ? true : false;
                u.flatId = result.getString(5);
                u.userName = result.getString(6);

                u.nameAlias = result.getString(7);
                u.mobileNo = result.getLong(8);
                u.mobileNoAlternative = result.getLong(9);

                u.emailId = result.getString(10);
                u.address = result.getString(11);
                u.flatJoinDate = result.getDate(12);

                u.flatLeftDate = result.getDate(13);

                String userAuthIds = result.getString(14);

                if (userAuthIds != null)
                    for (String authId : userAuthIds.split(","))
                        u.sAuthorizations.add(SocietyAuthorization.Type.values()[Integer.valueOf(authId)]);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return u;
    }


    public void addMonthlyMaintenance(Object objFwp) throws Exception {

        if (objFwp instanceof FlatWisePayable) {

            FlatWisePayable fwp = (FlatWisePayable) objFwp;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                if (fwp.flatId.equals(CONST_LIST_STR_ALL_FLATS)) {
                    if (fwp.expenseType.equals(ExpenseType.ExpenseTypeConst.Monthly_Maintenance)) {
                        pStat = con.prepareStatement(addlatMaintenancePayablesQuery);
                        pStat.setInt(1, fwp.month);
                        pStat.setInt(2, fwp.year);
                        pStat.setInt(3, fwp.expenseType.ordinal());
                        pStat.executeUpdate();
                    } else {
                        pStat = con.prepareStatement(addAllFlatPayablesQuery);
                        pStat.setFloat(1, fwp.amount);
                        pStat.setInt(2, fwp.month);
                        pStat.setInt(3, fwp.year);
                        pStat.setInt(4, fwp.expenseType.ordinal());
                        pStat.executeUpdate();
                    }
                } else {
                    pStat = con.prepareStatement(addSingleFlatPayablesQuery);
                    pStat.setString(1, fwp.flatId);
                    pStat.setFloat(2, fwp.amount);
                    pStat.setInt(3, fwp.month);
                    pStat.setInt(4, fwp.year);
                    pStat.setInt(5, fwp.expenseType.ordinal());
                    pStat.executeUpdate();
                }

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public List<SocietyHelpTransaction> getFlatWiseTransactions(String flatId) throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(flatWiseTransactionsQuery);
            pStat.setString(1, flatId);
            result = pStat.executeQuery();
            while (result.next()) {
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.flatId = result.getString(1);
                t.transactionFlow = result.getString(2);
                t.amount = result.getFloat(3);
                t.transactionDate = result.getDate(4);
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<Login> getAllAssignedLogin() throws Exception {
        List<Login> logins = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			SELECT `Login_Id`, `Password`, `Status`, `Authorised_Activity` FROM `sql6134070`.`Login` LIMIT 0, 1000;
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectAllAssignedLoginIdsQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                Login l = new Login();
                l.loginId = result.getString(1);
                logins.add(l);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return logins;
    }

    public void addUserCashPaymentDB(Object payment) throws Exception {
        if (payment instanceof UserPaid) {

            UserPaid userPaid = (UserPaid) payment;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                pStat = con.prepareStatement(addUserPaymentQuery);

                pStat.setString(1, userPaid.userId);
                pStat.setString(2, userPaid.flatId);
                pStat.setFloat(3, userPaid.amount);
                pStat.setDate(4, userPaid.expendDate);
                pStat.setString(5, userPaid.userComment);
                pStat.setInt(6, userPaid.expenseType.ordinal());

                pStat.executeUpdate();

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public List<UserPaid> getUnVerifiedUserCashPayment() throws Exception {
        List<UserPaid> payments = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			Payment_ID,User_ID,Flat_ID,Amount,Paid_Date,Type,User_Comment,Admin_Comment
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(unVerifiedCashPaymentByUserQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                UserPaid paid = new UserPaid();
                paid.paymentId = result.getInt(1);
                paid.userId = result.getString(2);
                paid.flatId = result.getString(3);
                paid.amount = result.getFloat(4);
                paid.expendDate = result.getDate(5);
                paid.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(6)];
                paid.userComment = result.getString(7);
                paid.adminComment = result.getString(8);
                payments.add(paid);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return payments;
    }

    public List<UserPaid> getUnSplittedUserCashPayment() throws Exception {
        List<UserPaid> payments = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			Payment_ID,User_ID,Flat_ID,Amount,Paid_Date,Type,User_Comment,Admin_Comment
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(unSplittedCashPaymentByUserQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                UserPaid paid = new UserPaid();
                paid.paymentId = result.getInt(1);
                paid.userId = result.getString(2);
                paid.flatId = result.getString(3);
                paid.amount = result.getFloat(4);
                paid.expendDate = result.getDate(5);
                paid.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(6)];
                paid.userComment = result.getString(7);
                paid.adminComment = result.getString(8);
                payments.add(paid);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return payments;
    }

    public void saveVerifiedCashPayment(String userId, String paymentIds) throws Exception {
        Connection con = null;
        Statement stat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            con.setAutoCommit(false); //transaction block start
            //TODO add here code for
            // Insert into Transactions_BalanceSheet table and get the Balance_Sheet_Transaction_ID id
            // Update User_paid verified state
            // Flat_Wise_Payable_Paid_Mapping map payable to paid

            stat = con.createStatement();
            String sqlQuery = "update User_Paid set Verified=1, Verified_by='"
                    + userId + "' where Payment_Id in (" + paymentIds + ")";
            stat.executeUpdate(sqlQuery);

            stat = con.createStatement();
            sqlQuery = "update User_Paid set Verified=1, Verified_by='"
                    + userId + "' where Payment_Id in (" + paymentIds + ")";
            stat.executeUpdate(sqlQuery);

            con.commit(); //transaction block end

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, stat, res);
        }
    }

    public List<UserPaid> generateSplittedTransactionsFlatWise() throws Exception {
        //If user has multiple flats? How to solve
        //Validate one user per flat
        List<UserPaid> splittedPaidAmountFlatWise = new ArrayList<>();
        try {
            // pick all flat wise payables
            List<SocietyHelpTransaction> unSplitedTransactions = getUnSplittedTransactions();
            // pick user un splitted cash payment
            List<UserPaid> unSplitedUserCashPayment = getUnSplittedUserCashPayment();
            // pick all flat wise advance payment
            List<TransactionOnBalanceSheet> advancePayment = getAdvancePayment();
            // all payables
            List<FlatWisePayable> unPaidAmountFlatWise = getUnPaidAmountFlatWise();
            // Final Balance Sheet Transactions
            List<TransactionOnBalanceSheet> readyToAddInBalanceSheet = new ArrayList<>();

            boolean exactMatchFound;
            boolean payableTotlyPaid;

            for (FlatWisePayable payable : unPaidAmountFlatWise) {
                //Check for exact match
                exactMatchFound = isExactMatchFoundInAdvancePayment(payable, advancePayment, readyToAddInBalanceSheet);
                if (!exactMatchFound)
                    exactMatchFound = isExactMatchFoundInBankTransactionPayment(payable, unSplitedTransactions, readyToAddInBalanceSheet);
                if (!exactMatchFound)
                    exactMatchFound = isExactMatchFoundInUserCashPaid(payable, unSplitedUserCashPayment, readyToAddInBalanceSheet);
                //Now if it extact match found, then pick next payable
                if (exactMatchFound) continue;

                //Now split the already paid amount
                payableTotlyPaid = totalPayableFoundInAdvancePayment(payable, advancePayment, readyToAddInBalanceSheet);
                if(!payableTotlyPaid) payableTotlyPaid = totalPayableFoundInBankTransactionPayment(payable, unSplitedTransactions, readyToAddInBalanceSheet);
                if(!payableTotlyPaid) payableTotlyPaid = totalPayableFoundInUserCashPaid(payable, unSplitedUserCashPayment, readyToAddInBalanceSheet);

                if(payableTotlyPaid) continue;
                //If this position reach, it means there is no payment left for that flat, so break this loop
                break;
            }
            addToBalanceSheet(readyToAddInBalanceSheet, unSplitedUserCashPayment, unSplitedTransactions);


        } catch (Exception e) {
            Log.e("error", "Error while generating splitted transactions.", e);
            throw e;
        }
        return splittedPaidAmountFlatWise;
    }

    public boolean addToBalanceSheet(List<TransactionOnBalanceSheet> readyToAddInBalanceSheet,
                                     List<UserPaid> unSplitedUserCashPayment, List<SocietyHelpTransaction> unSplitedTransactions)
    {
        // All this should happen in a only SQL transaction.
        // Insert in transactions_balance sheet (all splitted transaction)
        // Insert in flat_wise_payable_paid_mapping table
        // Update transactions_verified transaction splitted
        // Update user_paid transaction splitted

        List<TransactionOnBalanceSheet> insertTransactions = tobeInsertedTransactionInBalanceSheet(readyToAddInBalanceSheet);
        List<TransactionOnBalanceSheet> updateTransaction = tobeUpdatedTransactionInBalanceSheet(readyToAddInBalanceSheet);
        //do not make entry of advance update transaction
        List<TransactionOnBalanceSheet>  advancePayment = toBeInsertAsAdvancePayment(unSplitedUserCashPayment, unSplitedTransactions);

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            con.setAutoCommit(false); //transaction block start

            insertToPreparedStatementInBatch(con, insertTransactions);
            updateToPreparedStatementInBatch(con, updateTransaction);
            insertToPreparedStatementInBatch(con, advancePayment);
            insertToPreparedStatementInBatch(con, insertTransactions);
            updateSplittedUserCash(con, insertTransactions);
            updateSplittedBankStatement(con, insertTransactions);
            insertToPaymentToPaidFlatwise(con, insertTransactions);

            con.commit(); //transaction block end
        } catch (Exception e) {
            Log.e("Error", "Message - " + e.getMessage());
            //throw e;
        } finally {
            close(con, pStat, res);
        }
        return false;
    }

    public void insertToPaymentToPaidFlatwise(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception
    {
        PreparedStatement pStat = con.prepareStatement(insertFlatWisePayableToPaidMapping);
        for (TransactionOnBalanceSheet t : insertTransactions) {
            pStat.setInt(1, t.flatWisePayableID);
            pStat.setInt(2, t.balanceSheetTransactionID);
            pStat.addBatch();
            pStat.clearParameters();
        }
        pStat.executeBatch();
    }

    public void updateSplittedBankStatement(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception
    {
        PreparedStatement pStat = con.prepareStatement(updateUserCashSpitted);
        for (TransactionOnBalanceSheet t : insertTransactions) {

            if(t.transactionFromBankStatementID >0)
            {
                pStat.setInt(1, t.transactionFromBankStatementID);
                pStat.addBatch();
                pStat.clearParameters();
            }
        }
        pStat.executeBatch();
    }

    public void updateSplittedUserCash(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception
    {
        PreparedStatement pStat = con.prepareStatement(updateUserCashSpitted);
        for (TransactionOnBalanceSheet t : insertTransactions) {

            if(t.userCashPaymentID >0)
            {
                pStat.setInt(1, t.userCashPaymentID);
                pStat.addBatch();
                pStat.clearParameters();
            }
        }
        pStat.executeBatch();
    }

    public void insertToPreparedStatementInBatch(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception
    {
        PreparedStatement pStat = con.prepareStatement(insertToBalanceQuery);
        for (TransactionOnBalanceSheet t : insertTransactions) {
            pStat.setFloat(1, t.amount);
            pStat.setBoolean(2, t.isVerifiedByAdmin);
            pStat.setBoolean(3, t.isVerifiedByUser);
            pStat.setInt(4, t.expenseType.ordinal());
            pStat.setInt(5, t.transactionFromBankStatementID);
            pStat.setInt(6, t.userCashPaymentID);
            pStat.setInt(7, t.transactionExpenseId);
            pStat.setString(8, t.transactionFlow);
            pStat.addBatch();
            pStat.clearParameters();
        }
        pStat.executeBatch();
    }

    public void updateToPreparedStatementInBatch(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception
    {
        PreparedStatement pStat = con.prepareStatement(updateBalanceSheet_Transaction);
        for (TransactionOnBalanceSheet t : insertTransactions) {
            pStat.setInt(1, t.expenseType.ordinal());
            pStat.setFloat(2, t.amount);
            pStat.setInt(3, t.transactionExpenseId);
            pStat.setInt(4, t.userCashPaymentID);
            pStat.setInt(5, t.transactionFromBankStatementID);
            pStat.setInt(6, t.balanceSheetTransactionID);
            pStat.addBatch();
            pStat.clearParameters();
        }
        pStat.executeBatch();
    }

    public List<TransactionOnBalanceSheet> toBeInsertAsAdvancePayment(List<UserPaid> unSplitedUserCashPayment, List<SocietyHelpTransaction> unSplitedTransactions)
    {
        List<TransactionOnBalanceSheet> advancePayment = new ArrayList<>();
        for(UserPaid t : unSplitedUserCashPayment)
        {
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amount = t.amount;
            balanceSheetTranction.expenseType = ExpenseType.ExpenseTypeConst.Advance_Payment;
            balanceSheetTranction.transactionFromBankStatementID = t.transactionId;
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            balanceSheetTranction.action = TransactionOnBalanceSheet.DBAction.INSERT;
            balanceSheetTranction.isVerifiedByAdmin = true;
            advancePayment.add(balanceSheetTranction);
        }

        for(SocietyHelpTransaction t : unSplitedTransactions)
        {
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amount = t.amount;
            balanceSheetTranction.expenseType = ExpenseType.ExpenseTypeConst.Advance_Payment;
            balanceSheetTranction.transactionFromBankStatementID = t.transactionId;
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            balanceSheetTranction.action = TransactionOnBalanceSheet.DBAction.INSERT;
            balanceSheetTranction.isVerifiedByAdmin = true;
            advancePayment.add(balanceSheetTranction);
        }

        return advancePayment;
    }

    public List<TransactionOnBalanceSheet> tobeUpdatedTransactionInBalanceSheet(List<TransactionOnBalanceSheet> readyToAddInBalanceSheet)
    {
        List<TransactionOnBalanceSheet> updateTransaction = new ArrayList<>();
        for(TransactionOnBalanceSheet transaction : readyToAddInBalanceSheet)
        {
            if(transaction.action.equals(TransactionOnBalanceSheet.DBAction.UPDATE)) updateTransaction.add(transaction);
        }
        return updateTransaction;
    }

    public List<TransactionOnBalanceSheet> tobeInsertedTransactionInBalanceSheet(List<TransactionOnBalanceSheet> readyToAddInBalanceSheet)
    {
        List<TransactionOnBalanceSheet> insertTransaction = new ArrayList<>();
        for(TransactionOnBalanceSheet transaction : readyToAddInBalanceSheet)
        {
            if(transaction.action.equals(TransactionOnBalanceSheet.DBAction.INSERT)) insertTransaction.add(transaction);
        }
        return insertTransaction;
    }

    //Bank Monthly Statement Transaction Payment split to payables
    public boolean totalPayableFoundInUserCashPaid(FlatWisePayable payable, List<UserPaid> unSplitedUserCashPayment, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {

        List<UserPaid> consumedPayment = new ArrayList<>();
        for (UserPaid cashTransaction : unSplitedUserCashPayment) {

            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            //Payable is greater then Paid
            if(payable.amount > cashTransaction.amount)
            {
                payable.amount = payable.amount -  cashTransaction.amount;
                consumedPayment.add(cashTransaction); //it is totally consume, added in consume, so it will be removed from advancePayment list;
                balanceSheetTranction.amount = cashTransaction.amount;
            }
            //Payable is less then Paid
            else if(payable.amount < cashTransaction.amount)
            {
                cashTransaction.amount = cashTransaction.amount - payable.amount;
                balanceSheetTranction.amount = payable.amount;
                payable.amount = 0;
            }

            balanceSheetTranction.expenseType = payable.expenseType;
            balanceSheetTranction.transactionFromBankStatementID = cashTransaction.transactionId; //Monthly statement transaction id mapping only. [transactions_verified table]
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
            readyToAddInBalanceSheet.add(balanceSheetTranction);

            if(payable.amount == 0) break;
        }

        for(UserPaid consumedTransaction : consumedPayment)
        {
            unSplitedUserCashPayment.remove(consumedTransaction);
        }
        if(payable.amount == 0)    return true; //payable amount is totally paid.

        return false; //payable amount is not totally paid.
    }

    //Bank Monthly Statement Transaction Payment split to payables
    public boolean totalPayableFoundInBankTransactionPayment(FlatWisePayable payable, List<SocietyHelpTransaction> unSplitedTransactions, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {

        List<SocietyHelpTransaction> consumedPayment = new ArrayList<>();
        for (SocietyHelpTransaction bankTransaction : unSplitedTransactions) {

            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            //Payable is greater then Paid
            if(payable.amount >= bankTransaction.amount)
            {
                payable.amount = payable.amount -  bankTransaction.amount;
                consumedPayment.add(bankTransaction); //it is totally consume, added in consume, so it will be removed from advancePayment list;
                balanceSheetTranction.amount = bankTransaction.amount;
            }
            //Payable is less then Paid
            else if(payable.amount < bankTransaction.amount)
            {
                bankTransaction.amount = bankTransaction.amount - payable.amount;
                balanceSheetTranction.amount = payable.amount;
                payable.amount = 0;
            }

            balanceSheetTranction.expenseType = payable.expenseType;
            balanceSheetTranction.transactionFromBankStatementID = bankTransaction.transactionId; //Monthly statement transaction id mapping only. [transactions_verified table]
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
            readyToAddInBalanceSheet.add(balanceSheetTranction);

            if(payable.amount == 0) break;
        }

        for(SocietyHelpTransaction consumedTransaction : consumedPayment)
        {
            unSplitedTransactions.remove(consumedTransaction);
        }
        if(payable.amount == 0)    return true; //payable amount is totally paid.

        return false; //payable amount is not totally paid.
    }

    //Advance payment split to payables
    public boolean totalPayableFoundInAdvancePayment(FlatWisePayable payable, List<TransactionOnBalanceSheet> advancePayment, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {

        List<TransactionOnBalanceSheet> consumedPayment = new ArrayList<>();
        for (TransactionOnBalanceSheet advanceAmountInBalSheet : advancePayment) {
            //Payable is greater then Paid
            if(payable.amount >= advanceAmountInBalSheet.amount)
            {
                advanceAmountInBalSheet.action = TransactionOnBalanceSheet.DBAction.UPDATE;
                advanceAmountInBalSheet.expenseType = payable.expenseType;
                payable.amount = payable.amount -  advanceAmountInBalSheet.amount;
                readyToAddInBalanceSheet.add(advanceAmountInBalSheet);
                consumedPayment.add(advanceAmountInBalSheet); //it is totally consume, added in consume, so it will be removed from advancePayment list;
            }
            //Payable is less then Paid
            else if(payable.amount < advanceAmountInBalSheet.amount)
            {
                advanceAmountInBalSheet.amount = advanceAmountInBalSheet.amount - payable.amount;
                payable.amount = 0;

                TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
                balanceSheetTranction.amount = payable.amount;
                balanceSheetTranction.expenseType = payable.expenseType;
                balanceSheetTranction.transactionFromBankStatementID = advanceAmountInBalSheet.transactionFromBankStatementID; //Monthly statement transaction id mapping only. [transactions_verified table]
                balanceSheetTranction.userCashPaymentID = advanceAmountInBalSheet.userCashPaymentID; //Mapping of user cash id to this transaction
                balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
                balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
                readyToAddInBalanceSheet.add(balanceSheetTranction);
                break;
            }
        }

        for(TransactionOnBalanceSheet consumedTransaction : consumedPayment)
        {
            advancePayment.remove(consumedTransaction);
        }

        if(payable.amount == 0)    return true; //payable amount is totally paid.

        return false; //payable amount is not totally paid.
    }

    //Search for exact search in User cash payment
    public boolean isExactMatchFoundInUserCashPaid(FlatWisePayable payable, List<UserPaid> unSplitedUserCashPayment, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {
        UserPaid foundTransaction = null;
        for (UserPaid curTransaction : unSplitedUserCashPayment) {
            if (curTransaction.amount == payable.amount) {
                foundTransaction = curTransaction;
                break;
            }
        }
        if (foundTransaction != null) {
            unSplitedUserCashPayment.remove(foundTransaction);
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amount = payable.amount;
            balanceSheetTranction.expenseType = payable.expenseType;
            balanceSheetTranction.userCashPaymentID = foundTransaction.transactionId; //User cash payment transaction id mapping only. [user_paid table]
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
            readyToAddInBalanceSheet.add(balanceSheetTranction);
            return true;
        }
        return false;
    }

    //Search for exact search in Bank statement transaction
    public boolean isExactMatchFoundInBankTransactionPayment(FlatWisePayable payable, List<SocietyHelpTransaction> unSplitedTransactions, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {
        SocietyHelpTransaction foundTransaction = null;
        for (SocietyHelpTransaction curTransaction : unSplitedTransactions) {
            if (curTransaction.amount == payable.amount) {
                foundTransaction = curTransaction;
                break;
            }
        }
        if (foundTransaction != null) {
            unSplitedTransactions.remove(foundTransaction);
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amount = payable.amount;
            balanceSheetTranction.expenseType = payable.expenseType;
            balanceSheetTranction.transactionFromBankStatementID = foundTransaction.transactionId; //Monthly statement transaction id mapping only. [transactions_verified table]
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
            readyToAddInBalanceSheet.add(balanceSheetTranction);
            return true;
        }
        return false;
    }

    //Search for exact search in Advance payment
    public boolean isExactMatchFoundInAdvancePayment(FlatWisePayable payable, List<TransactionOnBalanceSheet> advancePayment, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {
        TransactionOnBalanceSheet foundTransaction = null;
        for (TransactionOnBalanceSheet advanceAmountInBalSheet : advancePayment) {
            if (advanceAmountInBalSheet.amount == payable.amount) {
                foundTransaction = advanceAmountInBalSheet;
                break;
            }
        }
        if (foundTransaction != null) {
            advancePayment.remove(foundTransaction);
            foundTransaction.expenseType = payable.expenseType;
            foundTransaction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
            foundTransaction.action = TransactionOnBalanceSheet.DBAction.UPDATE;
            readyToAddInBalanceSheet.add(foundTransaction);
            return true;
        }
        return false;
    }

    public SortedSet<FlatWisePayable> getTransactionWiseFlatPayables(SocietyHelpTransaction transaction, List<FlatWisePayable> unPaidAmountFlatWise) {
        SortedSet<FlatWisePayable> flatWisePayables = new TreeSet<>();

        for (FlatWisePayable fp : unPaidAmountFlatWise) {
            if (fp.flatId.equals(transaction.flatId)) flatWisePayables.add(fp);
        }

        return flatWisePayables;
    }


    public List<SocietyHelpTransaction> getUnSplittedTransactions() throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(unSplittedTransactionsQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                SELECT Transaction_ID,Amount,Transaction_Date,Transaction_Flow," +
				"Transaction_Mode,Transaction_Reference,User_Id,Flat_Id," +
				"Verified_By,Splitted " +
				"FROM Transactions_Verified " +
				"where splitted=0";
                 */
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.transactionId = result.getInt(1);
                t.amount = result.getFloat(2);
                t.transactionDate = result.getDate(3);
                t.transactionFlow = result.getString(4);
                t.type = result.getString(5);
                t.reference = result.getString(6);
                t.userId = result.getString(7);
                t.flatId = result.getString(8);
                t.verifiedBy = result.getString(9);
                t.splitted = result.getBoolean(10);
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<TransactionOnBalanceSheet> getAdvancePayment() throws Exception {
        List<TransactionOnBalanceSheet> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(advanceTransactionsQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                Balance_Sheet_Transaction_ID,Amount,
                Verified_By_Admin," "Verified_By_User,Expense_Type_Id,
				Transaction_From_Bank_Statement_ID," +
				"User_Cash_Payment_ID,Transaction_Expense_Id " +
                 */
                TransactionOnBalanceSheet t = new TransactionOnBalanceSheet();
                t.balanceSheetTransactionID = result.getInt(1);
                t.amount = result.getFloat(2);
                t.isVerifiedByAdmin = result.getBoolean(3);
                t.isVerifiedByUser = result.getBoolean(4);
                t.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(5)];
                t.transactionFromBankStatementID = result.getInt(6);
                t.userCashPaymentID = result.getInt(7);
                t.transactionExpenseId = result.getInt(8);

                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<FlatWisePayable> getUnPaidAmountFlatWise() throws Exception {
        List<FlatWisePayable> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            //pStat = connection.prepareStatement(unPaidFlatWiseAmountQuery);
            pStat = connection.prepareStatement(payableAndPaidFlatWiseAmountQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                         fwp.Flat_Wise_Payable_ID, fwp.Amount Payable, sum(tbs.Amount) Paid, " +
						"et.Type, et.Expense_Type_Id, et.Payable_Priority, " +
						"fwp.Flat_Id, fwp.Status, " +
						"fwp.Month, fwp.Year, " +
						"fwp.Comments, fwp.Payment_Status_ID "
                 */
                FlatWisePayable t = new FlatWisePayable();
                t.paymentId = result.getInt(1);
                t.amount = result.getFloat(2) - result.getFloat(3);
                t.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(5)];
                t.payablePriority = result.getInt(6);
                t.flatId = result.getString(7);
                t.status = result.getBoolean(8);
                t.month = result.getInt(9);
                t.year = result.getInt(10);
                t.comments = result.getString(11);
                t.paymentStatus = ExpenseType.PaymentStatusConst.values()[result.getInt(12)];
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<UserPaid> getPaidFlatnExpenseTypeWisePayment() throws Exception {
        List<UserPaid> payments = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			Payment_ID,User_ID,Flat_ID,Amount,Paid_Date,Type,User_Comment,Admin_Comment
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(paidFlatnExpenseTypeWiseAmountQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                UserPaid paid = new UserPaid();
                paid.paymentId = result.getInt(1);
                paid.userId = result.getString(2);
                paid.flatId = result.getString(3);
                paid.amount = result.getFloat(4);
                paid.expendDate = result.getDate(5);
                paid.expenseType = ExpenseType.ExpenseTypeConst.valueOf(result.getString(6));
                paid.userComment = result.getString(7);
                paid.adminComment = result.getString(8);
                payments.add(paid);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return payments;
    }

    public List<FlatWisePayable> getFlatWisePayables() throws Exception {
        List<FlatWisePayable> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(allFlatWiseAmountQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                FlatWisePayable t = new FlatWisePayable();
                t.paymentId = result.getInt(1);
                t.flatId = result.getString(2);
                t.status = result.getBoolean(3);
                t.month = result.getInt(4);
                t.year = result.getInt(5);
                t.amount = result.getFloat(6);
                t.expenseType = ExpenseType.ExpenseTypeConst.valueOf(result.getString(7));
                t.comments = result.getString(8);
                t.paymentIds = result.getString(9);
                t.paymentStatus = ExpenseType.PaymentStatusConst.valueOf(result.getString(10));
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

}
