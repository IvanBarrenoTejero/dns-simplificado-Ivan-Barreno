public class Registro {
    private String dominio;
    private String tipo;
    private String ip;

    public Registro(String dominio, String tipo, String ip) {
        this.dominio = dominio;
        this.tipo = tipo;
        this.ip = ip;
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
