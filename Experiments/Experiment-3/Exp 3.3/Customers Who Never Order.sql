-- Write your PostgreSQL query statement below



select name as Customers from Customers 
where id not In(
select customerId from Orders
)
