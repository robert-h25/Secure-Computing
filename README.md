# Secure-Computing
A Group project where we had to identify security risks in a piece of code and then secure them. 

# Identified risks and how we solved them:

1) SQL Injection. If the query “'or '1'='1” is entered in both the ‘Password’ and the ‘Surname’ fields, all records in the   database are returned. To solve this we parameterised the user input, seperating the SQL query from the values passed by the user.

2) Confidential credentials sent over HTTP. HTTP is highly insecure, sending credentials over this protocol could lead to intercepted data. To solve this we implemented a self-signed SSL certificate and changed the port from 8080 to 8443.

3) Password stored in plaintext. If unauthorised personnel, or authorised personnel with malicious intent accesses the database, they would be able to see or steal passwords of every user in the database. To solve this issue we ussed the SHA-256 algorithm to generate a hash for the password.

