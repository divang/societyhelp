package bhowa.dao.mysql.impl;

public class Queries {

	  //Queries
    public static final String loginQuery = 
    		"select Login_Id FROM Login WHERE Login_Id = ? and Password = ? and Status = 1";

    public static final String activityLoggingQuery = 
    		"Insert into User_Activity_Logging (UserName, Mobile, Activity, Comment, Time) values (? ,? ,?, ?, ?)";
  
    public static final String statementProcessedQuery =
    		"Insert into Bank_Statement(Bank_Statement_FileName, Uploaded_Date) values (?, ?)";
    
    public static final String isStatementProcessedQuery =
    		"Select * FROM Bank_Statement WHERE Statement_FileName = ?";
    
    public static final String transactionQuery =
    	    "Insert into Transactions(StatementID, Name, Amount, "
    	    + "Transaction_Date, Transaction_Flow, Transaction_Mode, Transaction_Reference) "
    	    + "values(?, ?, ?, ?, ?, ?, ?)";

	public static final String transactionStagingQuery =
			"Insert into Transactions_Staging_Data(StatementID, Name, Amount, "
					+ "Transaction_Date, Transaction_Flow, Transaction_Mode, Transaction_Reference) "
					+ "values(?, ?, ?, ?, ?, ?, ?)";

	public static final String insertRawDataQuery =
			"Insert into Row_Data(Transaction_Raw_Line_Parse_From_PDF) values (?)";

	public static final String selectAllRawQuery =
			"select * from Row_Data";

	public static final String deleteAllRawQuery =

			"delete from Row_Data";

	public static final String allUsersQuery =
			"SELECT * FROM User_Details";

	public static final String expenseTypeQuery =
			"SELECT * FROM Expense_Type";

	public static final String errorLoggingQuery =
			"Insert into Error_Log(User_Id, Log) values (?, ?)";

	public static final String allTransactionStagingUsersQuery =
			"select distinct(Name) from Transactions_Staging_Data";

	public static final String updateAliasUserId =
			"UPDATE User_Details SET Name_Alias = ? WHERE User_Id = ?";
}