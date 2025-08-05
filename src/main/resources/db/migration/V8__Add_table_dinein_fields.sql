-- Add new fields to tables table
ALTER TABLE tables 
ADD COLUMN qr_code VARCHAR(255),
ADD COLUMN location VARCHAR(255),
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Add table relationship and order type to orders table
ALTER TABLE orders 
ADD COLUMN table_id BIGINT,
ADD COLUMN order_type VARCHAR(50) DEFAULT 'DELIVERY';

-- Add foreign key constraint
ALTER TABLE orders 
ADD CONSTRAINT fk_orders_table 
FOREIGN KEY (table_id) REFERENCES tables(id);

-- Update existing orders to have DELIVERY type
UPDATE orders SET order_type = 'DELIVERY' WHERE order_type IS NULL;

-- Create index for better performance
CREATE INDEX idx_orders_table_id ON orders(table_id);
CREATE INDEX idx_tables_status ON tables(status);
CREATE INDEX idx_orders_order_type ON orders(order_type); 