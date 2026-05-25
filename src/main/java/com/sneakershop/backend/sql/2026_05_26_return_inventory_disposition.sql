-- Nâng cấp theo dõi hàng hoàn đã đi đâu sau khi duyệt/hoàn tiền.
-- Chạy script này nếu database của bạn không bật Hibernate auto update.

ALTER TABLE return_request_item_inspections
    ADD COLUMN IF NOT EXISTS disposition_type VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS warehouse_location VARCHAR(120) NULL;

CREATE TABLE IF NOT EXISTS return_inventory_dispositions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    return_request_id BIGINT NOT NULL,
    return_item_id BIGINT NOT NULL,
    inspection_id BIGINT NULL,
    variant_id BIGINT NOT NULL,
    condition_status VARCHAR(30) NOT NULL,
    disposition_type VARCHAR(40) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    restock_quantity INT NOT NULL DEFAULT 0,
    non_resellable_quantity INT NOT NULL DEFAULT 0,
    responsibility VARCHAR(40) NULL,
    warehouse_location VARCHAR(120) NULL,
    note TEXT NULL,
    created_by VARCHAR(120) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_return_disposition_request (return_request_id),
    INDEX idx_return_disposition_item (return_item_id),
    INDEX idx_return_disposition_variant (variant_id),
    INDEX idx_return_disposition_type (disposition_type),
    CONSTRAINT fk_return_disposition_request FOREIGN KEY (return_request_id) REFERENCES return_requests(id),
    CONSTRAINT fk_return_disposition_item FOREIGN KEY (return_item_id) REFERENCES return_request_items(id),
    CONSTRAINT fk_return_disposition_inspection FOREIGN KEY (inspection_id) REFERENCES return_request_item_inspections(id),
    CONSTRAINT fk_return_disposition_variant FOREIGN KEY (variant_id) REFERENCES product_variant(id)
);
