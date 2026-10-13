package comum;

import java.io.Serializable;

public record Estatisticas(double media, double maior, double menor, int quantidade)
        implements Serializable {}
