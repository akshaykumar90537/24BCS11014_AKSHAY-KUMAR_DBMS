-- Write a query to do the following:

-- FULL OUTER JOIN the 'student' and 'course' tables using 'Course_id' to match the tables. Output the joined table.


SELECT * from student as s
FULL OUTER JOIN
course as c
on
s.Course_id = c.Course_id;
