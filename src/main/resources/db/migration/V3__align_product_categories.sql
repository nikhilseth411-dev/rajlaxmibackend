-- Keep the selected database category and the product category enum aligned.
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_product_category_check;

INSERT INTO categories (name, slug, description, is_active, sort_order, parent_id, created_at)
SELECT 'Bracelets', 'gold-bracelets', 'Gold bracelets and wrist jewellery', true, 8, id, NOW()
FROM categories
WHERE slug = 'gold-jewellery'
ON CONFLICT (slug) DO NOTHING;

UPDATE products p
SET product_category = CASE
    WHEN c.slug = 'mangalsutra' THEN 'MANGALSUTRA'
    WHEN c.slug = 'gold-earrings' THEN 'EARRINGS'
    WHEN c.slug = 'gold-necklaces' THEN 'NECKLACES'
    WHEN c.slug = 'gold-bangles' THEN 'BANGLES'
    WHEN c.slug = 'gold-bracelets' THEN 'BRACELETS'
    WHEN c.slug = 'gold-rings' THEN 'RINGS'
    WHEN c.slug = 'gold-pendants' THEN 'PENDANTS'
    WHEN c.slug = 'gold-chains' THEN 'CHAINS'
    WHEN c.slug = 'gold-anklets' THEN 'ANKLETS'
    WHEN c.slug = 'diamond-jewellery' THEN 'DIAMOND_JEWELLERY'
    WHEN c.slug = 'bridal-collection' THEN 'BRIDAL_COLLECTION'
    WHEN c.slug = 'temple-jewellery' THEN 'TEMPLE_JEWELLERY'
    WHEN c.slug = 'antique-jewellery' THEN 'ANTIQUE_JEWELLERY'
    WHEN c.slug = 'silver-collection' THEN 'SILVER_COLLECTION'
    WHEN c.slug = 'gold-jewellery' THEN 'GOLD_JEWELLERY'
    ELSE p.product_category
END
FROM categories c
WHERE p.category_id = c.id;

ALTER TABLE products ADD CONSTRAINT products_product_category_check CHECK (
    product_category IS NULL OR product_category IN (
        'GOLD_JEWELLERY', 'DIAMOND_JEWELLERY', 'BRIDAL_COLLECTION',
        'TEMPLE_JEWELLERY', 'ANTIQUE_JEWELLERY', 'SILVER_COLLECTION',
        'MANGALSUTRA', 'EARRINGS', 'NECKLACES', 'BANGLES', 'RINGS',
        'PENDANTS', 'CHAINS', 'BRACELETS', 'ANKLETS'
    )
);
