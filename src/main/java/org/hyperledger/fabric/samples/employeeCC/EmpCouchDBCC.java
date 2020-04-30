package org.hyperledger.fabric.samples.employeeCC;

import java.util.List;

import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hyperledger Fabric Chaincode example using fabric-chaincode-java SDK and aimed/focused for CouchDB rich queries
 */


public final class EmpCouchDBCC extends ChaincodeBase {

	private class ChaincodeResponse {
        public String message;
        public String code;
        public boolean OK;

        public ChaincodeResponse(String message, String code, boolean OK) {
            this.code = code;
            this.message = message;
            this.OK = OK;
        }
    }

     /**
     * Chaincode 'init()' which gets called during Chaincode instantiation.
     *
     * @param ctx the transaction context
     * @param key the key for the new Order
     * @param Order JSON String
     * @return the stored Order
     */
    
    @Override
    public Response init(ChaincodeStub stub) {
        
    	// Initialize ledger with some initial/dummy values
    	List<String> args = stub.getParameters();
    	
    	if (args.size() != 5)	// Employee(long empID, String empName, String department, double salary, String location) 
            return newErrorResponse(responseError("init: Incorrect number of arguments, expecting 5", ""));
        
        String key = args.get(0);
        //Employee employee = new Employee(key, "Dummy", "Dummy", 0.0, "Dummy");
        Employee employee = new Employee(key, args.get(1), args.get(2), Double.parseDouble(args.get(3)), args.get(4));
        
        try {
            stub.putState(key, (new ObjectMapper()).writeValueAsBytes(employee));
            return newSuccessResponse(responseSuccess("init: Dummy Employee added"));
        } catch (Throwable e) {
            return newErrorResponse(responseError(e.getMessage(), ""));
        }
    }
    
    @Override
    public Response invoke(ChaincodeStub stub) {
        String func = stub.getFunction();
        List<String> params = stub.getParameters();
        if (func.equals("addEmployee"))
            return addEmployee(stub, params);
        else if (func.equals("queryEmployee"))
            return queryEmployee(stub, params);
        else if (func.equals("queryEmpBySalaryGreaterThanXAmount"))
            return queryEmpBySalaryGreaterThanXAmount(stub, params);
        else if (func.equals("queryEmployees"))
            return queryEmployees(stub, params);
//        else if (func.equals("getWalletsWithTokenAmountGreaterThan"))
//            return getWalletsWithTokenAmountGreaterThan(stub, params);
        return newErrorResponse(responseError("Unsupported method", ""));
    }
    
    private Response addEmployee(ChaincodeStub stub, List<String> args) {
    	
		// Employee(long empID, String empName, String department, double salary, String location)
        if (args.size() != 5)
            return newErrorResponse(responseError("addEmployee: Incorrect number of arguments, expecting 5", ""));
        
        String key = args.get(0);
        Employee employee = new Employee(key, args.get(1), args.get(2), Double.parseDouble(args.get(3)), args.get(4));
        
        try {
            if(checkString(stub.getStringState(key)))
                return newErrorResponse(responseError("addEmployee: Employee already exists with employee ID: " + key, ""));
            stub.putState(key, (new ObjectMapper()).writeValueAsBytes(employee));
            return newSuccessResponse(responseSuccess("addEmployee: Employeed added"));
        } catch (Throwable e) {
            return newErrorResponse(responseError(e.getMessage(), ""));
        }
    }
    
	// Normal regular query (not a rich query)
    private Response queryEmployee(ChaincodeStub stub, List<String> args) {
    	
		// args(0) - empID (nothing but key)
        if (args.size() != 1)
            return newErrorResponse(responseError("queryEmployee: Incorrect number of arguments, expecting 1", ""));
            
        try {
            String empString = stub.getStringState(args.get(0));
            if(!checkString(empString))
                return newErrorResponse(responseError("queryEmployee: Employee doesn't exists", ""));
            return newSuccessResponse((new ObjectMapper()).writeValueAsBytes(responseSuccessObject(empString)));
        } catch(Throwable e){
            return newErrorResponse(responseError(e.getMessage(), ""));
        }
    }
    
	// Parametarized rich query (prepare query string in this values)
    private Response queryEmpBySalaryGreaterThanXAmount(ChaincodeStub stub, List<String> args) {
    	
		// args(0) - salary
        if (args.size() != 1)
            return newErrorResponse(responseError("queryEmpBySalaryGreaterThanXAmount: Incorrect number of arguments, expecting 1", ""));
        
        String salaryStr = args.get(0);

        try {
            double salary = Double.parseDouble(salaryStr);
			System.out.println("Kalyan: queryEmpBySalaryGreaterThanXAmount: input salary amount: " + salary);
            String queryStr = "{ \"selector\": { \"salary\": { \"$gt\": " + salary + " } }, \"use_index\":[\"_design/salaryIndexDoc\", \"salaryIndex\"] }";	// * always recommed to mention 'use_index'
            String queryResult = query(stub, queryStr);
			System.out.println("Kalyan: queryEmpBySalaryGreaterThanXAmount: queryResult: " + queryResult);
            return newSuccessResponse((new ObjectMapper()).writeValueAsBytes(responseSuccessObject(queryResult)));
        } catch(Throwable e){
            return newErrorResponse(responseError(e.getMessage(), ""));
        }
    }
	
	// Ad hoc rich query (client sends query string and just use as it is)
    private Response queryEmployees(ChaincodeStub stub, List<String> args) {
    	
		// args(0) - query string
        if (args.size() != 1)
            return newErrorResponse(responseError("queryEmpBySalaryGreaterThanXAmount: Incorrect number of arguments, expecting 1", ""));
        
        String queryString = args.get(0);

        try {
			// just use as it is we receive from client
            String queryResult = query(stub, queryString);
			System.out.println("Kalyan: queryEmpBySalaryGreaterThanXAmount: queryResult: " + queryResult);
            return newSuccessResponse((new ObjectMapper()).writeValueAsBytes(responseSuccessObject(queryResult)));
        } catch(Throwable e){
            return newErrorResponse(responseError(e.getMessage(), ""));
        }
    }
	
	// helper function for rich queries
    private String query(ChaincodeStub stub, String queryString) {
        String result = "[";
        QueryResultsIterator<KeyValue> rows = stub.getQueryResult(queryString);
        while (rows.iterator().hasNext()) {
            String v = rows.iterator().next().getStringValue();
			System.out.println("Kalyan: query: individual record: " + v);
            if(v != null && v.trim().length() > 0) {
                result = result.concat(v);
                if (rows.iterator().hasNext())
                    result = result.concat(",");
            }
			System.out.println("Kalyan: query: concatinated result so far: " + result);
        }
        return result.concat("]");
    }
	
	// helper functions for CC responses
	private String responseError(String errorMessage, String code) {
        try {
            return (new ObjectMapper()).writeValueAsString(new ChaincodeResponse(errorMessage, code, false));
        } catch (Throwable e) {
            return "{\"code\":'" + code + "', \"message\":'" + e.getMessage() + " AND " + errorMessage + "', \"OK\":" + false + "}";
        }
    }

    private String responseSuccess(String successMessage) {
        try {
            return (new ObjectMapper()).writeValueAsString(new ChaincodeResponse(successMessage, "", true));
        } catch (Throwable e) {
            return "{\"message\":'" + e.getMessage() + " BUT " + successMessage + " (NO COMMIT)', \"OK\":" + false + "}";
        }
    }

    private String responseSuccessObject(String object) {
        return "{\"message\":" + object + ", \"OK\":" + true + "}";
    }

    private boolean checkString(String str) {
        if (str.trim().length() <= 0 || str == null)
            return false;
        return true;
    }

    public static void main(String[] args) {
        //System.out.println("OpenSSL avaliable: " + OpenSsl.isAvailable());
        new EmpCouchDBCC().start(args);
    }
}