/* Write a query to do the following. Try and use the concept of sub-queries.
- You need to output details of the dish - 'f_name', 'f_cost' and 'f_type' ONLY if the following condition is satisfied
- Average rating of the dish is greater than or equal to 4 */ 
select
f_name,
f_cost,
f_type
from food f
where (
    select avg(f_rating) from ratings where f_id = f.f_id
) >= 4;