Union

Till now, we have joined tables by columns. Now let’s join the data by rows.
We use the concept of UNION to do that.
UNION helps us to place a table right on top of another table.
There are set of criteria to be followed while appending the tables:

The number of columns of the tables should be same.
The data type of the table should be of the same order of that of the first table.
Below is query to combine tables 'Mfg_Ind' and 'Mfg_Int'

 SELECT *
 FROM Mfg_Ind
 UNION
 SELECT *
 FROM Mfg_Int;


 Task
Write a query using union to stack the table 'Arts' over 'Science' and output the final table

Note:
The UNION statmente removes the duplicate data in the new table formed.