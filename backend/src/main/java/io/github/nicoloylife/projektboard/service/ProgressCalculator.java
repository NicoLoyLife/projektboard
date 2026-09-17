package io.github.nicoloylife.projektboard.service;

/** Fortschritt eines Projekts als ganze Prozentzahl erledigter Aufgaben. */
public final class ProgressCalculator {

    private ProgressCalculator() {
    }

    public static int percent(long done, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(done * 100.0 / total);
    }
}
