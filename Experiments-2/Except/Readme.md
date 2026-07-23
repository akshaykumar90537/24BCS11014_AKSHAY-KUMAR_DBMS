Except
Previously we learned the concept of INTERSECT, now lets see how EXCEPT works.
EXCEPT is directly opposite to that of INTERSECT.
EXCEPT retrieves unique records from the first SELECT statement that are not present in the output of the second SELECT statement.

Below is the format for the same:

     SELECT * FROM table_1
     EXCEPT
     SELECT * FROM table_2;
Task
Consider the same supermarket database we used in the previous problem.
Write a query to output the name of the fruits (f_name) from the table 'fruit' which are not present in the table inventory.
f_name column has the name of the fruits and inv_name has the name of the items in inventory.

Expected output
┌────────┐
│ f_name │
├────────┤
│ Apple  │
│ Mango  │
│ Orange │
└────────┘![alt text](Except_output.png)