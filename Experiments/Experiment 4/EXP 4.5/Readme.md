Joins Practice-3
Okay, let's put your SQL JOIN knowledge to the test with some practical problems. We'll use the following tables:

1. customers Table:

customer_id	    customer_name	        city
1	            Alice Smith	            New York
2	            Bob Johnson	            Los Angeles
3	            Carol Williams	        Chicago
4	            David Brown	            Houston
5	            Emily Davis	            Phoenix
6	            Luffy	                NULL

2. orders Table:

order_id	customer_id	  product_name	 order_date	  quantity
1	        1              	Laptop	    2024-01-15	1
2       	1	            Mouse	    2024-01-15	2
3	        2	            Keyboard	2024-01-20	1
4	        3	            Monitor	    2024-01-22	1
5	        7	            Webcam	    2023-02-12	3

3. products Table:

product_id	product_name	category_id	price
1	        Laptop	            1	    1200
2	        Mouse	            2	    25
3	        Keyboard        	2	    75
4	        Monitor	            1	    300
5	        Webcam	            2	    60
6	        Tablet	            3	    250

4. categories Table:

category_id	category_name
1	    Electronics
2	    Accessories
3	    Tablets

5. employees Table:

employee_id	employee_name	manager_id	department
1	            John Doe	NULL	Sales
2	            Jane Smith	1	Sales
3	            Peter Jones	1	Marketing
4	            Mary Green	3	Marketing
5	            Raj	        2	Sales

Problems:

1.Employee and Manager Names: Display a list of employee names along with their manager's names. Use the 'employees' table provided above.

2.Every Possible Combination: Show every possible combination of 'customer_name' from the 'customers' table and 'product_name' from the 'products' table.