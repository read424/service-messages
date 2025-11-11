package org.walrex.infrastructure.adapters.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@Entity
@Table(name = "tbpersonal", schema = "rrhh")
public class EmpleadoEntity {
    @Id
    @Column(name = "id_personal", nullable = false)
    private Long id;

    @Column(name = "id_tipoper")
    private Integer idTipoPersonal;

    @Column(name = "cod_cuspp")
    private String codCUSPP;

    @Column(name = "no_apepat")
    private String primerApellido;

    @Column(name = "no_apemat")
    private String segundoApellido;

    @Column(name = "no_nombres")
    private String nombres;

    @Column(name = "id_tipodoc")
    private Integer idTipoDocumento;

    @Column(name = "nu_doc")
    private String numDocumento;

    @Column(name = "id_sexo")
    private Integer idGender;

    @Column(name = "id_estciv")
    private Integer idEdoCivil;

    @Column(name = "no_direc")
    private String detailAddress;

    @Column(name = "no_refdir")
    private String refAddress;

    @Column(name = "nu_telefo")
    private String numPhone;

    @Column(name = "nu_celular")
    private String numPhone2;

    @Column(name = "no_correo")
    private String email;

    @Column(name = "co_depart")
    private Integer idDepartamento;

    @Column(name = "fe_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "co_provin")
    private Integer idProvincia;

    @Column(name = "co_distri")
    private Integer idDistrito;

    @Column(name = "id_status")
    private Integer idStatus;

    @Column(name = "status")
    private Integer Status;

    @Column(name = "ho_extras")
    private Integer hourExtra;

    @Column(name = "tlf_corporativo")
    private String phoneCorporate;

    @Column(name = "id_det_personal")
    private Integer idDetPersonal;

    @Column(name = "pin2_tad")
    private String pinTAD;
}
