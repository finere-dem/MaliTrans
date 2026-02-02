-- =============================================================================
-- Compagnies de livraison fictives (Mali) + comptes COMPANY_MANAGER + admin
-- Mot de passe commun (tests) : password123
-- =============================================================================

-- 1) Insérer les compagnies
INSERT INTO delivery_company (name, address, is_active) VALUES
('Trans-Mali Express', 'Avenue Modibo Keita, Bamako', true),
('Bamako Livraison Rapide', 'Quartier Hippodrome, Bamako', true),
('Sahel Transport & Logistique', 'Route de Koulikoro, Bamako', true),
('Mali Express', 'Sikasso, Rue du Marché', true),
('Ségou Trans', 'Ségou, Avenue principale', true),
('Mopti Logistique', 'Mopti, Port fluvial', true),
('Kayes Express', 'Kayes, Gare routière', true),
('Gao Transport', 'Gao, Centre-ville', true),
('Tombouctou Livraisons', 'Tombouctou', true),
('Nioro Trans', 'Nioro du Sahel', true);

-- 2) Créer un compte COMPANY_MANAGER par compagnie (lié par company_id)
--    Connexion : username = manager_1, manager_2, ...  |  mot de passe = password123
--    Hash BCrypt pour "password123" (généré par Spring Security BCryptPasswordEncoder)
INSERT INTO utilisateur (username, password, first_name, last_name, phone, role, company_id, status, enabled)
SELECT
  'manager_' || dc.id,
  '$2a$10$.rqWR.0kf2H9r8V1VzPLPu7TzqE25xbe5ZWlrpU60s1IlwAOys/MS',
  dc.name,
  'Manager',
  '+2237000' || LPAD(dc.id::text, 4, '0'),
  'COMPANY_MANAGER',
  dc.id,
  'ACTIVE',
  true
FROM delivery_company dc
WHERE dc.name IN (
  'Trans-Mali Express',
  'Bamako Livraison Rapide',
  'Sahel Transport & Logistique',
  'Mali Express',
  'Ségou Trans',
  'Mopti Logistique',
  'Kayes Express',
  'Gao Transport',
  'Tombouctou Livraisons',
  'Nioro Trans'
)


AND NOT EXISTS (
  SELECT 1 FROM utilisateur u WHERE u.company_id = dc.id AND u.role = 'COMPANY_MANAGER'
);

-- 3) Compte administrateur (username = admin, mot de passe = password123)
INSERT INTO utilisateur (username, password, first_name, last_name, phone, role, company_id, status, enabled)
SELECT
  'admin',
  '$2a$10$.rqWR.0kf2H9r8V1VzPLPu7TzqE25xbe5ZWlrpU60s1IlwAOys/MS',
  'Admin',
  'MaliTrans',
  '+22300000000',
  'ADMIN',
  NULL,
  'ACTIVE',
  true
WHERE NOT EXISTS (SELECT 1 FROM utilisateur u WHERE u.username = 'admin');

-- 4) Si les managers ou l'admin existent déjà avec un ancien hash, mettre à jour le mot de passe :
--    (à exécuter si la connexion manager_4 / password123 échoue encore)
-- UPDATE utilisateur
-- SET password = '$2a$10$.rqWR.0kf2H9r8V1VzPLPu7TzqE25xbe5ZWlrpU60s1IlwAOys/MS'
-- WHERE (role = 'COMPANY_MANAGER' AND username LIKE 'manager_%') OR username = 'admin';

-- Résumé après exécution :
-- SELECT dc.id, dc.name, u.username, u.phone FROM delivery_company dc
-- LEFT JOIN utilisateur u ON u.company_id = dc.id AND u.role = 'COMPANY_MANAGER'
-- ORDER BY dc.id;
