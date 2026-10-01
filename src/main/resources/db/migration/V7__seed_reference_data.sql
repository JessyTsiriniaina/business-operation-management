-- V7: Seed reference data (departments + request types)
-- Idempotent: safe to re-run via ON CONFLICT DO NOTHING.
-- Intentionally seeds NO user rows: the first ADMIN must be created
-- out-of-band (see BOOTSTRAP_ADMIN.local.md), never committed.

INSERT INTO departments (name, description) VALUES
    ('RH', 'Ressources humaines'),
    ('Finance', 'Gestion financiere et comptabilite'),
    ('IT', 'Systemes d''information et support technique'),
    ('Logistique', 'Achats, stocks et operations logistiques'),
    ('Administration', 'Direction et services administratifs')
ON CONFLICT DO NOTHING;

INSERT INTO request_types (name, description, active, created_at) VALUES
    ('LEAVE', 'Demande de conge', TRUE, now()),
    ('EQUIPMENT', 'Demande de materiel informatique', TRUE, now()),
    ('PURCHASE', 'Demande d''achat', TRUE, now()),
    ('IT_SUPPORT', 'Demande d''intervention IT', TRUE, now()),
    ('OTHER', 'Autre demande', TRUE, now())
ON CONFLICT DO NOTHING;
