-- 1.Employee and Manager Names: Display a list of employee names along with their manager's names. Use the 'employees' table provided.

-- 2.Every Possible Combination: Show every possible combination of 'customer_name' from the 'customers' table and 'product_name' from the 'products' table.

select e.employee_name as Employee, m.employee_name as Manager
from employees as e
left join
employees as m
on e.manager_id = m.employee_id;


select c.customer_name ,p.product_name from customers as c
cross join
products as p;
