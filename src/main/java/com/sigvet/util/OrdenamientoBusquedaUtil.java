package com.sigvet.util;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utilidad de ordenación y búsqueda genérica para el sistema SIGVET.
 *
 * <p>Implementa los algoritmos de búsqueda y ordenación requeridos por la
 * <strong>Regla Académica 8</strong>, usando tipos genéricos para máxima reutilización:</p>
 *
 * <ul>
 *   <li><strong>Búsqueda lineal:</strong> recorre la lista secuencialmente con un predicado.</li>
 *   <li><strong>Búsqueda binaria:</strong> búsqueda en lista ordenada, retorna el índice.</li>
 *   <li><strong>MergeSort:</strong> ordena listas por nombre extrayendo el texto con un
 *       {@link Function}.</li>
 *   <li><strong>QuickSort:</strong> ordena listas con un {@link Comparator} genérico.</li>
 * </ul>
 *
 * <p>Clase de utilidad: no debe instanciarse (constructor privado).</p>
 *
 * @author SIGVET
 * @version 1.0
 */
public final class OrdenamientoBusquedaUtil {

    /**
     * Constructor privado: impide la instanciación de esta clase de utilidad.
     */
    private OrdenamientoBusquedaUtil() {
        // No instanciar
    }

    // =========================================================================
    // Búsqueda lineal
    // =========================================================================

    /**
     * Realiza una búsqueda lineal sobre una lista usando un predicado como criterio.
     *
     * <p>Recorre la lista de inicio a fin y retorna el primer elemento que satisface
     * el predicado. Complejidad temporal: O(n).</p>
     *
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * Optional<Veterinario> vet = OrdenamientoBusquedaUtil.busquedaLineal(
     *     veterinarios, v -> v.getMatricula().equals("MV1234"));
     * }</pre>
     *
     * @param <T>       tipo genérico de los elementos de la lista
     * @param lista     lista a recorrer (no puede ser nula)
     * @param predicado condición de búsqueda
     * @return {@link Optional} con el primer elemento que cumple la condición,
     *         o {@link Optional#empty()} si ninguno la cumple
     */
    public static <T> Optional<T> busquedaLineal(List<T> lista, Predicate<T> predicado) {
        if (lista == null || predicado == null) {
            return Optional.empty();
        }
        for (T elemento : lista) {
            if (predicado.test(elemento)) {
                return Optional.of(elemento);
            }
        }
        return Optional.empty();
    }

    // =========================================================================
    // Búsqueda binaria
    // =========================================================================

    /**
     * Realiza una búsqueda binaria sobre una lista previamente ordenada.
     *
     * <p>La lista DEBE estar ordenada con el mismo comparador que se pasa como argumento.
     * Complejidad temporal: O(log n).</p>
     *
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * List<Veterinario> ordenados = new ArrayList<>(veterinarios);
     * OrdenamientoBusquedaUtil.ordenar(ordenados, Comparator.comparing(Veterinario::getApellido));
     * Veterinario clave = new Veterinario();
     * clave.setApellido("García");
     * int idx = OrdenamientoBusquedaUtil.busquedaBinaria(
     *     ordenados, clave, Comparator.comparing(Veterinario::getApellido));
     * }</pre>
     *
     * @param <T>            tipo genérico de los elementos de la lista
     * @param listaOrdenada  lista ordenada con el mismo comparador
     * @param clave          elemento cuya posición se busca
     * @param comparator     comparador para comparar elementos
     * @return índice del elemento si se encuentra; índice negativo si no se encuentra
     *         (siguiendo la convención de {@link java.util.Collections#binarySearch})
     */
    public static <T> int busquedaBinaria(List<T> listaOrdenada, T clave, Comparator<T> comparator) {
        if (listaOrdenada == null || clave == null || comparator == null) {
            return -1;
        }
        int inicio = 0;
        int fin = listaOrdenada.size() - 1;

        while (inicio <= fin) {
            int medio = inicio + (fin - inicio) / 2;
            int comparacion = comparator.compare(listaOrdenada.get(medio), clave);

            if (comparacion == 0) {
                return medio; // elemento encontrado en el índice 'medio'
            } else if (comparacion < 0) {
                inicio = medio + 1; // buscar en la mitad derecha
            } else {
                fin = medio - 1;    // buscar en la mitad izquierda
            }
        }
        return -(inicio + 1); // no encontrado; retorna posición de inserción negativa
    }

    // =========================================================================
    // MergeSort por nombre
    // =========================================================================

    /**
     * Ordena una lista in-place usando el algoritmo MergeSort, ordenando alfabéticamente
     * por el nombre extraído mediante un {@link Function}.
     *
     * <p>MergeSort es estable y tiene complejidad O(n log n) en todos los casos.
     * Es ideal para ordenar listas de entidades por nombre de forma consistente.</p>
     *
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * OrdenamientoBusquedaUtil.ordenarPorNombre(mascotas, Mascota::getNombre);
     * }</pre>
     *
     * @param <T>        tipo genérico de los elementos de la lista
     * @param lista      lista a ordenar in-place; se modifica directamente
     * @param extractor  función que extrae el nombre (String) del elemento para comparar
     */
    public static <T> void ordenarPorNombre(List<T> lista, Function<T, String> extractor) {
        if (lista == null || lista.size() <= 1 || extractor == null) {
            return;
        }
        List<T> resultado = mergeSort(lista, Comparator.comparing(extractor));
        // Copiar el resultado ordenado de vuelta a la lista original (in-place)
        for (int i = 0; i < lista.size(); i++) {
            lista.set(i, resultado.get(i));
        }
    }

    /**
     * Implementación interna de MergeSort que retorna una nueva lista ordenada.
     *
     * @param <T>        tipo genérico
     * @param lista      sublista a ordenar
     * @param comparator comparador de elementos
     * @return nueva lista con los elementos ordenados
     */
    private static <T> List<T> mergeSort(List<T> lista, Comparator<T> comparator) {
        if (lista.size() <= 1) {
            return lista;
        }
        int medio = lista.size() / 2;
        List<T> izquierda = mergeSort(new java.util.ArrayList<>(lista.subList(0, medio)), comparator);
        List<T> derecha = mergeSort(new java.util.ArrayList<>(lista.subList(medio, lista.size())), comparator);
        return merge(izquierda, derecha, comparator);
    }

    /**
     * Combina dos sublistas ordenadas en una sola lista ordenada (paso merge de MergeSort).
     *
     * @param <T>        tipo genérico
     * @param izquierda  primera mitad ordenada
     * @param derecha    segunda mitad ordenada
     * @param comparator comparador
     * @return lista combinada y ordenada
     */
    private static <T> List<T> merge(List<T> izquierda, List<T> derecha, Comparator<T> comparator) {
        List<T> resultado = new java.util.ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < izquierda.size() && j < derecha.size()) {
            if (comparator.compare(izquierda.get(i), derecha.get(j)) <= 0) {
                resultado.add(izquierda.get(i));
                i++;
            } else {
                resultado.add(derecha.get(j));
                j++;
            }
        }
        // Agregar elementos restantes de la mitad izquierda
        while (i < izquierda.size()) {
            resultado.add(izquierda.get(i));
            i++;
        }
        // Agregar elementos restantes de la mitad derecha
        while (j < derecha.size()) {
            resultado.add(derecha.get(j));
            j++;
        }
        return resultado;
    }

    // =========================================================================
    // QuickSort genérico
    // =========================================================================

    /**
     * Ordena una lista in-place usando el algoritmo QuickSort con un {@link Comparator} genérico.
     *
     * <p>QuickSort tiene complejidad promedio O(n log n) y O(n²) en el peor caso.
     * Esta implementación usa el elemento del medio como pivote para mitigar el peor caso.</p>
     *
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * OrdenamientoBusquedaUtil.ordenar(turnos,
     *     Comparator.comparing(t -> t.getSlot().getFecha()));
     * }</pre>
     *
     * @param <T>        tipo genérico de los elementos de la lista
     * @param lista      lista a ordenar in-place; se modifica directamente
     * @param comparator comparador para determinar el orden
     */
    public static <T> void ordenar(List<T> lista, Comparator<T> comparator) {
        if (lista == null || lista.size() <= 1 || comparator == null) {
            return;
        }
        quickSort(lista, comparator, 0, lista.size() - 1);
    }

    /**
     * Implementación recursiva interna de QuickSort.
     *
     * @param <T>        tipo genérico
     * @param lista      lista siendo ordenada
     * @param comparator comparador de elementos
     * @param inicio     índice inicial de la sublista actual
     * @param fin        índice final de la sublista actual
     */
    private static <T> void quickSort(List<T> lista, Comparator<T> comparator, int inicio, int fin) {
        if (inicio < fin) {
            int indicePivote = particionar(lista, comparator, inicio, fin);
            quickSort(lista, comparator, inicio, indicePivote - 1);
            quickSort(lista, comparator, indicePivote + 1, fin);
        }
    }

    /**
     * Particiona la sublista alrededor de un pivote (elemento del medio).
     *
     * @param <T>        tipo genérico
     * @param lista      lista siendo particionada
     * @param comparator comparador
     * @param inicio     índice inicial
     * @param fin        índice final
     * @return índice final del pivote después de la partición
     */
    private static <T> int particionar(List<T> lista, Comparator<T> comparator, int inicio, int fin) {
        // Usar el elemento del medio como pivote para evitar el peor caso en listas casi ordenadas
        int indiceMedio = inicio + (fin - inicio) / 2;
        T pivote = lista.get(indiceMedio);

        // Mover el pivote al final
        intercambiar(lista, indiceMedio, fin);

        int indiceMenor = inicio - 1;

        for (int j = inicio; j < fin; j++) {
            if (comparator.compare(lista.get(j), pivote) <= 0) {
                indiceMenor++;
                intercambiar(lista, indiceMenor, j);
            }
        }
        // Colocar el pivote en su posición correcta
        intercambiar(lista, indiceMenor + 1, fin);
        return indiceMenor + 1;
    }

    /**
     * Intercambia dos elementos en la lista.
     *
     * @param <T>    tipo genérico
     * @param lista  lista en la que intercambiar
     * @param i      primer índice
     * @param j      segundo índice
     */
    private static <T> void intercambiar(List<T> lista, int i, int j) {
        T temp = lista.get(i);
        lista.set(i, lista.get(j));
        lista.set(j, temp);
    }
}
