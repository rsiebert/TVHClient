package org.tvheadend.tvhclient.utils;

/**
 * Container to ease passing around a tuple of three objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.
 */
public class Triple<F, S, T> {
    public final F first;
    public final S second;
    public final T third;
    /**
     * Constructor for a Pair.
     *
     * @param first the first object in the Pair
     * @param second the second object in the pair
     */
    private Triple(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
    /**
     * Checks the three objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link Triple} to which this one is to be checked for equality
     * @return true if the underlying objects of the Triple are both considered
     *         equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Triple)) {
            return false;
        }
        Triple<?, ?, ?> p = (Triple<?, ?, ?>) o;
        return p.first.equals(first)
                && p.second.equals(second)
                && p.third.equals(third);
    }
    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the Triple
     */
    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode())
                ^ (second == null ? 0 : second.hashCode())
                ^ (third == null ? 0 : third.hashCode());
    }
    @Override
    public String toString() {
        return "Triple{" + String.valueOf(first)
                + " " + String.valueOf(second)
                + " " + String.valueOf(third) + "}";
    }
    /**
     * Convenience method for creating an appropriately typed pair.
     * @param a the first object in the Triple
     * @param b the second object in the pair
     * @return a Triple that is templatized with the types of a and b
     */
    public static <A, B, C> Triple <A, B, C> create(A a, B b, C c) {
        return new Triple<>(a, b, c);
    }
}