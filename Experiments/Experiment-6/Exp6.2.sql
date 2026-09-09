create MATERIALIZED VIEW MV_EMP AS
Select emp_id, emp_name from employees
WITH NO DATA

REFRESH MATERIALIZED VIEW MV_EMP

select * from MV_EMP