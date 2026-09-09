

CREATE TABLE Orders (
    Order_ID NUMBER PRIMARY KEY,
    Customer_Name VARCHAR2(50),
    Amount NUMBER(10,2)
);


INSERT INTO Orders VALUES (101, 'Amit', 5000);
INSERT INTO Orders VALUES (102, 'Rahul', 15000);
INSERT INTO Orders VALUES (103, 'Priya', 8000);
INSERT INTO Orders VALUES (104, 'Neha', 25000);
INSERT INTO Orders VALUES (105, 'Ravi', 12000);

DECLARE

 
    CURSOR order_cursor IS
        SELECT Order_ID, Customer_Name, Amount
        FROM Orders;

BEGIN

    FOR order_rec IN order_cursor LOOP


        IF order_rec.Amount > 10000 THEN

            DBMS_OUTPUT.PUT_LINE(
                'Order ID: ' || order_rec.Order_ID ||
                ' | Customer: ' || order_rec.Customer_Name ||
                ' | Amount: ' || order_rec.Amount ||
                ' | High Value'
            );

        END IF;

    END LOOP;

END;
