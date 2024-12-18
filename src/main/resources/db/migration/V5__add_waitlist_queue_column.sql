ALTER TABLE conferences 
ADD COLUMN IF NOT EXISTS waitlist_queue_id VARCHAR(255); 