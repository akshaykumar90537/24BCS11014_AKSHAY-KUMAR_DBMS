Self Joins
Sometimes, you need to join a table to itself. This is called a self join. It's not a distinct type of JOIN like INNER, LEFT, RIGHT, or FULL; rather, it's a technique where you use any of those join types, but the left and right tables are the same table. You treat the table as if it were two separate tables, typically using aliases to distinguish them.

Self joins are crucial when you need to compare rows within the same table or when you have hierarchical data within a single table. Think of situations like:

Employee-Manager Relationships: A single employees table might have an employee_id column and a manager_id column (which also references employee_id). To find each employee's manager, you'd join the table to itself.
Finding Pairs: You might want to find pairs of employees who live in the same city.
Parts List Consider a scenario in mechanical industry where a self-join helps retrieve hierarchical relationships in a Bill of Materials (BOM) by linking components to their parent assemblies within the same table.
Here's the general idea, using aliases (a and b in this case):

SELECT
    e1.employee_name AS Employee,
    e2.employee_name AS Manager
FROM
    employees AS e1  -- Alias for employee
INNER JOIN
    employees AS e2  -- Alias for manager
ON
    e1.manager_id = e2.employee_id;
Key Points:
1.Table Aliases: You must use table aliases (like a and b above) to differentiate between the "two" instances of the same table. Without aliases, the database wouldn't know which instance you're referring to.

2.Join Condition: The ON clause is critical. It defines how rows within the same table are related. This is where you specify the relationship you're investigating (e.g., a.manager_id = b.employee_id). The Join Condition cannot have same name on both sides of comparison, that will include all the entries in the table.

3.Join Types: Although its mostly used with the INNER JOIN, any of the previously dicussed joins could also be used in SELF JOIN.

Task
We have a student table that also stores the Course_id of a student's favorite course. Our task has two parts related to using a SELF JOIN:

Find pairs of students that belong to the same department.
Identify students who have chosen the same Course_id as their favorite. Display the St_id, St_Name, and Course_id and order it in increasing Course_id.