-- V2: Add indexes for query optimization
-- This migration creates indexes on commonly queried columns and JSONB fields
-- to optimize query performance for the PostgreSQL projection

-- Booking table indexes
CREATE INDEX idx_booking_reference ON booking(booking_reference);
CREATE INDEX idx_booking_status ON booking(status);
CREATE INDEX idx_booking_lead_pax ON booking(lead_pax_id);

-- Booking pax table indexes
CREATE INDEX idx_booking_pax_email ON booking_pax(email);
CREATE INDEX idx_booking_pax_booking_id ON booking_pax(booking_id);

-- Booking product table indexes
CREATE INDEX idx_booking_product_booking_id ON booking_product(booking_id);
CREATE INDEX idx_booking_product_type ON booking_product(product_type);

-- GIN index for JSONB queries on product_details
-- This enables efficient queries on nested JSON fields
CREATE INDEX idx_booking_product_details ON booking_product USING GIN (product_details);

-- Add comments for documentation
COMMENT ON INDEX idx_booking_reference IS 'Unique constraint enforcement and fast lookup by booking reference';
COMMENT ON INDEX idx_booking_status IS 'Optimize queries filtering by booking status';
COMMENT ON INDEX idx_booking_lead_pax IS 'Optimize queries filtering by lead passenger';
COMMENT ON INDEX idx_booking_pax_email IS 'Optimize queries searching bookings by passenger email';
COMMENT ON INDEX idx_booking_pax_booking_id IS 'Optimize joins between booking and pax tables';
COMMENT ON INDEX idx_booking_product_booking_id IS 'Optimize joins between booking and product tables';
COMMENT ON INDEX idx_booking_product_type IS 'Optimize queries filtering by product type (Transfer, Activity, Hotel)';
COMMENT ON INDEX idx_booking_product_details IS 'GIN index for efficient JSONB queries on product-specific fields';
