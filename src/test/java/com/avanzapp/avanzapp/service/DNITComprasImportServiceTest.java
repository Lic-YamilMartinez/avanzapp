package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.dto.ImportResultDTO;
import com.avanzapp.avanzapp.model.DNITCompraCabecera;
import com.avanzapp.avanzapp.model.FuenteCompra;
import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.repository.DNITCompraCabeceraRepository;
import com.avanzapp.avanzapp.repository.UsuarioRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DNITComprasImportServiceTest {

    @Mock
    private DNITCompraCabeceraRepository cabeceraRepo;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private DNITComprasImportService service;

    @Test
    void importaIrpMensualEIgnoraFilasPago() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setRol("CLIENT");

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(cabeceraRepo
                .findFirstByUsuarioIdAndPeriodoMesAndPeriodoAnioAndFuenteAndProveedorRucAndNroComprobante(
                        anyLong(), anyInt(), anyInt(), eq(FuenteCompra.IRP), anyString(), anyString()
                ))
                .thenReturn(Optional.empty());
        when(cabeceraRepo.save(any(DNITCompraCabecera.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        byte[] contenido;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Hoja1");
            Row header = sheet.createRow(6);
            String[] headers = {
                    "FECHA",
                    "RUC o CI del Vendedor",
                    "Nombre y Apellido o Razón Social del Vendedor",
                    "Numero del Documento",
                    "Numero del Timbrado",
                    "Tipo del Doc.",
                    "Monto de Compra",
                    "Monto Iva",
                    "Monto Pagado",
                    "Total Comprobante",
                    "Ruc del Titular",
                    "Rubro de Gastos",
                    "Observación",
                    "Gastos"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row factura = sheet.createRow(7);
            factura.createCell(0).setCellValue(java.sql.Date.valueOf(LocalDate.of(2026, 11, 5)));
            factura.createCell(1).setCellValue("80008811-5");
            factura.createCell(2).setCellValue("PROVEEDOR TEST");
            factura.createCell(3).setCellValue("001-001-0001234");
            factura.createCell(4).setCellValue(15039141);
            factura.createCell(5).setCellValue("Factura Electrónica");
            factura.createCell(6).setCellValue(100000);
            factura.createCell(7).setCellValue(10000);
            factura.createCell(8).setCellValue(110000);
            factura.createCell(9).setCellValue(110000);
            factura.createCell(10).setCellValue("1810050-3");
            factura.createCell(11).setCellValue("1-Egresos directamente relacionados con la actividad gravada");
            factura.createCell(12).setCellValue("Compra de prueba");
            factura.createCell(13).setCellValue(100000);

            Row pago = sheet.createRow(8);
            pago.createCell(0).setCellValue(java.sql.Date.valueOf(LocalDate.of(2026, 11, 5)));
            pago.createCell(3).setCellValue("001-001-0001234");
            pago.createCell(5).setCellValue("Pago");
            pago.createCell(8).setCellValue(110000);
            pago.createCell(9).setCellValue(110000);

            workbook.write(out);
            contenido = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "LIBRO EGRESO NOVIEMBRE 2026.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                contenido
        );

        ImportResultDTO resultado = service.importarCompras(file, 10L, false);

        assertEquals("IRP", resultado.getFuente());
        assertEquals(11, resultado.getPeriodoMes());
        assertEquals(2026, resultado.getPeriodoAnio());
        assertEquals(2, resultado.getTotalLeidas());
        assertEquals(1, resultado.getInsertadas());
        assertEquals(1, resultado.getIgnoradas());
        assertEquals(1, resultado.getPeriodos().size());
        assertEquals("11/2026", resultado.getPeriodos().get(0));

        ArgumentCaptor<DNITCompraCabecera> captor =
                ArgumentCaptor.forClass(DNITCompraCabecera.class);
        verify(cabeceraRepo, times(1)).save(captor.capture());

        DNITCompraCabecera guardada = captor.getValue();
        assertEquals(FuenteCompra.IRP, guardada.getFuente());
        assertEquals("80008811-5", guardada.getProveedorRuc());
        assertEquals("001-001-0001234", guardada.getNroComprobante());
        assertEquals(LocalDate.of(2026, 11, 5), guardada.getFechaEmision());
        assertEquals("11/2026", guardada.getPeriodoEmision());
        assertEquals("1-Egresos directamente relacionados con la actividad gravada", guardada.getRubroGastos());
        assertEquals(0, guardada.getTotalComprobante().compareTo(new java.math.BigDecimal("110000.0")));
    }
}
