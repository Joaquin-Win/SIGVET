package com.sigvet.model;

import com.sigvet.model.enums.EstadoConsulta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa una consulta médica veterinaria en el sistema SIGVET.
 *
 * <p>Una consulta puede originarse de dos formas (RN-01):</p>
 * <ol>
 *   <li><strong>Con turno previo:</strong> {@link #turno} no es nulo; se crea
 *       mediante {@code sp_registrar_consulta_turno}, que además marca el turno como
 *       {@code Atendido} (RN-12).</li>
 *   <li><strong>Urgencia:</strong> {@link #turno} es nulo; se crea mediante
 *       {@code sp_registrar_consulta_urgencia}.</li>
 * </ol>
 *
 * <p><strong>Regla de negocio crítica RN-07:</strong> las consultas médicas NUNCA se
 * eliminan físicamente. El trigger {@code trg_prevenir_eliminar_consulta} lanza un error
 * si se intenta un DELETE. La baja lógica se realiza con {@code sp_baja_logica_consulta}
 * que cambia el estado a {@link EstadoConsulta#Inactiva}.</p>
 *
 * <p><strong>Auditoría:</strong> el trigger {@code trg_registro_modificacion_consulta}
 * registra automáticamente el veterinario que realizó la última modificación
 * ({@link #veterinarioModif}) y la fecha ({@link #fechaModificacion}).</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code consulta_medica}, PK {@code id_consulta}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Turno
 * @see Mascota
 * @see Veterinario
 * @see ItemReceta
 */
public class ConsultaMedica {

    /** Identificador único de la consulta (PK en BD). */
    private int idConsulta;

    /**
     * Turno asociado a la consulta (nullable).
     * Es nulo cuando la consulta es de urgencia (RN-01).
     */
    private Turno turno;

    /** Mascota paciente (FK {@code id_mascota → mascota}). */
    private Mascota mascota;

    /** Veterinario que realizó la consulta (FK {@code id_veterinario → veterinario}). */
    private Veterinario veterinario;

    /** Fecha y hora de la consulta. */
    private LocalDateTime fecha;

    /** Síntomas reportados y observados (obligatorio, NOT NULL en BD). */
    private String sintomas;

    /** Diagnóstico del veterinario (obligatorio, NOT NULL en BD). */
    private String diagnostico;

    /** Estado lógico de la consulta. */
    private EstadoConsulta estado;

    /** Fecha de la última modificación (nullable; se actualiza por trigger). */
    private LocalDate fechaModificacion;

    /**
     * Veterinario que realizó la última modificación (nullable).
     * FK {@code id_veterinario_modif → veterinario}.
     */
    private Veterinario veterinarioModif;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. Estado por defecto: {@link EstadoConsulta#Activa}.
     */
    public ConsultaMedica() {
        this.estado = EstadoConsulta.Activa;
        this.fecha = LocalDateTime.now();
    }

    /**
     * Constructor completo con todos los atributos de la consulta.
     *
     * @param idConsulta        identificador único (PK en BD)
     * @param turno             turno de origen (puede ser nulo para urgencias)
     * @param mascota           mascota paciente; no puede ser nula
     * @param veterinario       veterinario actuante; no puede ser nulo
     * @param fecha             fecha y hora de la consulta
     * @param sintomas          síntomas reportados; no puede ser nulo ni vacío
     * @param diagnostico       diagnóstico del veterinario; no puede ser nulo ni vacío
     * @param estado            estado lógico de la consulta
     * @param fechaModificacion fecha de la última modificación (nullable)
     * @param veterinarioModif  veterinario que modificó (nullable)
     */
    public ConsultaMedica(int idConsulta, Turno turno, Mascota mascota,
                          Veterinario veterinario, LocalDateTime fecha,
                          String sintomas, String diagnostico, EstadoConsulta estado,
                          LocalDate fechaModificacion, Veterinario veterinarioModif) {
        this.idConsulta = idConsulta;
        this.turno = turno; // Nullable para urgencias (RN-01)
        setMascota(mascota);
        setVeterinario(veterinario);
        this.fecha = (fecha != null) ? fecha : LocalDateTime.now();
        setSintomas(sintomas);
        setDiagnostico(diagnostico);
        this.estado = (estado != null) ? estado : EstadoConsulta.Activa;
        this.fechaModificacion = fechaModificacion;
        this.veterinarioModif = veterinarioModif;
    }

    /**
     * Constructor parcial para consultas con turno (sin campos de auditoría).
     *
     * @param idConsulta  identificador único
     * @param turno       turno de origen (no nulo para este constructor)
     * @param mascota     mascota paciente
     * @param veterinario veterinario actuante
     * @param fecha       fecha y hora de la consulta
     * @param sintomas    síntomas reportados
     * @param diagnostico diagnóstico
     * @param estado      estado lógico
     */
    public ConsultaMedica(int idConsulta, Turno turno, Mascota mascota,
                          Veterinario veterinario, LocalDateTime fecha,
                          String sintomas, String diagnostico, EstadoConsulta estado) {
        this(idConsulta, turno, mascota, veterinario, fecha,
             sintomas, diagnostico, estado, null, null);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único de la consulta.
     *
     * @return ID de la consulta
     */
    public int getIdConsulta() {
        return idConsulta;
    }

    /**
     * Establece el identificador único de la consulta.
     *
     * @param idConsulta ID; debe ser >= 0
     */
    public void setIdConsulta(int idConsulta) {
        if (idConsulta < 0) {
            throw new IllegalArgumentException("El ID de consulta no puede ser negativo.");
        }
        this.idConsulta = idConsulta;
    }

    /**
     * Retorna el turno asociado a la consulta.
     *
     * @return turno (puede ser nulo para urgencias)
     */
    public Turno getTurno() {
        return turno;
    }

    /**
     * Establece el turno asociado. Puede ser nulo para consultas de urgencia (RN-01).
     *
     * @param turno turno de origen (nullable)
     */
    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    /**
     * Retorna la mascota paciente.
     *
     * @return mascota
     */
    public Mascota getMascota() {
        return mascota;
    }

    /**
     * Establece la mascota paciente. No puede ser nula.
     *
     * @param mascota mascota paciente
     * @throws IllegalArgumentException si la mascota es nula
     */
    public void setMascota(Mascota mascota) {
        if (mascota == null) {
            throw new IllegalArgumentException("La mascota de la consulta no puede ser nula.");
        }
        this.mascota = mascota;
    }

    /**
     * Retorna el veterinario que realizó la consulta.
     *
     * @return veterinario actuante
     */
    public Veterinario getVeterinario() {
        return veterinario;
    }

    /**
     * Establece el veterinario actuante. No puede ser nulo.
     *
     * @param veterinario veterinario que atiende
     * @throws IllegalArgumentException si es nulo
     */
    public void setVeterinario(Veterinario veterinario) {
        if (veterinario == null) {
            throw new IllegalArgumentException("El veterinario de la consulta no puede ser nulo.");
        }
        this.veterinario = veterinario;
    }

    /**
     * Retorna la fecha y hora de la consulta.
     *
     * @return fecha y hora
     */
    public LocalDateTime getFecha() {
        return fecha;
    }

    /**
     * Establece la fecha y hora de la consulta.
     *
     * @param fecha fecha y hora (no puede ser nula)
     */
    public void setFecha(LocalDateTime fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la consulta no puede ser nula.");
        }
        this.fecha = fecha;
    }

    /**
     * Retorna los síntomas de la consulta.
     *
     * @return síntomas reportados
     */
    public String getSintomas() {
        return sintomas;
    }

    /**
     * Establece los síntomas. No pueden ser nulos ni vacíos (RN-03).
     *
     * @param sintomas síntomas reportados
     * @throws IllegalArgumentException si son nulos o vacíos
     */
    public void setSintomas(String sintomas) {
        if (sintomas == null || sintomas.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Los síntomas son obligatorios (RN-03).");
        }
        this.sintomas = sintomas.trim();
    }

    /**
     * Retorna el diagnóstico de la consulta.
     *
     * @return diagnóstico
     */
    public String getDiagnostico() {
        return diagnostico;
    }

    /**
     * Establece el diagnóstico. No puede ser nulo ni vacío (RN-03).
     *
     * @param diagnostico diagnóstico veterinario
     * @throws IllegalArgumentException si es nulo o vacío
     */
    public void setDiagnostico(String diagnostico) {
        if (diagnostico == null || diagnostico.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "El diagnóstico es obligatorio (RN-03).");
        }
        this.diagnostico = diagnostico.trim();
    }

    /**
     * Retorna el estado lógico de la consulta.
     *
     * @return estado de la consulta
     */
    public EstadoConsulta getEstado() {
        return estado;
    }

    /**
     * Establece el estado lógico de la consulta.
     *
     * @param estado estado de la consulta; no puede ser nulo
     * @throws IllegalArgumentException si es nulo
     */
    public void setEstado(EstadoConsulta estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado de la consulta no puede ser nulo.");
        }
        this.estado = estado;
    }

    /**
     * Retorna la fecha de la última modificación (registrada por trigger).
     *
     * @return fecha de modificación (puede ser nula si no hubo modificaciones)
     */
    public LocalDate getFechaModificacion() {
        return fechaModificacion;
    }

    /**
     * Establece la fecha de la última modificación.
     *
     * @param fechaModificacion fecha de modificación (nullable)
     */
    public void setFechaModificacion(LocalDate fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    /**
     * Retorna el veterinario que realizó la última modificación.
     *
     * @return veterinario modificador (puede ser nulo si no hubo modificaciones)
     */
    public Veterinario getVeterinarioModif() {
        return veterinarioModif;
    }

    /**
     * Establece el veterinario que realizó la última modificación.
     *
     * @param veterinarioModif veterinario modificador (nullable)
     */
    public void setVeterinarioModif(Veterinario veterinarioModif) {
        this.veterinarioModif = veterinarioModif;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto de la consulta para la UI.
     *
     * @return descripción con fecha, mascota, veterinario y estado
     */
    @Override
    public String toString() {
        String nombreMascota = (mascota != null) ? mascota.getNombre() : "Sin mascota";
        String nombreVet = (veterinario != null)
            ? "Dr. " + veterinario.getApellido() : "Sin veterinario";
        return "Consulta #" + idConsulta + " - " + fecha.toLocalDate()
            + " | " + nombreMascota + " | " + nombreVet + " [" + estado + "]";
    }

    /**
     * Compara dos consultas por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConsultaMedica otra = (ConsultaMedica) obj;
        return this.idConsulta == otra.idConsulta;
    }

    /**
     * Hash code basado en el ID de la consulta.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idConsulta);
    }
}
