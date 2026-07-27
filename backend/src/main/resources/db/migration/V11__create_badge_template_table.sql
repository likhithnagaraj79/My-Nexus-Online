-- Exactly one row ever exists — a single shared badge print layout that Crew designs once via
-- the drag-and-drop template editor, applying to every exhibitor's printed badge afterward.
-- Positions are percentages (0-100) of the physical 10cm x 15cm badge so the same template
-- renders correctly at any editor preview scale and at true print size; font sizes are in
-- points, honored consistently by both screen and print CSS.
CREATE TABLE badge_templates (
    id                        UUID PRIMARY KEY,
    name_x_percent            DOUBLE PRECISION NOT NULL,
    name_y_percent            DOUBLE PRECISION NOT NULL,
    name_font_size_pt         DOUBLE PRECISION NOT NULL,
    name_bold                 BOOLEAN NOT NULL,
    designation_x_percent     DOUBLE PRECISION NOT NULL,
    designation_y_percent     DOUBLE PRECISION NOT NULL,
    designation_font_size_pt  DOUBLE PRECISION NOT NULL,
    designation_bold          BOOLEAN NOT NULL,
    company_x_percent         DOUBLE PRECISION NOT NULL,
    company_y_percent         DOUBLE PRECISION NOT NULL,
    company_font_size_pt      DOUBLE PRECISION NOT NULL,
    company_bold              BOOLEAN NOT NULL,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL
);
