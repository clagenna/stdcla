package prova.date;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DeltaT — interpreta stringhe come intervalli di tempo (positivi o negativi)
 * e li somma/sottrae da un LocalDateTime.
 *
 * Formati supportati:
 *   Completo con segno:  [-|+]Y-MM-DD HH:mm
 *     es. "-1-00-01 00:13"  → -1 anno, -1 giorno, -13 minuti
 *         "+0-02-10 01:30"  → +2 mesi, +10 giorni, +1 ora 30 minuti
 *
 *   Solo ore/minuti:     [+|-]H:mm
 *     es. "0:27"  → +27 minuti
 *         "-2:15" → -2 ore 15 minuti
 *
 *   Solo minuti:         [+|-]mm
 *     es. "45"  → +45 minuti
 *         "-90" → -90 minuti
 */
public class DeltaT {

    // Formato esteso: [-+]Y-MM-DD HH:mm  (o varianti parziali)
    private static final Pattern FULL = Pattern.compile(
        "^([+-]?)(\\d+)-(\\d{1,2})-(\\d{1,2})\\s+(\\d{1,2}):(\\d{2})$"
    );

    // Solo H:mm  (es. "0:27", "-2:15")
    private static final Pattern HMS = Pattern.compile(
        "^([+-]?)(\\d+):(\\d{2})$"
    );

    // Solo minuti  (es. "45", "-90")
    private static final Pattern MINS = Pattern.compile(
        "^([+-]?)(\\d+)$"
    );

    private final boolean negative;
    private final int years;
    private final int months;
    private final int days;
    private final int hours;
    private final int minutes;

    // ------------------------------------------------------------------ //
    //  Costruttori
    // ------------------------------------------------------------------ //

    /** Costruisce un DeltaT dal parsing automatico della stringa. */
    public DeltaT(String input) {
        String s = input == null ? "" : input.trim();
        int[] parsed = parse(s);
        this.negative = parsed[0] == 1;
        this.years    = parsed[1];
        this.months   = parsed[2];
        this.days     = parsed[3];
        this.hours    = parsed[4];
        this.minutes  = parsed[5];
    }

    /** Costruisce un DeltaT esplicitamente (valori positivi, usa negative per il segno). */
    public DeltaT(boolean negative, int years, int months, int days,
                  int hours, int minutes) {
        this.negative = negative;
        this.years    = Math.abs(years);
        this.months   = Math.abs(months);
        this.days     = Math.abs(days);
        this.hours    = Math.abs(hours);
        this.minutes  = Math.abs(minutes);
    }

    // ------------------------------------------------------------------ //
    //  Parser
    // ------------------------------------------------------------------ //

    private static int[] parse(String s) {
        // int[6]: {isNegative, years, months, days, hours, minutes}

        Matcher m = FULL.matcher(s);
        if (m.matches()) {
            boolean neg = "-".equals(m.group(1));
            return new int[]{
                neg ? 1 : 0,
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)),
                Integer.parseInt(m.group(4)),
                Integer.parseInt(m.group(5)),
                Integer.parseInt(m.group(6))
            };
        }

        m = HMS.matcher(s);
        if (m.matches()) {
            boolean neg = "-".equals(m.group(1));
            return new int[]{
                neg ? 1 : 0, 0, 0, 0,
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))
            };
        }

        m = MINS.matcher(s);
        if (m.matches()) {
            boolean neg = "-".equals(m.group(1));
            return new int[]{
                neg ? 1 : 0, 0, 0, 0, 0,
                Integer.parseInt(m.group(2))
            };
        }

        throw new IllegalArgumentException(
            "Formato DeltaT non riconosciuto: \"" + s + "\"\n" +
            "Esempi validi: \"-1-00-01 00:13\", \"0:27\", \"+2:00\", \"45\""
        );
    }

    // ------------------------------------------------------------------ //
    //  Applicazione al LocalDateTime
    // ------------------------------------------------------------------ //

    /**
     * Somma (o sottrae) questo delta a {@code base} e restituisce
     * il nuovo LocalDateTime.
     */
    public LocalDateTime plus(LocalDateTime base) {
        if (base == null) throw new IllegalArgumentException("base non può essere null");

        Period  period   = Period.of(years, months, days);
        Duration duration = Duration.ofHours(hours).plusMinutes(minutes);

        if (negative) {
            return base.minus(period).minus(duration);
        } else {
            return base.plus(period).plus(duration);
        }
    }

    /**
     * Sottrae questo delta da {@code base} (indipendentemente dal segno
     * interno: inverte il segno corrente).
     */
    public LocalDateTime minus(LocalDateTime base) {
        DeltaT inverted = new DeltaT(!this.negative, years, months,
                                      days, hours, minutes);
        return inverted.plus(base);
    }

    // ------------------------------------------------------------------ //
    //  Utility
    // ------------------------------------------------------------------ //

    public boolean isNegative() { return negative; }
    public int getYears()   { return years; }
    public int getMonths()  { return months; }
    public int getDays()    { return days; }
    public int getHours()   { return hours; }
    public int getMinutes() { return minutes; }

    /** Converte il delta in {@link Duration} pura (approssimazione: 1 anno = 365 giorni, 1 mese = 30 giorni). */
    public Duration toDurationApprox() {
        long totalMinutes = (long) years * 365 * 24 * 60
                          + (long) months * 30 * 24 * 60
                          + (long) days * 24 * 60
                          + (long) hours * 60
                          + minutes;
        return negative ? Duration.ofMinutes(-totalMinutes)
                        : Duration.ofMinutes(totalMinutes);
    }

    @Override
    public String toString() {
        return String.format("DeltaT[%s%dY %dM %dD %dh %dm]",
            negative ? "-" : "+", years, months, days, hours, minutes);
    }
}
