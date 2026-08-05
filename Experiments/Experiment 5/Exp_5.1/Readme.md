Problem Link : https://www.codechef.com/learn/course/sql-intermediate/SQ00BS09/problems/GSQ85D?tab=statement

Practice Problem - 4
Write a query to do the following

Find out what percentage of the total revenue (sum of all orders) is contributed by American Cuisine. - Alias the percentage calculation function as 'American_Revenue'. Round the final value to 2 decimal places
Expected output
┌──────────────────┐
│ American_Revenue │
├──────────────────┤
│ 27.75            │
└──────────────────┘
Your table 'Orders' has the following fields:

order_id INT PRIMARY KEY,
item_name VARCHAR(255),
cuisine VARCHAR(255) ,
category VARCHAR(255) ,
price DECIMAL(10,2),
status VARCHAR(255)