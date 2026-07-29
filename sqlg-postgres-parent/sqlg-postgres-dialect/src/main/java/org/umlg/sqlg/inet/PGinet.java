package org.umlg.sqlg.inet;

import org.postgresql.PGConnection;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class PGinet extends PGobject implements Serializable, Cloneable {

    private String address;

    public PGinet() {
        type = "inet";
    }

    public PGinet(String address) {
        this();
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PGinet pGinet)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(address, pGinet.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), address);
    }

    @Override
    public void setValue(String s) throws SQLException {
        address = s;
    }

    @Override
    public String getValue() {
        return address;
    }

    public InetAddress toInetAddress() {
        try {
            String host = address.replaceAll(
                    "\\/.*$", ""
            );
            return Inet4Address.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a new {@link PGinet} with {@code delta} added to the last octet of this IPv4 address.
     * Any CIDR suffix (e.g. {@code /24}) is preserved unchanged.
     *
     * @throws IllegalStateException   if this address is not a dotted-decimal IPv4 address
     * @throws IllegalArgumentException if the resulting octet falls outside the valid range [0, 255]
     */
    public PGinet addToLastOctet(int delta) {
        return withLastOctetOffset(delta);
    }

    /**
     * Returns a new {@link PGinet} with {@code delta} subtracted from the last octet of this IPv4 address.
     * Any CIDR suffix (e.g. {@code /24}) is preserved unchanged.
     *
     * @throws IllegalStateException   if this address is not a dotted-decimal IPv4 address
     * @throws IllegalArgumentException if the resulting octet falls outside the valid range [0, 255]
     */
    public PGinet subtractFromLastOctet(int delta) {
        return withLastOctetOffset(-delta);
    }

    private PGinet withLastOctetOffset(int delta) {
        String hostPart = address;
        String cidrSuffix = "";
        int slashIndex = address.indexOf('/');
        if (slashIndex != -1) {
            hostPart = address.substring(0, slashIndex);
            cidrSuffix = address.substring(slashIndex);
        }
        String[] octets = hostPart.split("\\.");
        if (octets.length != 4) {
            throw new IllegalStateException("Can only add/subtract from the last octet of a dotted-decimal IPv4 address, got: " + address);
        }
        int lastOctet;
        try {
            lastOctet = Integer.parseInt(octets[3]) + delta;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Can only add/subtract from the last octet of a dotted-decimal IPv4 address, got: " + address, e);
        }
        if (lastOctet < 0 || lastOctet > 255) {
            throw new IllegalArgumentException("Resulting last octet " + lastOctet + " is out of range [0, 255] for address: " + address);
        }
        return new PGinet(octets[0] + "." + octets[1] + "." + octets[2] + "." + lastOctet + cidrSuffix);
    }

    public static void registerType(Connection conn) throws SQLException {
        conn.unwrap(PGConnection.class).addDataType("inet", PGinet.class);
    }

    @Override
    public PGinet clone() {
        try {
            PGinet clone = (PGinet) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
