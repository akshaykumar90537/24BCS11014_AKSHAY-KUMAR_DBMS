Full Outer Joins
Now that we've explored LEFT JOIN and RIGHT JOIN, let's look at a join that combines both: the FULL OUTER JOIN. A FULL OUTER JOIN (sometimes just called FULL JOIN) returns all rows from both tables.

Here's a breakdown of how FULL OUTER JOIN works:

Matching Rows: If a row in one table has a matching row in the other table (based on the ON clause), the join combines those rows into a single row in the result.
Unmatched Rows in Left Table: If a row in the left table does not have a match in the right table, the join includes that row. The columns from the right table will be filled with NULL values.
Unmatched Rows in Right Table: If a row in the right table does not have a match in the left table, the join also includes that row. The columns from the left table will be filled with NULL values.
In short, a FULL OUTER JOIN guarantees that every row from both tables appears in the result set, with NULLs used to fill in missing values where there's no match. It's a combination of LEFT JOIN and RIGHT JOIN. It doesn't omit any rows from the tables that are being joined.

Here's the general syntax:

SELECT *
FROM table1
FULL OUTER JOIN table2
ON table1.column = table2.column;
Task
Write a query to do the following:

FULL OUTER JOIN the student and course tables using 'Course_id' to match the tables. Output the joined table.