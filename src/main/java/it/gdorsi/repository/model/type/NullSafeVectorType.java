package it.gdorsi.repository.model.type;

import java.io.Serializable;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Custom Vector Type, der NULL-Werte korrekt handhabt.
 * 
 * PROBLEM: Der standardmäßige VectorJdbcType von Hibernate wirft eine NullPointerException,
 * wenn er versucht, einen NULL-Wert aus der Datenbank zu lesen. Der Fehler tritt auf in:
 * VectorJdbcType$1.getFloatArray() -> String.length() auf NULL-String.
 * 
 * LÖSUNG: Dieser Custom Type prüft explizit auf NULL-Werte bevor er versucht,
 * den String zu parsen. Bei NULL-Werten wird ein leeres float[] zurückgegeben.
 */
public class NullSafeVectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER; // PostgreSQL vector type wird als OTHER gemappt
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        if (x == y) return true;
        if (x == null || y == null) return false;
        if (x.length != y.length) return false;
        for (int i = 0; i < x.length; i++) {
            if (Float.floatToIntBits(x[i]) != Float.floatToIntBits(y[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode(float[] x) {
        if (x == null) return 0;
        int result = 1;
        for (float f : x) {
            result = 31 * result + Float.floatToIntBits(f);
        }
        return result;
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) 
            throws SQLException {
        String vectorString = rs.getString(position);
        
        // WICHTIG: Prüfe auf NULL, bevor du versuchst, den String zu parsen
        if (vectorString == null || vectorString.trim().isEmpty()) {
            return new float[0]; // Leeres Array statt NULL
        }
        
        try {
            // Parse den Vector-String im Format "[1.0, 2.0, 3.0]"
            return parseVectorString(vectorString);
        } catch (Exception e) {
            // Fallback: Leeres Array bei Parse-Fehlern
            return new float[0];
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index, SharedSessionContractImplementor session) 
            throws SQLException {
        if (value == null || value.length == 0) {
            // Setze NULL in der Datenbank für leere Arrays
            st.setNull(index, Types.OTHER);
        } else {
            // Konvertiere float[] zu PostgreSQL Vector-String
            st.setObject(index, toVectorString(value), Types.OTHER);
        }
    }

    @Override
    public float[] deepCopy(float[] value) {
        if (value == null) return null;
        float[] copy = new float[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return copy;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return deepCopy((float[]) cached);
    }

    @Override
    public float[] replace(float[] original, float[] target, Object owner) {
        return deepCopy(original);
    }

    /**
     * Parst einen PostgreSQL Vector-String im Format "[1.0, 2.0, 3.0]"
     */
    private float[] parseVectorString(String vectorString) {
        // Entferne eckige Klammern
        String clean = vectorString.trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        
        if (clean.isEmpty()) {
            return new float[0];
        }
        
        String[] parts = clean.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    /**
     * Konvertiert float[] zu PostgreSQL Vector-String
     */
    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}