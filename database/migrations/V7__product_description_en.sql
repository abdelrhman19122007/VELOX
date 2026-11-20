-- Products get an English description next to the Arabic one; each app language shows its own.
ALTER TABLE products ADD COLUMN description_en VARCHAR(500);

-- Menus imported before this kept both languages in one field as "عربي || English": split them.
UPDATE products
SET description_en = NULLIF(TRIM(SPLIT_PART(description, '||', 2)), ''),
    description    = NULLIF(TRIM(SPLIT_PART(description, '||', 1)), '')
WHERE description LIKE '%||%';
