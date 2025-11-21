package org.walrex.infrastructure.adapters.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
@Entity
@Table(name="tbusuarios", schema = "seguridad")
public class UsuarioEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "id_empleado", insertable = false, updatable = false)
    private Long idEmpleado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_empleado", referencedColumnName = "id_personal")
    private EmpleadoEntity empleado;

    @Column(name = "no_usuario")
    private String nameUser;

    @Column(name = "no_passwd")
    private String noPassword;

    @Column(name = "il_estado")
    private Boolean status;

    @Column(name = "idrol_sistema")
    private Integer idRol;

    @Column(name = "fec_ingreso")
    private LocalDate fecIngreso;

    @Column(name = "fec_inactivo")
    private LocalDate fecInactivo;

    @Column(name = "state_default")
    private Integer stateDefault;

    @Column(name = "use_app")
    private Integer useApp;
}
