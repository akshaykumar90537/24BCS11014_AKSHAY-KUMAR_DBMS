INSERT INTO roles (name) VALUES
    ('ROLE_USER'),
    ('ROLE_ADMIN'),
    ('ROLE_MANAGER'),
    ('ROLE_EMPLOYEE')
ON CONFLICT (name) DO NOTHING;

INSERT INTO sla_policies (priority, target_hours, reopen_window_days) VALUES
    ('CRITICAL', 4, 7),
    ('HIGH', 24, 7),
    ('MEDIUM', 72, 7),
    ('LOW', 120, 7)
ON CONFLICT (priority) DO NOTHING;

INSERT INTO departments (name, description, active) VALUES
    ('Facilities & Maintenance', 'Handles plumbing, electrical, HVAC, building repairs, and campus infrastructure.', TRUE),
    ('IT & Network Services', 'Handles Wi-Fi, network outages, lab hardware, portals, and software access.', TRUE),
    ('Hostel & Sanitation', 'Handles hostel accommodation, cleanliness, water supply, and mess hygiene.', TRUE),
    ('Academic & Administration', 'Handles examinations, timetabling, documentation, and administrative grievances.', TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, default_department_id, active) VALUES
    ('Water & Plumbing', 'Water leakage, pipe bursts, shortage, or drainage issues.',
     (SELECT id FROM departments WHERE name = 'Facilities & Maintenance'), TRUE),
    ('Electrical & Power', 'Power outages, lighting faults, or electrical safety hazards.',
     (SELECT id FROM departments WHERE name = 'Facilities & Maintenance'), TRUE),
    ('Network & Wi-Fi', 'Internet downtime, slow connectivity, or access point issues.',
     (SELECT id FROM departments WHERE name = 'IT & Network Services'), TRUE),
    ('Software & Portal Access', 'Login errors, ERP issues, or academic portal bugs.',
     (SELECT id FROM departments WHERE name = 'IT & Network Services'), TRUE),
    ('Hostel & Cleanliness', 'Room maintenance, sanitation, pest control, or waste disposal.',
     (SELECT id FROM departments WHERE name = 'Hostel & Sanitation'), TRUE),
    ('Administrative & Academic', 'Certificate delays, timetable conflicts, or general administrative issues.',
     (SELECT id FROM departments WHERE name = 'Academic & Administration'), TRUE)
ON CONFLICT (name) DO NOTHING;

