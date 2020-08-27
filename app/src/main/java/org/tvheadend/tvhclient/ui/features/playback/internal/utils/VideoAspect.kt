package org.tvheadend.tvhclient.ui.features.playback.internal.utils;

public class Rational {

    android.util.Rational rational;
    /*
     * Do not change the order of these fields or add new instance fields to maintain the
     * Serializable compatibility across API revisions.
     */
    private final int mNumerator;
    private final int mDenominator;

    public Rational(int numerator, int denominator) {

        if (denominator < 0) {
            numerator = -numerator;
            denominator = -denominator;
        }

        // Convert to reduced form
        if (denominator == 0 && numerator > 0) {
            mNumerator = 1; // +Inf
            mDenominator = 0;
        } else if (denominator == 0 && numerator < 0) {
            mNumerator = -1; // -Inf
            mDenominator = 0;
        } else if (denominator == 0 && numerator == 0) {
            mNumerator = 0; // NaN
            mDenominator = 0;
        } else if (numerator == 0) {
            mNumerator = 0;
            mDenominator = 1;
        } else {
            int gcd = gcd(numerator, denominator);

            mNumerator = numerator / gcd;
            mDenominator = denominator / gcd;
        }
    }

    /**
     * Calculates the greatest common divisor using Euclid's algorithm.
     *
     * <p><em>Visible for testing only.</em></p>
     *
     * @param numerator   the numerator in a fraction
     * @param denominator the denominator in a fraction
     * @return An int value representing the gcd. Always positive.
     */
    private static int gcd(int numerator, int denominator) {
        /*
         * Non-recursive implementation of Euclid's algorithm:
         *
         *  gcd(a, 0) := a
         *  gcd(a, b) := gcd(b, a mod b)
         *
         */
        int a = numerator;
        int b = denominator;

        while (b != 0) {
            int oldB = b;

            b = a % b;
            a = oldB;
        }

        return Math.abs(a);
    }

    /**
     * Gets the numerator of the rational.
     *
     * <p>The numerator will always return {@code 1} if this rational represents
     * infinity (that is, the denominator is {@code 0}).</p>
     */
    public int getNumerator() {
        return mNumerator;
    }

    /**
     * Gets the denominator of the rational
     *
     * <p>The denominator may return {@code 0}, in which case the rational may represent
     * positive infinity (if the numerator was positive), negative infinity (if the numerator
     * was negative), or {@code NaN} (if the numerator was {@code 0}).</p>
     *
     * <p>The denominator will always return {@code 1} if the numerator is {@code 0}.
     */
    public int getDenominator() {
        return mDenominator;
    }
}
