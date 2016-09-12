package societyhelp.dao.mysql.impl;

import java.util.List;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;

import societyhelp.dao.ISocietyHelpDatabase;

/**
 * Created by divang.sharma on 8/6/2016.
 */
public class SocietyHelpDatabaseAndroidSDKImpl extends AsyncTask<Object, Integer, Object> implements ISocietyHelpDatabase {

	private DatabaseCoreAPIs dbCore = new DatabaseCoreAPIs();

	enum QUERY_NAME
	{
		login,
		activityLogging,
		isStatementAlreadyProcessed,
		uploadMonthlyTransactions,
		insertRawData,
		showRawData,
		deleteAllRawData,
		getActiveUser,
		getExpenseType,
		errorLogging,
		getAllTransactionStagingUsers,
		setAliasOfUserId,
		createLogin,
		addFlatDetails,
        getAllLogins,
        getAllFlats,
        addUserDetails,
		getAllDetailsTransactions,
		saveVerifiedTransactions,
		getMyTransactions,
		getMyDue,
        getMyDetails,
		addMonthlyMaintenance,
		getFlatWiseTransactions
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
	}

	@Override
	protected Object doInBackground(Object... params) {

		Object result = null;
		try{

			switch(QUERY_NAME.valueOf(String.valueOf(params[0])))
			{
				case login:
					result = dbCore.loginDB(String.valueOf(params[1]), String.valueOf(params[2]));
					break;

				case activityLogging:
					dbCore.activityLoggingDB(params[1]);
					break;

				case isStatementAlreadyProcessed:
					result = dbCore.isStatementAlreadyProcessedDB(String.valueOf(params[1]));
					break;

				case uploadMonthlyTransactions:
					result = dbCore.uploadMonthlyTransactionsDB(params[1]);
					break;

				case deleteAllRawData:
					dbCore.deleteAllRawData();
					break;

				case errorLogging:
					dbCore.errorLogging(String.valueOf(params[1]));
					break;

				case getActiveUser:
					result = dbCore.getAllUsers();
					break;

				case getAllTransactionStagingUsers:
					result = dbCore.getAllTransactionStagingUsers();
					break;

				case getExpenseType:
					result = dbCore.getExpenseType();
					break;

				case insertRawData:
					dbCore.insertRawData((List<String>)params[1]);
					break;

				case showRawData:
					result = dbCore.showRawData();
					break;

				case setAliasOfUserId:
					dbCore.setAliasOfUserId(String.valueOf(params[1]), String.valueOf(params[2]));
					break;

				case createLogin:
					dbCore.createUserLogin(String.valueOf(params[1]), String.valueOf(params[2]));
					break;

				case addFlatDetails:
					dbCore.addFlatDetails(params[1]);
					break;

                case getAllLogins:
                    result = dbCore.getAllLogin();
                    break;

                case getAllFlats:
                    result = dbCore.getAllFlats();
                    break;

                case addUserDetails:
                    dbCore.addUserDetails(params[1]);
                    break;

				case getAllDetailsTransactions:
					result = dbCore.getAllDetailTransaction();
					break;

				case saveVerifiedTransactions:
					dbCore.saveVerifiedTransactionsDB(params[1]);
					break;

				case getMyTransactions:
					result = dbCore.getMyTransactions(String.valueOf(params[1]));
					break;

				case getMyDue:
					result = dbCore.myDue(String.valueOf(params[1]));
					break;

                case getMyDetails:
                    result = dbCore.getMyDetails(String.valueOf(params[1]));
                    break;

				case addMonthlyMaintenance:
					dbCore.addMonthlyMaintenance(params[1]);
					break;

				case getFlatWiseTransactions:
					result = dbCore.getFlatWiseTransactions(String.valueOf(params[1]));
					break;
			}
		}catch(Exception e){
			System.err.println("Error");
		}
		return result;
	}

	@Override
	protected void onPostExecute(Object result) {
	}

	@Override
	public boolean login(Object userDetailsObj)  throws Exception{

		if(userDetailsObj instanceof UserDetails)
		{
			UserDetails user = (UserDetails) userDetailsObj;
			AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.login.name(), user.userName, user.password);
			try {
				return (Boolean)aTask.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public void activityLogging(Object activity)  throws Exception{

		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.activityLogging.name(), activity);
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public boolean isStatementAlreadyProcessed(String monthlyStatementFileName)  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.isStatementAlreadyProcessed.name(), monthlyStatementFileName);
		try {
			return (Boolean)aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void uploadMonthlyTransactions(Object transactions)  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.uploadMonthlyTransactions.name(), transactions);
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void insertRawData(List<String> data)  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.insertRawData.name(), data);
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public List<String> showRawData()  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.showRawData.name());
		try {
			return (List<String>)aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void deleteAllRawData()  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.deleteAllRawData.name());
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public List<UserDetails> getAllUsers()  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getActiveUser.name());
		try {
			return (List<UserDetails>)aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public List<String> getExpenseType()  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getExpenseType.name());
		try {
			return (List<String>)aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void errorLogging(String data)  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.errorLogging.name(), data);
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public List<String> getAllTransactionStagingUsers()  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getAllTransactionStagingUsers.name());
		try {
			return (List<String>)aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void setAliasOfUserId(String userId, String allAliasNames)  throws Exception{
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.setAliasOfUserId.name(), userId, allAliasNames);
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void createLogin(String loginId, String password) throws Exception {
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.createLogin.name(), loginId, password);
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void addFlatDetails(Object flat) throws Exception {

		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.addFlatDetails.name(), flat);
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

    @Override
    public List<Login> getAllLogins() throws Exception {
        AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getAllLogins.name());
        try {
            return (List<Login>)aTask.get();
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public List<Flat> getAllFlats() throws Exception {
        AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getAllFlats.name());
        try {
            return (List<Flat>)aTask.get();
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void addUserDetails(Object userDetails) throws Exception {
        AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.addUserDetails.name(),userDetails);
        try {
            aTask.get();
        } catch (Exception e) {
            throw e;
        }
    }

	@Override
	public List<SocietyHelpTransaction> getAllDetailsTransactions() throws Exception {
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getAllDetailsTransactions.name());
		try {
			return (List<SocietyHelpTransaction>)aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void saveVerifiedTransactions(Object obj) throws Exception {
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.saveVerifiedTransactions.name(),obj);
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public List<SocietyHelpTransaction> getMyTransactions(String userId) throws Exception {
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getMyTransactions.name(), userId);
		try {
			return (List<SocietyHelpTransaction>)aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public float getMyDue(String flatId) throws Exception {
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getMyDue.name(), flatId);
		try {
			return (float)aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

    @Override
    public UserDetails getMyDetails(String loginId) throws Exception {
        AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getMyDetails.name(), loginId);
        try {
            return (UserDetails)aTask.get();
        } catch (Exception e) {
            throw e;
        }
    }

	@Override
	public void addMonthlyMaintenance(Object obj) throws Exception {
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.addMonthlyMaintenance.name(), obj);
		try {
			aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public List<SocietyHelpTransaction> getFlatWiseTransactions(String flatId) throws Exception {
		AsyncTask<Object, Integer, Object> aTask = this.execute(QUERY_NAME.getFlatWiseTransactions.name(), flatId);
		try {
			return (List<SocietyHelpTransaction>)aTask.get();
		} catch (Exception e) {
			throw e;
		}
	}
}