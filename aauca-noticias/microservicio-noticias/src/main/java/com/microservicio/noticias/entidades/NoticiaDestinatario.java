package com.microservicio.noticias.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "noticia_destinatarios")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoticiaDestinatario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "noticia_id", nullable = false)
    private Long noticiaId;

    @Column(name = "tipo_id", nullable = false)
    private Long tipoId;

    @Column(name = "valor", length = 100)
    private String valor;

    @Column(name = "usuario_ref_id")
    private Long usuarioRefId;

    @Column(name = "habitacion_ref_id")
    private Long habitacionRefId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_id", insertable = false, updatable = false)
    private TipoDestinatario tipo;
}