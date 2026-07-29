Left Joins
We've learned that by default SQL removes the rows which doesn't match while joining tables.

However, if we wish to join two tables whose rows doesn't match, we can do that using LEFT JOIN.
When two tables are joined using 'LEFT JOIN', and if the rows don't match,

All the rows in the first table(left) will be kept as such and
Whenever a row doesn't a corresponding row in the second table (right), those columns will be kept blank.
Below is the query to join the table 'customer' and 'order' using LEFT JOIN

     SELECT *
     FROM customer
     LEFT JOIN order
     ON customer.cust_id = order.cust_id;
Task
Write a query to do the following:

JOIN the tables 'student' and 'course' using 'Course_id' to match both the tables and output the joined table.
LEFT JOIN the tables 'student' and 'course' using 'Course_id' to match both the tables and output the joined table.