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
            return newErrorResponse(responseError("addEmployee: Incorrect number of arguments, expecting 5", ""));
        
        String key = args.get(0);
        Employee employee = new Employee("0", "Dummy", "Dummy", 0.0, "Dummy");
        
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
//        else if (func.equals("getWalletsWithTokenAmountGreaterThan"))
//            return getWalletsWithTokenAmountGreaterThan(stub, params);
        return newErrorResponse(responseError("Unsupported method", ""));
    }
    
    private Response addEmployee(ChaincodeStub stub, List<String> args) {
    	
        if (args.size() != 5)	// Employee(long empID, String empName, String department, double salary, String location) 
            return newErrorResponse(responseError("addEmployee: Incorrect number of arguments, expecting 5", ""));
        
        String key = args.get(0);
        Employee employee = new Employee(args.get(0), args.get(1), args.get(2), Double.parseDouble(args.get(0)), args.get(4));
        
        try {
            if(checkString(stub.getStringState(key)))
                return newErrorResponse(responseError("addEmployee: Employee already exists with employee ID: " + key, ""));
            stub.putState(key, (new ObjectMapper()).writeValueAsBytes(employee));
            return newSuccessResponse(responseSuccess("addEmployee: Employeed added"));
        } catch (Throwable e) {
            return newErrorResponse(responseError(e.getMessage(), ""));
        }
    }
    
    private Response queryEmployee(ChaincodeStub stub, List<String> args) {
    	
        if (args.size() != 1)
            return newErrorResponse(responseError("queryEmployee: Incorrect number of arguments, expecting 1", ""));
        
        String empID = args.get(0);
        
        try {
            String empString = stub.getStringState(empID);
            if(!checkString(empString))
                return newErrorResponse(responseError("queryEmployee: Employee doesn't exists", ""));
            return newSuccessResponse((new ObjectMapper()).writeValueAsBytes(responseSuccessObject(empString)));
        } catch(Throwable e){
            return newErrorResponse(responseError(e.getMessage(), ""));
        }
    }
    
    private Response queryEmpBySalaryGreaterThanXAmount(ChaincodeStub stub, List<String> args) {
    	
        if (args.size() != 1)
            return newErrorResponse(responseError("queryEmpBySalaryGreaterThanXAmount: Incorrect number of arguments, expecting 1", ""));
        
        String salaryStr = args.get(0);

        try {
            double salary = Double.parseDouble(salaryStr);
            String queryStr = "{ \"selector\": { \"salary\": { \"$gt\": " + salary + " } } }";
            String queryResult = query(stub, queryStr);
            return newSuccessResponse((new ObjectMapper()).writeValueAsBytes(responseSuccessObject(queryResult)));
        } catch(Throwable e){
            return newErrorResponse(responseError(e.getMessage(), ""));
        }
    }
    
    
    private String query(ChaincodeStub stub, String queryString) {
        String result = "[";
        QueryResultsIterator<KeyValue> rows = stub.getQueryResult(queryString);
        while (rows.iterator().hasNext()) {
            String v = rows.iterator().next().getStringValue();
            if(v != null && v.trim().length() > 0) {
                result = result.concat(v);
                if (rows.iterator().hasNext())
                    result = result.concat(",");
            }
        }
        return result.concat("]");
    }
}
