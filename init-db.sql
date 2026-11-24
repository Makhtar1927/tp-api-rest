-- Script de configuration PostgreSQL pour le TP REST (Section II)
-- À exécuter avec un compte administrateur PostgreSQL (ex: postgres) :
-- psql -U postgres -h localhost -f init-db.sql

-- 1. Création du rôle applicatif non-superutilisateur tp_api
DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'tp_api') THEN
      CREATE ROLE tp_api WITH LOGIN PASSWORD 'tp_api_password_secure';
   END IF;
END
$do$;

ALTER ROLE tp_api WITH NOSUPERUSER NOCREATEDB NOCREATEROLE;

-- 2. Création de la base de données tp_rest appartenant à tp_api
SELECT 'CREATE DATABASE tp_rest OWNER tp_api'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tp_rest')\gexec

-- 3. Configuration des privilèges sur le schéma public de la base tp_rest
\c tp_rest
GRANT ALL ON SCHEMA public TO tp_api;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO tp_api;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO tp_api;
