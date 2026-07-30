-- We have a student table that also stores the Course_id of a student's favorite course. Our task has two parts related to using a SELF JOIN:

--     Find pairs of students that belong to the same department.
--     Identify students who have chosen the same Course_id as their favorite. Display the St_id, St_Name, and Course_id and order it in increasing Course_id.

select s1.St_id,s1.St_Name,s1.Department,s2.St_id,s2.St_Name,s2.Department
from student as s1
join student as s2
on s1.Department = s2.Department and s1.St_id != s2.St_id;


select s1.St_id,s1.St_Name,s1.Course_id
from student as s1
join student as s2
on 
s1.Course_id = s2.Course_id
where s1.St_id is not s2.St_id
order by s1.Course_id;
