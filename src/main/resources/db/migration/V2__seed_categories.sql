-- ================================================================
-- V2 Seed: Jewellery Categories for राज लक्ष्मी ज्वेलर्स
-- ================================================================

-- Root categories (parent_id = NULL)
INSERT INTO categories (name, slug, description, is_active, sort_order, parent_id) VALUES
('Gold Jewellery',      'gold-jewellery',     'Premium 18K, 22K, 24K gold jewellery',               true, 1,  NULL),
('Diamond Jewellery',   'diamond-jewellery',  'Certified diamond jewellery',                         true, 2,  NULL),
('Bridal Collection',   'bridal-collection',  'Complete bridal sets and wedding jewellery',           true, 3,  NULL),
('Temple Jewellery',    'temple-jewellery',   'Traditional temple style jewellery',                  true, 4,  NULL),
('Antique Jewellery',   'antique-jewellery',  'Handcrafted antique and vintage jewellery',           true, 5,  NULL),
('Silver Collection',   'silver-collection',  '925 sterling silver jewellery',                       true, 6,  NULL),
('Mangalsutra',         'mangalsutra',        'Traditional and modern mangalsutra designs',           true, 7,  NULL)
ON CONFLICT (slug) DO NOTHING;

-- Sub-categories under Gold Jewellery (parent = Gold Jewellery id)
INSERT INTO categories (name, slug, description, is_active, sort_order, parent_id)
SELECT 'Necklaces', 'gold-necklaces', 'Gold necklaces and haars', true, 1, id FROM categories WHERE slug = 'gold-jewellery'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, slug, description, is_active, sort_order, parent_id)
SELECT 'Earrings', 'gold-earrings', 'Gold earrings - jhumkas, studs, hoops', true, 2, id FROM categories WHERE slug = 'gold-jewellery'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, slug, description, is_active, sort_order, parent_id)
SELECT 'Bangles', 'gold-bangles', 'Gold bangles and kada', true, 3, id FROM categories WHERE slug = 'gold-jewellery'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, slug, description, is_active, sort_order, parent_id)
SELECT 'Rings', 'gold-rings', 'Gold rings - engagement, wedding, fashion', true, 4, id FROM categories WHERE slug = 'gold-jewellery'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, slug, description, is_active, sort_order, parent_id)
SELECT 'Pendants', 'gold-pendants', 'Gold pendants and lockets', true, 5, id FROM categories WHERE slug = 'gold-jewellery'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, slug, description, is_active, sort_order, parent_id)
SELECT 'Chains', 'gold-chains', 'Gold chains', true, 6, id FROM categories WHERE slug = 'gold-jewellery'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, slug, description, is_active, sort_order, parent_id)
SELECT 'Anklets', 'gold-anklets', 'Gold anklets and payal', true, 7, id FROM categories WHERE slug = 'gold-jewellery'
ON CONFLICT (slug) DO NOTHING;
