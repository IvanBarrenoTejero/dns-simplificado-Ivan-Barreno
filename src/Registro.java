public class Registro {
    private String dominio;
    private String tipo;
    private String valor;

    public Registro(String dominio, String tipo, String valor) {
        this.dominio = dominio;
        this.tipo = tipo;
        this.valor = valor;
    }

    public String getDominio() {
        return dominio;
    }

    public void setDominio(String dominio) {
        this.dominio = dominio;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
