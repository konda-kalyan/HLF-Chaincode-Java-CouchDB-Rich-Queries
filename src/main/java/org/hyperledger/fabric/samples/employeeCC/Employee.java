package org.hyperledger.fabric.samples.employeeCC;

public final class Employee {
	
    private String empID;
  
    private String empName;
    
    private String department;
    
    private double salary;

    private String location;

    
	public Employee() {
		super();
	}

	public Employee(String empID, String empName, String department, double salary, String location) {
		super();
		this.empID = empID;
		this.empName = empName;
		this.department = department;
		this.salary = salary;
		this.location = location;
	}

	public String getEmpID() {
		return empID;
	}

	public void setEmpID(String empID) {
		this.empID = empID;
	}

	public String getEmpName() {
		return empName;
	}

	public void setEmpName(String empName) {
		this.empName = empName;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public double getSalary() {
		return salary;
	}

	public void setSalary(double salary) {
		this.salary = salary;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}
