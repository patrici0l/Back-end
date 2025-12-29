package com.lucero.backend.controllers;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
// Quitamos el import general de layout.element para evitar conflictos con 'Table'
// import com.itextpdf.layout.element.*; 

import com.lucero.backend.models.*;
import com.lucero.backend.repositories.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/programador/reportes")
@CrossOrigin(origins = "*")
public class ReportesProgramadorController {

    @Autowired
    private AsesoriaRepository asesoriaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ProgramadorRepository programadorRepository;

    // =====================
    // AUX: Programador actual
    // =====================
    private Programador obtenerProgramadorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Usuario u = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return programadorRepository.findByUsuarioId(u.getId())
                .orElseThrow(() -> new RuntimeException("No eres programador"));
    }

    // =====================
    // PDF
    // =====================
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> reportePdf() {
        Programador p = obtenerProgramadorActual();
        List<Asesoria> asesorias = asesoriaRepository.findByProgramadorIdOrderByFechaAscHoraAsc(p.getId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            doc.add(new Paragraph("Reporte de Asesorías")
                    .setBold().setFontSize(16));
            doc.add(new Paragraph("Programador: " + p.getUsuario().getNombre()));
            doc.add(new Paragraph(" "));

            // DEFINIMOS EL ANCHO DE LAS 5 COLUMNAS
            float[] pointColumnWidths = { 100F, 100F, 150F, 150F, 100F };

            // USAMOS EL NOMBRE COMPLETO PARA EVITAR CONFLICTO CON EXCEL
            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(pointColumnWidths);

            table.addHeaderCell("Fecha");
            table.addHeaderCell("Hora");
            table.addHeaderCell("Solicitante");
            table.addHeaderCell("Email");
            table.addHeaderCell("Estado");

            for (Asesoria a : asesorias) {
                table.addCell(a.getFecha().toString());
                table.addCell(a.getHora().toString());
                table.addCell(a.getNombreSolicitante());
                table.addCell(a.getEmailSolicitante());
                table.addCell(a.getEstado());
            }

            doc.add(table);
            doc.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=asesorias.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // =====================
    // EXCEL
    // =====================
    @GetMapping("/excel")
    public ResponseEntity<byte[]> reporteExcel() {
        Programador p = obtenerProgramadorActual();
        List<Asesoria> asesorias = asesoriaRepository.findByProgramadorIdOrderByFechaAscHoraAsc(p.getId());

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Asesorías");
            Row header = sheet.createRow(0);

            String[] cols = { "Fecha", "Hora", "Solicitante", "Email", "Estado" };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            int rowIdx = 1;
            for (Asesoria a : asesorias) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getFecha().toString());
                row.createCell(1).setCellValue(a.getHora().toString());
                row.createCell(2).setCellValue(a.getNombreSolicitante());
                row.createCell(3).setCellValue(a.getEmailSolicitante());
                row.createCell(4).setCellValue(a.getEstado());
            }

            workbook.write(baos);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=asesorias.xlsx")
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(baos.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}