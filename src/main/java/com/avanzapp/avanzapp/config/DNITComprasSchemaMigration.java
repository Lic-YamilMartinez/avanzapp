package com.avanzapp.avanzapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DNITComprasSchemaMigration implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DNITComprasSchemaMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                return;
            }
        }

        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ADD COLUMN IF NOT EXISTS fuente varchar(10)");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ADD COLUMN IF NOT EXISTS timbrado varchar(30)");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ADD COLUMN IF NOT EXISTS monto_compra numeric(18,2)");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ADD COLUMN IF NOT EXISTS monto_iva numeric(18,2)");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ADD COLUMN IF NOT EXISTS monto_pagado numeric(18,2)");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ADD COLUMN IF NOT EXISTS ruc_titular varchar(25)");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ADD COLUMN IF NOT EXISTS rubro_gastos varchar(500)");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ADD COLUMN IF NOT EXISTS observacion varchar(1000)");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ADD COLUMN IF NOT EXISTS gastos numeric(18,2)");

        jdbcTemplate.execute("UPDATE dnit_compra_cabecera SET fuente = 'IVA' WHERE fuente IS NULL");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ALTER COLUMN fuente SET DEFAULT 'IVA'");
        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera ALTER COLUMN fuente SET NOT NULL");

        jdbcTemplate.execute("ALTER TABLE dnit_compra_cabecera DROP CONSTRAINT IF EXISTS uk_dnit_compras_usuario_periodo_numero");
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_dnit_compras_fuente_proveedor_numero
                ON dnit_compra_cabecera (
                    usuario_id,
                    periodo_mes,
                    periodo_anio,
                    fuente,
                    COALESCE(proveedor_ruc, ''),
                    nro_comprobante
                )
                """);
    }
}
