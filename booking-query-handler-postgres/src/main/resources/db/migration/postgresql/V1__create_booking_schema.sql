-- V1: Create booking schema with tables for booking, booking_pax, and booking_product
-- This migration creates the core PostgreSQL projection schema using a hybrid approach
-- with normalized tables for core entities and JSONB columns for polymorphic product data

-- Create booking table (main entity)
CREATE TABLE booking (
    booking_id UUID PRIMARY KEY,
    booking_reference VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    lead_pax_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);

-- Create booking_pax table (normalized passengers)
CREATE TABLE booking_pax (
    pax_id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    age INTEGER NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    pax_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_booking_pax_booking 
        FOREIGN KEY (booking_id) 
        REFERENCES booking(booking_id) 
        ON DELETE CASCADE
);

-- Create booking_product table (normalized products with JSONB for polymorphic data)
CREATE TABLE booking_product (
    product_id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    search_id UUID NOT NULL,
    search_created_at TIMESTAMP NOT NULL,
    product_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    hash VARCHAR(255) NOT NULL,
    paxes_ids JSONB NOT NULL,
    product_details JSONB NOT NULL,
    CONSTRAINT fk_booking_product_booking 
        FOREIGN KEY (booking_id) 
        REFERENCES booking(booking_id) 
        ON DELETE CASCADE
);

-- Add comments for documentation
COMMENT ON TABLE booking IS 'Main booking entity with optimistic locking';
COMMENT ON TABLE booking_pax IS 'Normalized passenger information for bookings';
COMMENT ON TABLE booking_product IS 'Normalized product information with JSONB for type-specific details';
COMMENT ON COLUMN booking.version IS 'Optimistic locking version field';
COMMENT ON COLUMN booking_product.paxes_ids IS 'JSONB array of passenger UUIDs associated with this product';
COMMENT ON COLUMN booking_product.product_details IS 'JSONB column storing polymorphic product-specific data (Transfer, Activity, Hotel)';
