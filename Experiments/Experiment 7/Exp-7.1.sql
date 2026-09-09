
CREATE TABLE STAFF (
    EMP_ID NUMBER PRIMARY KEY,
    NAME VARCHAR2(50),
    SALARY NUMBER(10,2)
);

INSERT INTO STAFF VALUES (101, 'Amit', 45000);
INSERT INTO STAFF VALUES (102, 'Rahul', 75000);
INSERT INTO STAFF VALUES (103, 'Priya', 60000);
INSERT INTO STAFF VALUES (104, 'Neha', 90000);
INSERT INTO STAFF VALUES (105, 'Ravi', 55000);
INSERT INTO STAFF VALUES (106, 'Simran', 85000);

SELECT * FROM STAFF;

declare 
cursor staff_cursor is 
SELECT NAME,SALARY from STAFF
order by SALARY desc;

s_name STAFF.NAME%Type;
s_salary STAFF.SALARY%Type;
v_count NUMBER := 0;

BEGIN
    open staff_cursor;
  

    LOOP
        FETCH staff_cursor INTO s_name, s_salary;

        EXIT WHEN v_count = 5;

        v_count := v_count + 1;

        DBMS_OUTPUT.PUT_LINE(
            'Name: ' || s_name || '  Salary: ' || s_salary
        );
    END LOOP;

    close staff_cursor;

    End;


