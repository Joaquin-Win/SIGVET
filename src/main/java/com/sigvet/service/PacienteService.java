package com.sigvet.service;

import com.sigvet.dao.DuenoDAO;
import com.sigvet.dao.EspecieDAO;
import com.sigvet.dao.MascotaDAO;
import com.sigvet.dao.RazaDAO;
import com.sigvet.model.Dueno;
import com.sigvet.model.Especie;
import com.sigvet.model.Mascota;
import com.sigvet.model.Raza;
import com.sigvet.util.OrdenamientoBusquedaUtil;
import com.sigvet.util.ValidadorUtil;

import java.sql.SQLException;
import java.util.List;

/**
 * Servicio de lógica de negocio para el caso de uso CU-06: Gestionar Dueños y Mascotas.
 *
 * <p>Orquesta las operaciones CRUD sobre dueños ({@link Dueno}) y mascotas ({@link Mascota}),
 * así como la consulta de los catálogos de especies y razas para los formularios de la UI.</p>
 *
 * <p><strong>Responsabilidades:</strong></p>
 * <ul>
 *   <li>Validar campos obligatorios con {@link ValidadorUtil} antes de persistir.</li>
 *   <li>Delegar la anonimización de datos personales al SP {@code sp_anonimizar_dueno}
 *       en cumplimiento de la Ley 25.326 (RN-13).</li>
 *   <li>Proveer el catálogo de especies y razas para el combo cascada de la UI.</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see DuenoDAO
 * @see MascotaDAO
 * @see EspecieDAO
 * @see RazaDAO
 */
public class PacienteService {

    /** DAO para la gestión de dueños. */
    private final DuenoDAO duenoDAO;

    /** DAO para la gestión de mascotas. */
    private final MascotaDAO mascotaDAO;

    /** DAO para el catálogo de especies. */
    private final EspecieDAO especieDAO;

    /** DAO para el catálogo de razas. */
    private final RazaDAO razaDAO;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor que instancia los DAOs necesarios para la gestión de pacientes.
     */
    public PacienteService() {
        this.duenoDAO   = new DuenoDAO();
        this.mascotaDAO = new MascotaDAO();
        this.especieDAO = new EspecieDAO();
        this.razaDAO    = new RazaDAO();
    }

    // =========================================================================
    // Métodos de negocio — Dueños
    // =========================================================================

    /**
     * Retorna todos los dueños activos del sistema.
     *
     * @return lista de dueños con estado {@code Activo}, ordenada por apellido
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Dueno> obtenerDuenos() throws SQLException {
        List<Dueno> lista = duenoDAO.buscarActivos();
        OrdenamientoBusquedaUtil.ordenarPorNombre(lista, d -> d.getApellido());
        return lista;
    }

    /**
     * Busca un dueño por su número de DNI.
     *
     * @param dni DNI a buscar (se valida el formato antes de consultar)
     * @return dueño encontrado, o {@code null} si no existe
     * @throws IllegalArgumentException si el formato del DNI es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public Dueno buscarDuenoPorDni(String dni)
            throws IllegalArgumentException, SQLException {
        ValidadorUtil.validarDni(dni);
        return duenoDAO.buscarPorDni(dni.trim().replaceAll("[.\\-]", ""));
    }

    /**
     * Da de alta un nuevo dueño en el sistema.
     *
     * <p><strong>Validaciones previas:</strong></p>
     * <ul>
     *   <li>DNI con formato válido (7–9 dígitos).</li>
     *   <li>Nombre, apellido y teléfono obligatorios.</li>
     *   <li>Email con formato válido si se provee (opcional).</li>
     * </ul>
     *
     * @param d dueño con los datos a registrar
     * @throws IllegalArgumentException si los datos son inválidos
     * @throws SQLException             si el DNI ya existe u otro error de BD
     */
    public void altaDueno(Dueno d)
            throws IllegalArgumentException, SQLException {
        ValidadorUtil.validarDni(d.getDni());
        ValidadorUtil.validarNoVacio(d.getNombre(),   "Nombre");
        ValidadorUtil.validarNoVacio(d.getApellido(), "Apellido");
        ValidadorUtil.validarNoVacio(d.getTelefono(), "Teléfono");
        ValidadorUtil.validarEmail(d.getEmail()); // No lanza si está vacío (campo opcional)

        duenoDAO.insertar(d);
    }

    /**
     * Modifica los datos de un dueño existente.
     *
     * <p>Aplica las mismas validaciones que {@link #altaDueno(Dueno)}.</p>
     *
     * @param d dueño con los datos actualizados; debe tener el {@code id} asignado
     * @throws IllegalArgumentException si los datos son inválidos
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void modificarDueno(Dueno d)
            throws IllegalArgumentException, SQLException {
        if (d.getId() <= 0) {
            throw new IllegalArgumentException("ID de dueño inválido.");
        }
        ValidadorUtil.validarDni(d.getDni());
        ValidadorUtil.validarNoVacio(d.getNombre(),   "Nombre");
        ValidadorUtil.validarNoVacio(d.getApellido(), "Apellido");
        ValidadorUtil.validarNoVacio(d.getTelefono(), "Teléfono");
        ValidadorUtil.validarEmail(d.getEmail());

        duenoDAO.actualizar(d);
    }

    /**
     * Aplica baja lógica a un dueño (estado → {@code Inactivo}).
     *
     * @param id identificador del dueño
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void bajaLogicaDueno(int id)
            throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de dueño inválido.");
        }
        duenoDAO.bajaLogica(id);
    }

    /**
     * Anonimiza los datos personales de un dueño llamando al SP {@code sp_anonimizar_dueno}
     * en cumplimiento de la Ley 25.326 de Protección de Datos Personales (RN-13).
     *
     * <p>El SP reemplaza nombre, apellido, DNI, teléfono, email y dirección con
     * valores genéricos, preservando solo el ID para mantener la integridad referencial.</p>
     *
     * @param id identificador del dueño a anonimizar
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si ocurre un error durante la anonimización
     */
    public void anonimizarDueno(int id)
            throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de dueño inválido.");
        }
        duenoDAO.anonimizar(id);
    }

    // =========================================================================
    // Métodos de negocio — Mascotas
    // =========================================================================

    /**
     * Retorna todas las mascotas activas de un dueño específico.
     *
     * @param idDueno identificador del dueño
     * @return lista de mascotas activas del dueño
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<Mascota> obtenerMascotasPorDueno(int idDueno)
            throws IllegalArgumentException, SQLException {
        if (idDueno <= 0) {
            throw new IllegalArgumentException("ID de dueño inválido.");
        }
        return mascotaDAO.buscarPorDueno(idDueno);
    }

    /**
     * Da de alta una nueva mascota en el sistema.
     *
     * <p><strong>Validaciones previas:</strong></p>
     * <ul>
     *   <li>Nombre de la mascota es obligatorio.</li>
     *   <li>El dueño debe tener un ID asignado.</li>
     *   <li>La especie y raza deben tener IDs asignados.</li>
     * </ul>
     *
     * @param m mascota con los datos a registrar
     * @throws IllegalArgumentException si los datos son inválidos
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void altaMascota(Mascota m)
            throws IllegalArgumentException, SQLException {
        ValidadorUtil.validarNoVacio(m.getNombre(), "Nombre de mascota");
        if (m.getDueno() == null || m.getDueno().getId() <= 0) {
            throw new IllegalArgumentException("Debe asociar la mascota a un dueño válido.");
        }
        if (m.getEspecie() == null || m.getEspecie().getIdEspecie() <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una especie válida.");
        }
        if (m.getRaza() == null || m.getRaza().getIdRaza() <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una raza válida.");
        }

        mascotaDAO.insertar(m);
    }

    /**
     * Modifica los datos de una mascota existente.
     *
     * @param m mascota con los datos actualizados
     * @throws IllegalArgumentException si los datos son inválidos
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void modificarMascota(Mascota m)
            throws IllegalArgumentException, SQLException {
        if (m.getIdMascota() <= 0) {
            throw new IllegalArgumentException("ID de mascota inválido.");
        }
        ValidadorUtil.validarNoVacio(m.getNombre(), "Nombre de mascota");

        mascotaDAO.actualizar(m);
    }

    /**
     * Aplica baja lógica a una mascota (estado → {@code Inactivo}).
     *
     * @param id identificador de la mascota
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void bajaLogicaMascota(int id)
            throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de mascota inválido.");
        }
        mascotaDAO.bajaLogica(id);
    }

    /**
     * Busca mascotas por nombre (prefijo).
     *
     * @param nombre texto de búsqueda
     * @return lista de mascotas cuyo nombre comienza con el texto dado
     * @throws IllegalArgumentException si el texto está vacío
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<Mascota> buscarMascotaPorNombre(String nombre)
            throws IllegalArgumentException, SQLException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingrese al menos un carácter para buscar.");
        }
        return mascotaDAO.buscarPorNombre(nombre.trim());
    }

    /**
     * Busca una mascota específica por su ID.
     *
     * @param idMascota identificador de la mascota
     * @return mascota encontrada, o {@code null}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public Mascota buscarMascota(int idMascota) throws SQLException {
        return mascotaDAO.buscarPorId(idMascota);
    }

    // =========================================================================
    // Métodos de negocio — Catálogos
    // =========================================================================

    /**
     * Retorna todas las especies del catálogo, ordenadas por nombre.
     *
     * <p>Utilizado para poblar el combo de especie en el formulario de alta de mascotas.</p>
     *
     * @return lista de especies
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Especie> obtenerEspecies() throws SQLException {
        return especieDAO.buscarTodos();
    }

    /**
     * Retorna las razas pertenecientes a una especie (para combo cascada Especie → Raza).
     *
     * <p>Se llama cada vez que el usuario selecciona una especie en el formulario,
     * para actualizar dinámicamente el combo de razas.</p>
     *
     * @param idEspecie identificador de la especie seleccionada
     * @return lista de razas de la especie, ordenada por nombre
     * @throws IllegalArgumentException si el ID de especie es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<Raza> obtenerRazasPorEspecie(int idEspecie)
            throws IllegalArgumentException, SQLException {
        if (idEspecie <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una especie válida.");
        }
        return razaDAO.buscarPorEspecie(idEspecie);
    }
}
