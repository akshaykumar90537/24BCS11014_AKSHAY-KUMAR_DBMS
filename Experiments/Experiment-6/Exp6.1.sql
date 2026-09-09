CREATE VIEW Unsold_Items AS
SELECT ProductName, Category
FROM Products p
WHERE NOT EXISTS (
    SELECT 1
    FROM Order_Details od
    WHERE p.ProductID = od.ProductID
);

select *from Unsold_Items