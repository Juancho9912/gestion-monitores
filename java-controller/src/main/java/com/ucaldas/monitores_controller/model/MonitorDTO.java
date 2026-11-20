package com.ucaldas.monitores_controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MonitorDTO {

    @JsonProperty("codigo")
    private String codigo;

    @JsonProperty("nombre_completo")
    private String nombre_completo;

    @JsonProperty("correo")
    private String correo;

    @JsonProperty("materia_id")
    private Integer materia_id;

    @JsonProperty("materia_nombre")
    private String materia_nombre;

    public MonitorDTO() {
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre_completo() {
        return nombre_completo;
    }

    public void setNombre_completo(String nombre_completo) {
        this.nombre_completo = nombre_completo;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Integer getMateria_id() {
        return materia_id;
    }

    public void setMateria_id(Integer materia_id) {
        this.materia_id = materia_id;
    }

    public String getMateria_nombre() {
        return materia_nombre;
    }

    public void setMateria_nombre(String materia_nombre) {
        this.materia_nombre = materia_nombre;
    }
}
