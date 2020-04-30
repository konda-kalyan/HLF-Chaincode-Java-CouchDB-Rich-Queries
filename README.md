Employee structure: {empID, empName, department, salary, location}

Key: empID

Indexes - Total 5 indexes:
  {empName}
  {salary}
  {location}
  {department, location}
  {empName, department, salary}

Rich queries:
  1.	Get employee by ID 	(this is regular query operation which also works in LevelDB)
  2.	Get employees by name (Ad hoc query)
  3.	Get employees whose salary is greater than X amount (Parameterized query)
  4.	Get employees whose employee IDs are in given range (this is regular query operation which also works in LevelDB)
  5.	Pagination: Get 3 employees at a time


Refer scripts/install_instantiate_invoke_couchdb_java_chaincode.sh	for CLI peer chaincode commands

Queries - Contact at 'konda.kalyan@gmail.com'
